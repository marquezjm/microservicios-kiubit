package com.kubit.authservice.domain.entity;

import static org.junit.jupiter.api.Assertions.*;

import com.kubit.authservice.domain.entity.AuthUser;
import com.kubit.authservice.domain.entity.RefreshToken;
import com.kubit.authservice.domain.repository.AuthUserRepository;
import com.kubit.authservice.domain.repository.RefreshTokenRepository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@DataJpaTest
class RefreshTokenIntegrationTest {

    @Autowired
    private RefreshTokenRepository tokenRepository;
    @Autowired
    private AuthUserRepository userRepository;

    @Test
    void shouldPersistRefreshToken() {
        AuthUser user = AuthUser.builder()
                                .email("tokenuser@test.com")
                                .passwordHash("secret")
                                .status(AuthUserStatus.ACTIVE)
                                .build();
        AuthUser savedUser = userRepository.saveAndFlush(user);

        RefreshToken token = RefreshToken.builder()
            .token("sometokenvalue123")
            .expiresAt(LocalDateTime.ofInstant(Instant.now().plusSeconds(3600), ZoneId.systemDefault()))
            .authUser(savedUser)
            .revoked(false)
            .build();

        RefreshToken saved = tokenRepository.save(token);

        assertNotNull(saved.getId());
        assertEquals("sometokenvalue123", saved.getToken());
        assertEquals(savedUser.getId(), saved.getAuthUser().getId());
    }

    @Test
    void shouldNotAllowNullToken() {
        AuthUser user = AuthUser.builder()
                                .email("nulltoken@test.com")
                                .passwordHash("secret")
                                .status(AuthUserStatus.ACTIVE)
                                .build();
        AuthUser savedUser = userRepository.saveAndFlush(user);

        RefreshToken token = RefreshToken.builder()
            .token(null)
            .expiresAt(LocalDateTime.ofInstant(Instant.now().plusSeconds(3600), ZoneId.systemDefault()))
            .authUser(savedUser)
            .revoked(false)
            .build();

        assertThrows(Exception.class, () -> tokenRepository.saveAndFlush(token));
    }
}
