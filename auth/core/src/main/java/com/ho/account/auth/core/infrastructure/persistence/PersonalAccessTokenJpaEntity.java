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

    @Column(name = "username", nullable = false, length = 80)
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

    /**
     * Domain POJO 객체를 JPA Entity로 변환합니다.
     */
    public static PersonalAccessTokenJpaEntity fromDomain(com.ho.account.auth.core.domain.model.PersonalAccessToken domain) {
        if (domain == null) {
            return null;
        }
        PersonalAccessTokenJpaEntity entity = new PersonalAccessTokenJpaEntity(
                domain.getId(),
                domain.getUsername(),
                domain.getTokenName(),
                domain.getTokenPrefix(),
                domain.getTokenHash(),
                domain.getStatus(),
                domain.getExpiresAt(),
                domain.getCreatedAt());
        entity.lastUsedAt = domain.getLastUsedAt();
        return entity;
    }

    /**
     * JPA Entity를 Pure Domain POJO 객체로 변환합니다 (Data Mapping).
     */
    public com.ho.account.auth.core.domain.model.PersonalAccessToken toDomain() {
        return new com.ho.account.auth.core.domain.model.PersonalAccessToken(
                this.id,
                this.username,
                this.tokenName,
                this.tokenPrefix,
                this.tokenHash,
                this.status,
                this.expiresAt,
                this.lastUsedAt,
                this.createdAt);
    }

    public void revoke() {
        this.status = "REVOKED";
    }

    public void markUsed() {
        this.lastUsedAt = LocalDateTime.now();
    }
}
