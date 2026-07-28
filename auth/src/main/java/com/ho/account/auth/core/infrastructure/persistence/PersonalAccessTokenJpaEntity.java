package com.ho.account.auth.core.infrastructure.persistence;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "auth_personal_access_tokens")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalAccessTokenJpaEntity {

    @Id
    @Column(name = "id", length = 36)
    private String id;

    @Column(name = "username", nullable = false, length = 50)
    private String username;

    @Column(name = "token_name", nullable = false, length = 100)
    private String tokenName;

    @Column(name = "token_prefix", nullable = false, length = 20)
    private String tokenPrefix;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public PersonalAccessTokenJpaEntity(
            String id,
            String username,
            String tokenName,
            String tokenPrefix,
            String tokenHash,
            String status,
            LocalDateTime expiresAt,
            LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.tokenName = tokenName;
        this.tokenPrefix = tokenPrefix;
        this.tokenHash = tokenHash;
        this.status = status;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public void revoke() {
        this.status = "REVOKED";
    }

    public void markUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
