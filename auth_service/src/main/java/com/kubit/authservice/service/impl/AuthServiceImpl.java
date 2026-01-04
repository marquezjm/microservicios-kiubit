package com.kubit.authservice.service.impl;

import java.time.LocalDateTime;
import java.util.List;
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
                String deviceId = request.getDeviceId();
                String ipAddress = request.getIpAddress();
                // Revocar refreshToken anterior de este user+deviceId si existe
                List<RefreshToken> existingTokens = refreshTokenRepository.findAllByAuthUserIdAndDeviceId(user.getId(),
                                deviceId);
                existingTokens.forEach(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                });
                // Generar los tokens nuevos
                String accessToken = jwtUtil.generateToken(user);
                String refreshTokenStr = jwtUtil.generateRefreshToken(user);
                // Persistir refresh token
                RefreshToken refreshToken = RefreshToken.builder()
                                .authUser(user)
                                .token(refreshTokenStr)
                                .deviceId(deviceId)
                                .ipAddress(ipAddress)
                                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000))
                                .revoked(false)
                                .build();
                refreshTokenRepository.save(refreshToken);
                // Auditoría (si tienes AudithLogRepository inyectado)
                AudithLog log = AudithLog.builder()
                                .authUserId(user.getId())
                                .eventType("LOGIN")
                                .ipAddress(ipAddress)
                                .build();
                audithLogRepository.save(log);
                return new UserLoginResponse(user, accessToken, refreshTokenStr);
        }

        @Override
        public UserLoginResponse refreshToken(String refreshTokenStr, String deviceId, String ipAddress) {
                // Busca y valida el refreshToken para el usuario y deviceId
                RefreshToken oldToken = refreshTokenRepository.findByToken(refreshTokenStr)
                                .filter(token -> !token.getRevoked())
                                .filter(token -> token.getExpiresAt().isAfter(LocalDateTime.now()))
                                .filter(token -> deviceId.equals(token.getDeviceId()))
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Invalid/expired refresh token for device"));
                AuthUser user = oldToken.getAuthUser();
                // Revoca el token anterior (rotación)
                oldToken.setRevoked(true);
                refreshTokenRepository.save(oldToken);
                // Generar y guardar nuevos tokens
                String newAccessToken = jwtUtil.generateToken(user);
                String newRefreshToken = jwtUtil.generateRefreshToken(user);
                RefreshToken newToken = RefreshToken.builder()
                                .authUser(user)
                                .token(newRefreshToken)
                                .deviceId(deviceId)
                                .ipAddress(ipAddress)
                                .expiresAt(LocalDateTime.now().plusSeconds(jwtUtil.getRefreshExpirationMs() / 1000))
                                .revoked(false)
                                .build();
                refreshTokenRepository.save(newToken);
                // Audit log
                AudithLog log = AudithLog.builder()
                                .authUserId(user.getId())
                                .eventType("REFRESH_TOKEN")
                                .ipAddress(ipAddress)
                                .build();
                audithLogRepository.save(log);
                return new UserLoginResponse(user, newAccessToken, newRefreshToken);
        }

        @Override
        public void logout(String refreshTokenStr, String deviceId, String ipAddress) {
                RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
                                .filter(token -> deviceId.equals(token.getDeviceId()))
                                .orElseThrow(() -> new IllegalArgumentException("Refresh token/deviceId not found"));
                refreshToken.setRevoked(true);
                refreshTokenRepository.save(refreshToken);
                AudithLog log = AudithLog.builder()
                                .authUserId(refreshToken.getAuthUser().getId())
                                .eventType("LOGOUT")
                                .ipAddress(ipAddress)
                                .build();
                audithLogRepository.save(log);
        }

        @Override
        public void logoutAllForUser(Long userId, String ipAddress) {
                var tokens = refreshTokenRepository.findAll().stream()
                                .filter(t -> t.getAuthUser().getId().equals(userId) && !t.getRevoked())
                                .toList();
                tokens.forEach(token -> {
                        token.setRevoked(true);
                        refreshTokenRepository.save(token);
                });
                AudithLog log = AudithLog.builder()
                                .authUserId(userId)
                                .eventType("LOGOUT_ALL")
                                .ipAddress(ipAddress)
                                .build();
                audithLogRepository.save(log);
        }

        @Override
        public boolean validateJwt(String jwt) {
                return jwtUtil.validateToken(jwt);
        }

}
