package com.kubit.authservice.service.impl;

import com.kubit.authservice.domain.entity.*;
import com.kubit.authservice.domain.repository.*;
import com.kubit.authservice.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceImplTest {

    @Mock private AuthUserRepository authUserRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private AudithLogRepository audithLogRepository;

    @InjectMocks private AuthServiceImpl authService;

    private AuthUser user;
    private Role role;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        role = Role.builder().id(1L).name("ROLE_USER").build();
        user = AuthUser.builder().id(1L).email("t@kiubit.mx")
                .passwordHash("abc").status(AuthUserStatus.ACTIVE).roles(Set.of(role)).build();
    }

    @Test
    void register_successful() {
        RegisterRequest req = RegisterRequest.builder().email("e@x.com").password("123").build();
        when(authUserRepository.existsByEmail(req.getEmail())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(role));
        when(passwordEncoder.encode("123")).thenReturn("hashed");
        when(authUserRepository.save(any())).thenReturn(user);
        AuthUser result = authService.register(req);
        assertEquals(user, result);
    }

    @Test
    void register_duplicatedEmail_throws() {
        RegisterRequest req = RegisterRequest.builder().email("e@x.com").password("123").build();
        when(authUserRepository.existsByEmail(req.getEmail())).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> authService.register(req));
    }

    @Test
    void login_valid() {
        LoginRequest req = LoginRequest.builder().email(user.getEmail()).password("pw").build();
        when(authUserRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pw", user.getPasswordHash())).thenReturn(true);
        UserLoginResponse result = authService.login(req);
        assertEquals(user, result.getUser());
    }

    @Test
    void login_fail() {
        LoginRequest req = LoginRequest.builder().email("nope").password("pw").build();
        when(authUserRepository.findByEmail("nope")).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> authService.login(req));
    }

    @Test
    void refreshToken_oldTokenRevoked_generatesNew_andAudits() {
        String oldTokenValue = "token1";
        String newTokenValue = "token2";
        String deviceId = "dev1";
        String ip = "127.0.0.1";
        RefreshToken oldToken = RefreshToken.builder()
                .token(oldTokenValue)
                .revoked(false)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .authUser(user)
                .deviceId(deviceId)
                .build();
        when(refreshTokenRepository.findByToken(oldTokenValue)).thenReturn(Optional.of(oldToken));
        when(jwtUtil.generateToken(user)).thenReturn("jwtNew");
        when(jwtUtil.generateRefreshToken(user)).thenReturn(newTokenValue);
        when(audithLogRepository.save(any())).thenReturn(null);
        UserLoginResponse resp = authService.refreshToken(oldTokenValue, deviceId, ip);
        assertEquals(user, resp.getUser());
        assertEquals("jwtNew", resp.getAccessToken());
        assertEquals(newTokenValue, resp.getRefreshToken());
        assertTrue(oldToken.getRevoked());
        verify(audithLogRepository, atLeastOnce()).save(any());
    }

    @Test
    void refreshToken_failsWithRevokedOrExpired() {
        String tokenVal = "token";
        String deviceId = "dev1";
        RefreshToken old = RefreshToken.builder()
                .token(tokenVal).revoked(true)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .authUser(user)
                .deviceId(deviceId)
                .build();
        when(refreshTokenRepository.findByToken(tokenVal)).thenReturn(Optional.of(old));
        assertThrows(IllegalArgumentException.class,
                () -> authService.refreshToken(tokenVal, deviceId, "127.0.0.1"));
    }

    @Test
    void logout_revokesToken_andAudits() {
        String tokenVal = "token";
        String deviceId = "dev1";
        RefreshToken rt = RefreshToken.builder().token(tokenVal).revoked(false)
                .authUser(user).deviceId(deviceId).build();
        when(refreshTokenRepository.findByToken(tokenVal)).thenReturn(Optional.of(rt));
        when(audithLogRepository.save(any())).thenReturn(null);
        authService.logout(tokenVal, deviceId, "ipx");
        assertTrue(rt.getRevoked());
        verify(audithLogRepository, atLeastOnce()).save(any());
    }

    @Test
    void logoutAllForUser_revokesAll_andAudits() {
        RefreshToken rt1 = RefreshToken.builder().revoked(false).authUser(user).deviceId("d1").build();
        RefreshToken rt2 = RefreshToken.builder().revoked(false).authUser(user).deviceId("d2").build();
        when(refreshTokenRepository.findAll()).thenReturn(List.of(rt1, rt2));
        when(audithLogRepository.save(any())).thenReturn(null);
        authService.logoutAllForUser(user.getId(), "ip1");
        assertTrue(rt1.getRevoked() && rt2.getRevoked());
        verify(audithLogRepository, atLeastOnce()).save(any());
    }
}
