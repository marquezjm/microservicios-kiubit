package com.kubit.authservice.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.kubit.authservice.domain.entity.AuthUser;
import com.kubit.authservice.domain.entity.LoginRequest;
import com.kubit.authservice.domain.entity.RegisterRequest;
import com.kubit.authservice.domain.entity.UserLoginResponse;
import com.kubit.authservice.service.AuthService;
import com.kubit.authservice.util.JwtUtil;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.Parameter;

@Tag(name = "Autenticación", description = "Endpoints para registrar, autenticar, refrescar y cerrar sesión de usuarios")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @Operation(
        summary = "Registro de usuario nuevo",
        description = "Crea una nueva cuenta de usuario con email único. Devuelve el usuario creado sin la contraseña.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Usuario registrado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o email duplicado")
    })
    @PostMapping("/register")
    public ResponseEntity<AuthUser> register(@RequestBody RegisterRequest request) {
        AuthUser user = authService.register(request);
        user.setPasswordHash(null);
        return ResponseEntity.ok(user);
    }

    @Operation(
        summary = "Login de usuario",
        description = "Autentica a un usuario existente. Devuelve los tokens de acceso/refresh y el usuario. Si no se proporciona deviceId, se genera uno y se devuelve como cookie visible.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login exitoso - usuario, accessToken y refreshToken devueltos"),
        @ApiResponse(responseCode = "400", description = "Credenciales inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(@RequestBody LoginRequest request,
            HttpServletRequest servletRequest,
            HttpServletResponse response,
            @CookieValue(value = "deviceId", required = false) String deviceId) {
        // Si el cliente no envió deviceId, el backend lo genera y lo pone en cookie
        // estándar
        if (deviceId == null || deviceId.isBlank()) {
            deviceId = UUID.randomUUID().toString();
            Cookie deviceCookie = new Cookie("deviceId", deviceId);
            deviceCookie.setHttpOnly(false); // Cliente JS debe acceder y enviar en siguientes requests
            deviceCookie.setSecure(true);
            deviceCookie.setPath("/");
            deviceCookie.setMaxAge(60 * 60 * 24 * 365); // Vive 1 año
            response.addCookie(deviceCookie);
        }
         String ipAddress = servletRequest.getHeader("X-Forwarded-For");
        if (ipAddress == null)
            ipAddress = servletRequest.getRemoteAddr();
        // Pasa deviceId y ipAddress al LoginRequest
        request.setDeviceId(deviceId);
        request.setIpAddress(ipAddress);
        UserLoginResponse loginResponse = authService.login(request);
        // setea cookies access/refresh como antes
        // ...
        setAuthCookies(response, loginResponse.getAccessToken(), loginResponse.getRefreshToken());
        loginResponse.getUser().setPasswordHash(null);
        return ResponseEntity.ok(new UserLoginResponse(loginResponse.getUser(), null, null));
    }

    @Operation(
        summary = "Renueva tokens de acceso",
        description = "Renueva el access token usando un refresh token válido, atado a un deviceId. Requiere ambos cookies.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token renovado exitosamente"),
        @ApiResponse(responseCode = "400", description = "Token inválido, revocado o faltante")
    })
    @PostMapping("/refresh")
    public ResponseEntity<UserLoginResponse> refresh(
            @CookieValue("refreshToken") String refreshToken,
            @CookieValue("deviceId") String deviceId,
            HttpServletRequest servletRequest,
            HttpServletResponse response) {
        String ipAddress = extractIp(servletRequest);
        UserLoginResponse loginResponse = authService.refreshToken(refreshToken, deviceId, ipAddress);
        setAuthCookies(response, loginResponse.getAccessToken(), loginResponse.getRefreshToken());
        loginResponse.getUser().setPasswordHash(null);
        return ResponseEntity.ok(new UserLoginResponse(loginResponse.getUser(), null, null));
    }

    @Operation(
        summary = "Logout de dispositivo actual",
        description = "Revoca el refresh token de este dispositivo y elimina cookies de sesión."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout exitoso"),
        @ApiResponse(responseCode = "400", description = "Token inválido o deviceId incorrecto")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue("refreshToken") String refreshToken,
            @CookieValue("deviceId") String deviceId,
            HttpServletRequest servletRequest,
            HttpServletResponse response) {
        String ipAddress = extractIp(servletRequest);
        authService.logout(refreshToken, deviceId, ipAddress);
        clearAuthCookies(response);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Logout en todos los dispositivos",
        description = "Revoca todos los refresh tokens activos del usuario (requiere el userId asociado autenticado) y elimina cookies."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Logout global exitoso"),
        @ApiResponse(responseCode = "400", description = "Usuario no encontrado")
    })
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@RequestParam Long userId,
        HttpServletRequest servletRequest,
            HttpServletResponse response) {
        String ipAddress = extractIp(servletRequest);
        authService.logoutAllForUser(userId, ipAddress);
        clearAuthCookies(response);
        return ResponseEntity.ok().build();
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // Configurar la cookie del token de acceso
        Cookie accessCookie = new Cookie("accessToken", accessToken);
        accessCookie.setHttpOnly(true);
        accessCookie.setSecure(true);
        accessCookie.setPath("/");
        accessCookie.setMaxAge((int) jwtUtil.getJwtExpirationMs() / 1000); // Convertir a segundos
        response.addCookie(accessCookie);

        // Configurar la cookie del token de refresco
        Cookie refreshCookie = new Cookie("refreshToken", refreshToken);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setSecure(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge((int) jwtUtil.getRefreshExpirationMs() / 1000); // Convertir a segundos
        response.addCookie(refreshCookie);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        Cookie accessCookie = new Cookie("accessToken", null);
        accessCookie.setPath("/");
        accessCookie.setMaxAge(0); // Eliminar la cookie
        response.addCookie(accessCookie);

        Cookie refreshCookie = new Cookie("refreshToken", null);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // Eliminar la cookie
        response.addCookie(refreshCookie);
    }

    private String extractIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null)
            ip = request.getRemoteAddr();
        return ip;
    }

}
