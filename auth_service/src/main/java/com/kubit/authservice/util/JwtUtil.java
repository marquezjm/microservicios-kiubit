package com.kubit.authservice.util;
import com.kubit.authservice.domain.entity.AuthUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.security.Key;
import java.security.SecureRandom;
import java.util.*;
import java.util.stream.Collectors;
import java.time.Instant;
import java.util.Base64;

@Component
public class JwtUtil {
    private final Key key;
    private final long jwtExpirationMs;
    private final long refreshExpirationMs;
    private final SecureRandom secureRandom = new SecureRandom();
    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.expirationMs}") long jwtExpirationMs,
        @Value("${jwt.refreshExpirationMs}") long refreshExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.jwtExpirationMs = jwtExpirationMs;
        this.refreshExpirationMs = refreshExpirationMs;
    }
    // --- ACCESS TOKEN: JWT ---
    /**
     * Genera un JWT para el usuario dado.
     */
    public String generateToken(AuthUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("email", user.getEmail());
        // Se asume que getRoles() devuelve Set<Role> y Role tiene getName()
        claims.put("roles", user.getRoles().stream().map(r -> r.getName()).collect(Collectors.toList()));
        // AuthUserStatus es un enum, se toma su nombre como string
        claims.put("status", user.getStatus().name());
        Instant now = Instant.now();
        Instant expiry = now.plusMillis(jwtExpirationMs);
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(user.getId().toString())
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(expiry))
            .signWith(key, SignatureAlgorithm.HS256)
            .compact();
    }
    /**
     * Valida el access token (JWT).
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
    // Métodos para extraer info del JWT
    public String extractEmail(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("email", String.class);
    }
    public String extractSubject(String token) {
        return extractAllClaims(token).getSubject();
    }
    public List<String> extractRoles(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("roles", List.class);
    }
    public String extractStatus(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("status", String.class);
    }
    public boolean isTokenExpired(String token) {
        Date expiration = extractAllClaims(token).getExpiration();
        return expiration.before(new Date());
    }
    public Claims extractAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
    }
    // --- REFRESH TOKEN: String aleatorio ---
    /**
     * Genera un refresh token seguro (no JWT, solo string largo con entropía).
     * Debe ser persistido en la base de datos junto a su expiración calculada en la capa de servicio.
     */
    public String generateRefreshToken(AuthUser user) {
        // String aleatorio seguro. Puedes usar solo UUID si prefieres mayor compatibilidad.
        byte[] randomBytes = new byte[64];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    /**
     * @return configuración en milisegundos de expiración para refresh tokens.
     * Se recomienda usar esto al crear y persistir el refresh token en la capa de servicio.
     */
    public long getRefreshExpirationMs() {
        return this.refreshExpirationMs;
    }
    /**
     * @return configuración en milisegundos de expiración para access tokens (JWT).
     * No suele ser necesario fuera de aquí.
     */
    public long getJwtExpirationMs() {
        return this.jwtExpirationMs;
    }
}