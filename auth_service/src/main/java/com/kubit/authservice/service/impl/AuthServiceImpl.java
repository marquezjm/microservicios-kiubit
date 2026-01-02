package com.kubit.authservice.service.impl;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.kubit.authservice.domain.entity.AudithLog;
import com.kubit.authservice.domain.entity.AuthUser;
import com.kubit.authservice.domain.entity.AuthUserStatus;
import com.kubit.authservice.domain.entity.LoginRequest;
import com.kubit.authservice.domain.entity.RefreshToken;
import com.kubit.authservice.domain.entity.RegisterRequest;
import com.kubit.authservice.domain.entity.Role;
import com.kubit.authservice.domain.entity.UserLoginResponse;
import com.kubit.authservice.domain.repository.AudithLogRepository;
import com.kubit.authservice.domain.repository.AuthUserRepository;
import com.kubit.authservice.domain.repository.RefreshTokenRepository;
import com.kubit.authservice.domain.repository.RoleRepository;
import com.kubit.authservice.service.AuthService;
import com.kubit.authservice.util.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final AuthUserRepository authUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtUtil jwtUtil;
    private final AudithLogRepository audithLogRepository;

    public AuthUser register(RegisterRequest request) {
        if (authUserRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }
        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new IllegalStateException("Default role not found"));
        AuthUser user = AuthUser.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .status(AuthUserStatus.ACTIVE)
                .roles(Set.of(userRole))
                .build();
        return authUserRepository.save(user);
    }

    @Override
    public UserLoginResponse login(LoginRequest request) {
        AuthUser user = authUserRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        // Generar ambos tokens
        String accessToken = jwtUtil.generateToken(user);
        String refreshTokenStr = jwtUtil.generateRefreshToken(user);
        // Persistir el refresh token
        RefreshToken refreshToken = RefreshToken.builder()
                .authUser(user)
                .token(refreshTokenStr)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(refreshToken);

        AudithLog log = AudithLog.builder()
                .authUserId(user.getId())
                .eventType("LOGIN")
                .ipAddress("unknown") // Si tienes acceso al request, pásala aquí
                .build();
        audithLogRepository.save(log);
        return new UserLoginResponse(user, accessToken, refreshTokenStr);
    }

    @Override
    public UserLoginResponse refreshToken(String refreshTokenStr) {
        // 1. Buscar el refresh token en BD y validar
        RefreshToken oldToken = refreshTokenRepository.findByToken(refreshTokenStr)
                .filter(token -> !token.getRevoked())
                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired refresh token"));
        AuthUser user = oldToken.getAuthUser();
        // 2. Revocar el refresh token usado (rotación segura)
        oldToken.setRevoked(true);
        refreshTokenRepository.save(oldToken);
        // 3. Generar nuevos tokens
        String newAccessToken = jwtUtil.generateToken(user);
        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        // 4. Persistir el nuevo refresh token asociado al usuario
        RefreshToken newToken = RefreshToken.builder()
                .authUser(user)
                .token(newRefreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000))
                .revoked(false)
                .build();
        refreshTokenRepository.save(newToken);
        // 5. Retornar user, access token, refresh token al Controller (los cookies se
        // setean allá)
        return new UserLoginResponse(user, newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(String refreshTokenString) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenString)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token not found"));
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);
        // Auditoría: registra evento de logout
        AudithLog log = AudithLog.builder()
                .authUserId(refreshToken.getAuthUser().getId())
                .eventType("LOGOUT")
                .ipAddress("unknown") // O remítelo del contexto del request si lo tienes
                .build();
        audithLogRepository.save(log);
    }

    @Override
    public void logoutAllForUser(Long userId) {
        // Extra: si quieres registrar cuántos tokens se revocan:
        var tokens = refreshTokenRepository.findAll().stream()
                .filter(t -> t.getAuthUser().getId().equals(userId) && !t.getRevoked())
                .toList();
        tokens.forEach(token -> {
            token.setRevoked(true);
            refreshTokenRepository.save(token);
        });
        // Auditoría
        AudithLog log = AudithLog.builder()
                .authUserId(userId)
                .eventType("LOGOUT_ALL")
                .ipAddress("unknown") // O remítelo del contexto del request si lo tienes
                .build();
        audithLogRepository.save(log);
        // Limpieza "dura": también puedes hacer
        // refreshTokenRepository.deleteByAuthUserId(userId);
        // Pero así ya queda trazabilidad
    }

    @Override
    public boolean validateJwt(String jwt) {
        return jwtUtil.validateToken(jwt);
    }

}
