package com.kubit.authservice.domain.entity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class AuthUserTest {

    private AuthUser authUser;
    private Role testRole;

    @BeforeEach
    void setUp() {
        testRole = Role.builder().id(1L).name("STUDENT").build();
        authUser = AuthUser.builder()
                .id(100L)
                .email("test@example.com")
                .passwordHash("hashpass")
                .status(AuthUserStatus.ACTIVE)
                .roles(Set.of(testRole))
                .build();
    }

    @Test
    void testConstructorAndGetters() {
        assertEquals(100L, authUser.getId());
        assertEquals("test@example.com", authUser.getEmail());
        assertEquals("hashpass", authUser.getPasswordHash());
        assertEquals(AuthUserStatus.ACTIVE, authUser.getStatus());
        assertNotNull(authUser.getRoles());
        assertEquals(1, authUser.getRoles().size());
        assertTrue(authUser.getRoles().contains(testRole));
    }

    @Test
    void testSetters() {
        authUser.setEmail("other@example.com");
        authUser.setPasswordHash("newhash");
        authUser.setStatus(AuthUserStatus.BLOCKED);
        assertEquals("other@example.com", authUser.getEmail());
        assertEquals("newhash", authUser.getPasswordHash());
        assertEquals(AuthUserStatus.BLOCKED, authUser.getStatus());
    }

    @Test
    void testPrePersistLifecycleSetsCreatedAtAndUpdatedAt() {
        authUser.onCreate();
        assertNotNull(authUser.getCreatedAt());
        assertNotNull(authUser.getUpdatedAt());
        assertEquals(authUser.getCreatedAt(), authUser.getUpdatedAt());
    }

    @Test
    void testPreUpdateLifecycleUpdatesUpdatedAtOnly() {
        LocalDateTime now = LocalDateTime.now();
        authUser.setCreatedAt(now.minusDays(1));
        authUser.setUpdatedAt(now.minusDays(1));
        authUser.onUpdate();
        assertNotNull(authUser.getUpdatedAt());
        assertNotEquals(authUser.getCreatedAt(), authUser.getUpdatedAt());
    }
}
