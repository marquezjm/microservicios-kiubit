package com.kubit.authservice.util;

import com.kubit.authservice.domain.entity.AuthUser;
import com.kubit.authservice.domain.entity.Role;

import io.jsonwebtoken.ExpiredJwtException;

import com.kubit.authservice.domain.entity.AuthUserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private final String jwtSecret = "SuperClaveJWTParaFirmaSegura123456SuperClaveJWTParaFirmaSegura123456"; // >=32
                                                                                                             // chars
    private final long jwtExpirationMs = 3600_000; // 1h
    private final long refreshExpirationMs = 604800_000; // 1 semana
    private AuthUser testUser;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil(jwtSecret, jwtExpirationMs, refreshExpirationMs);
        testUser = AuthUser.builder()
                .id(1L)
                .email("test@kiubit.mx")
                .passwordHash("no-matter")
                .status(AuthUserStatus.ACTIVE)
                .roles(Set.of(Role.builder().name("USER").build(), Role.builder().name("ADMIN").build()))
                .build();
    }

    @Test
    void shouldGenerateValidJwtToken() {
        String token = jwtUtil.generateToken(testUser);
        assertNotNull(token);
        assertTrue(jwtUtil.validateToken(token));
    }

    @Test
    void shouldExtractClaimsFromToken() {
        String token = jwtUtil.generateToken(testUser);
        assertEquals("test@kiubit.mx", jwtUtil.extractEmail(token));
        assertEquals("1", jwtUtil.extractSubject(token));
        assertEquals("ACTIVE", jwtUtil.extractStatus(token));
        assertEquals(Set.of("USER", "ADMIN"), Set.copyOf(jwtUtil.extractRoles(token)));
    }

    @Test
    void shouldDetectExpiredToken() throws InterruptedException {
        JwtUtil shortExpiryUtil = new JwtUtil(jwtSecret, 1, refreshExpirationMs); // Expira en 1 ms
        String token = shortExpiryUtil.generateToken(testUser);
        Thread.sleep(10);
        assertFalse(shortExpiryUtil.validateToken(token)); // El token debe ser inválido

        // Alternativo, atrapa la excepción al llamar explicitamente isTokenExpired() si
        // quieres
        assertThrows(ExpiredJwtException.class, () -> shortExpiryUtil.isTokenExpired(token));
    }

    @Test
    void shouldInvalidateModifiedToken() {
        String token = jwtUtil.generateToken(testUser);
        String invalidToken = token.substring(0, token.length() - 2) + "xx";
        assertFalse(jwtUtil.validateToken(invalidToken));
    }

    @Test
    void shouldGenerateSecureUniqueRefreshToken() {
        String one = jwtUtil.generateRefreshToken(testUser);
        String two = jwtUtil.generateRefreshToken(testUser);
        assertNotNull(one);
        assertNotNull(two);
        assertNotEquals(one, two);
        assertTrue(one.length() >= 80); // Debe ser largo por entropía/base64
        assertTrue(two.length() >= 80);
    }

    @Test
    void configMethodsReturnExpectedValues() {
        assertEquals(jwtExpirationMs, jwtUtil.getJwtExpirationMs());
        assertEquals(refreshExpirationMs, jwtUtil.getRefreshExpirationMs());
    }
}
