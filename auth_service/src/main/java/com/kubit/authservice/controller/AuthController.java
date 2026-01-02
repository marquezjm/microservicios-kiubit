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
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

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
    public ResponseEntity<AuthUser> register (@RequestBody RegisterRequest request) {
        AuthUser user = authService.register(request);
        user.setPasswordHash(null);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponse> login(@RequestBody LoginRequest request,
                                                     HttpServletResponse response) {
        UserLoginResponse loginResponse = authService.login(request);

        setAuthCookies(response, loginResponse.getAccessToken(), loginResponse.getRefreshToken());
        loginResponse.getUser().setPasswordHash(null);
        return ResponseEntity.ok(new UserLoginResponse(loginResponse.getUser(),null,null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<UserLoginResponse> refresh(@CookieValue("refreshToken") String refreshToken,
                                                     HttpServletResponse response) {
        UserLoginResponse loginResponse = authService.refreshToken(refreshToken);
        setAuthCookies(response, loginResponse.getAccessToken(), loginResponse.getRefreshToken());
        loginResponse.getUser().setPasswordHash(null);
        return ResponseEntity.ok(new UserLoginResponse(loginResponse.getUser(),null,null));
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue("refreshToken") String refreshToken,
                                       HttpServletResponse response) {
        authService.logout(refreshToken);
        clearAuthCookies(response);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(@RequestParam Long userId,
                                          HttpServletResponse response) {
        authService.logoutAllForUser(userId);
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
    
}
