package com.kubit.authservice.service.impl;

import com.kubit.authservice.domain.entity.AuthUser;
import com.kubit.authservice.domain.entity.AuthUserStatus;
import com.kubit.authservice.domain.entity.RegisterRequest;
import com.kubit.authservice.domain.entity.Role;
import com.kubit.authservice.domain.repository.AudithLogRepository;
import com.kubit.authservice.domain.repository.AuthUserRepository;
import com.kubit.authservice.domain.repository.RefreshTokenRepository;
import com.kubit.authservice.domain.repository.RoleRepository;
import com.kubit.authservice.util.JwtUtil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private AuthUserRepository authUserRepository;
    private RoleRepository roleRepository;
    private PasswordEncoder passwordEncoder;
    private AuthServiceImpl authService;
    private RefreshTokenRepository refreshTokenRepository;
    private JwtUtil jwtUtil;
    private AudithLogRepository audithLogRepository;

    @BeforeEach
    void setUp() {
        authUserRepository = mock(AuthUserRepository.class);
        roleRepository = mock(RoleRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        jwtUtil = mock(JwtUtil.class);
        audithLogRepository = mock(AudithLogRepository.class);
        authService = new AuthServiceImpl(authUserRepository, roleRepository, passwordEncoder, refreshTokenRepository, jwtUtil, audithLogRepository);
    }

    @Test
    void register_successful() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .age(20)
                .email("test@email.com")
                .password("1234").build();
        Role userRole = Role.builder().name("ROLE_USER").build();

        when(authUserRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashed1234");

        authService.register(request);

        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        verify(authUserRepository).save(userCaptor.capture());
        AuthUser savedUser = userCaptor.getValue();

        assertEquals(request.getEmail(), savedUser.getEmail());
        assertEquals("hashed1234", savedUser.getPasswordHash());
        assertEquals(AuthUserStatus.ACTIVE, savedUser.getStatus());
        assertTrue(savedUser.getRoles().contains(userRole));
    }

    @Test
    void register_emailAlreadyExists_shouldThrow() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .age(20)
                .email("test@email.com")
                .password("1234").build();
        when(authUserRepository.existsByEmail(request.getEmail())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.register(request);
        });
        assertEquals("Email already registered", ex.getMessage());
        verify(authUserRepository, never()).save(any());
    }

    @Test
    void register_roleNotFound_shouldThrow() {
        RegisterRequest request = RegisterRequest.builder()
                .name("Test User")
                .age(20)
                .email("test@email.com")
                .password("1234").build();
        when(authUserRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            authService.register(request);
        });
        assertEquals("Default role not found", ex.getMessage());
        verify(authUserRepository, never()).save(any());
    }
}
