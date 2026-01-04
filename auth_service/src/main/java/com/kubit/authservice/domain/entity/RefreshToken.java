package com.kubit.authservice.domain.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "refresh_token")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RefreshToken {
    /*
     * 'id', 'bigint', 'NO', 'PRI', NULL, 'auto_increment'
     * 'auth_user_id', 'bigint', 'NO', 'MUL', NULL, ''
     * 'token', 'varchar(512)', 'NO', 'UNI', NULL, ''
     * 'expires_at', 'timestamp', 'NO', '', NULL, ''
     * 'revoked', 'tinyint(1)', 'YES', '', '0', ''
     * 'created_at', 'timestamp', 'YES', '', 'CURRENT_TIMESTAMP', 'DEFAULT_GENERATED'
     */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "auth_user_id")
    private AuthUser authUser;

    @Column(unique = true, nullable = false, length = 512)
    private String token;

    @Column(name = "device_id", length = 128)
    private String deviceId;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "revoked", nullable = false)
    private Boolean revoked;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
