package com.kubit.authservice.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public ResponseEntity<AuthUser> register(@RequestBody RegisterRequest request) {
        AuthUser user = authService.register(request);
        user.setPasswordHash(null);
        return ResponseEntity.ok(user);
    }

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
