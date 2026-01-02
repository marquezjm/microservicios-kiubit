package com.kubit.authservice.service.impl;

import com.kubit.authservice.domain.entity.*;
import com.kubit.authservice.domain.repository.AuthUserRepository;
import com.kubit.authservice.domain.repository.RoleRepository;
import com.kubit.authservice.service.AuthService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthServiceImplIntegrationTest {

    @Autowired
    private AuthService authService;

    @Autowired
    private AuthUserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        // Crea el rol 'ROLE_USER' antes de cada test
        if (roleRepository.findByName("ROLE_USER").isEmpty()) {
            Role role = Role.builder()
                    .name("ROLE_USER")
                    .build();
            roleRepository.save(role);
        }
    }

    @Test
    void register_shouldPersistUserAndEncryptPassword() {
        RegisterRequest req = RegisterRequest.builder()
                .name("Test User")
                .age(22)
                .email("inttest@email.com")
                .password("secret").build();

        authService.register(req);

        AuthUser persisted = userRepository.findByEmail(req.getEmail()).orElse(null);
        assertNotNull(persisted);
        assertEquals(req.getEmail(), persisted.getEmail());
        assertEquals(AuthUserStatus.ACTIVE, persisted.getStatus());
        // La contraseña guardada debe ser hash, NO igual que el texto plano
        assertNotEquals(req.getPassword(), persisted.getPasswordHash());
        assertTrue(persisted.getRoles().stream().anyMatch(r -> "ROLE_USER".equals(r.getName())));
    }

    @Test
    void register_withDuplicateEmail_shouldThrowException() {
        RegisterRequest req1 = RegisterRequest.builder()
                .name("User1")
                .age(21)
                .email("duplicate@email.com")
                .password("pw1").build();

        RegisterRequest req2 = RegisterRequest.builder()
                .name("User2")
                .age(25)
                .email("duplicate@email.com")
                .password("pw2").build();

        authService.register(req1);

        // El segundo intento debe fallar por email duplicado
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.register(req2);
        });
        assertEquals("Email already registered", ex.getMessage());
    }

    @Test
    void register_withoutRole_shouldThrowException() {
        // Borra el rol para simular ausencia
        roleRepository.findByName("ROLE_USER")
            .ifPresent(roleRepository::delete);

        RegisterRequest req = RegisterRequest.builder()
                .name("User3")
                .age(30)
                .email("norole@email.com")
                .password("pw3").build();

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> {
            authService.register(req);
        });
        assertEquals("Default role not found", ex.getMessage());
    }

    @Test
    void login_withValidCredentials_shouldReturnUser() {
        RegisterRequest regReq = RegisterRequest.builder()
                .name("Login User")
                .age(28)
                .email("loginuser@email.com")
                .password("loginpw").build();

        authService.register(regReq);

        LoginRequest loginReq = LoginRequest.builder()
                .email("loginuser@email.com")
                .password("loginpw")
                .build();

        UserLoginResponse loggedInUser = authService.login(loginReq);
        assertNotNull(loggedInUser);
        assertEquals(regReq.getEmail(), loggedInUser.getUser().getEmail());
    }

    @Test
    void login_withInvalidCredentials_shouldThrowException() {
        LoginRequest loginReq = LoginRequest.builder()
                .email("nonexistent@email.com")
                .password("wrongpw")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.login(loginReq);
        });
        assertEquals("Invalid credentials", ex.getMessage());
    
    }

    @Test
    void login_withWrongPassword_shouldThrowException() {
        RegisterRequest regReq = RegisterRequest.builder()
                .name("Wrong Password User")
                .age(28)
                .email("wrongpassworduser@email.com")
                .password("correctpw").build();

        authService.register(regReq);

        LoginRequest loginReq = LoginRequest.builder()
                .email("wrongpassworduser@email.com")
                .password("wrongpw")
                .build();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            authService.login(loginReq);
        });
        assertEquals("Invalid credentials", ex.getMessage());
    }

    
}