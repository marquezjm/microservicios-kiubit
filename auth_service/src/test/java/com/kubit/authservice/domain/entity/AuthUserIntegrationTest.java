package com.kubit.authservice.domain.entity;

import com.kubit.authservice.domain.repository.AuthUserRepository;
import com.kubit.authservice.domain.repository.RoleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class AuthUserIntegrationTest {
    @Autowired
    private AuthUserRepository authUserRepository;
    @Autowired
    private RoleRepository roleRepository;

    private Role studentRole, adminRole;

    @BeforeEach
    void setupRoles() {
        studentRole = roleRepository.save(Role.builder().name("STUDENT").build());
        adminRole = roleRepository.save(Role.builder().name("ADMIN").build());
    }

    @Test
    @DisplayName("No permite dos usuarios con el mismo email")
    void shouldNotAllowDuplicateEmail() {
        AuthUser u1 = AuthUser.builder()
                .email("duplicate@kiubit.com")
                .passwordHash("abc1234")
                .status(AuthUserStatus.ACTIVE)
                .roles(Set.of(studentRole))
                .build();
        authUserRepository.save(u1);
        AuthUser u2 = AuthUser.builder()
                .email("duplicate@kiubit.com")
                .passwordHash("xyz7890")
                .status(AuthUserStatus.ACTIVE)
                .roles(Set.of(studentRole))
                .build();
        assertThrows(DataIntegrityViolationException.class, () -> {
            authUserRepository.save(u2);
            authUserRepository.flush();
        });
    }

    @Test
    @DisplayName("Debe asociar uno o más roles a un usuario")
    void shouldRequireAtLeastOneRole() {
        AuthUser user = AuthUser.builder()
                .email("rolerequired@kiubit.com")
                .passwordHash("supersecr")
                .status(AuthUserStatus.ACTIVE)
                .roles(Set.of(studentRole, adminRole))
                .build();
        AuthUser saved = authUserRepository.save(user);
        assertEquals(2, saved.getRoles().size());
        assertTrue(saved.getRoles().contains(studentRole));
        assertTrue(saved.getRoles().contains(adminRole));
    }

    @Test
    @DisplayName("Debe persistir el estado (status) del usuario")
    void shouldPersistAuthUserStatus() {
        AuthUser user = AuthUser.builder()
                .email("status@kiubit.com")
                .passwordHash("passpass")
                .status(AuthUserStatus.PENDING_PROFILE)
                .roles(Set.of(studentRole))
                .build();
        AuthUser saved = authUserRepository.save(user);
        assertEquals(AuthUserStatus.PENDING_PROFILE, saved.getStatus());
    }

    @Test
    @DisplayName("No permite usuario sin passwordHash")
    void shouldNotAllowNullPasswordHash() {
        AuthUser user = AuthUser.builder()
                .email("pwrequired@kiubit.com")
                .status(AuthUserStatus.ACTIVE)
                .roles(Set.of(studentRole))
                .build();
        assertThrows(DataIntegrityViolationException.class, () -> {
            authUserRepository.save(user);
            authUserRepository.flush();
        });
    }
}
