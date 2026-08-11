package com.ho.account.auth.core.domain.model;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 개인용 액세스 토큰 (Personal Access Token, PAT) 도메인 엔티티 (Pure POJO).
 *
 * <p>🐣 [초보자를 위한 헥사고날 아키텍처 & DDD 설명]
 * 이 클래스는 시스템의 핵심 비즈니스 개념인 '개인용 액세스 토큰'을 표현하는 Pure Java POJO 도메인 모델입니다.
 *
 * 1. 기술 독립성 (Framework/Infrastructure Free):
 *    JPA의 `@Entity`, `@Column`, Spring의 `@Component` 등 특정 프레임워크나 데이터베이스 기술 종속적인 어노테이션이 전혀 존재하지 않습니다.
 *    따라서 DB 기술이 RDB(JPA)에서 NoSQL(MongoDB)이나 In-Memory 캐시(Redis)로 바뀌더라도 이 도메인 모델은 전혀 변경되지 않습니다.
 *
 * 2. 캡슐화된 비즈니스 로직:
 *    상태 변경(예: 토큰 폐기 `revoke()`) 및 상태 계산(예: 만료 여부에 따른 실질 상태 계산 `getEffectiveStatus()`) 등의
 *    핵심 비즈니스 규칙을 도메인 모델 내부로 캡슐화하여 서비스나 영속성 계층에 비즈니스 로직이 파편화되는 것을 방지합니다.
 * </p>
 */
public class PersonalAccessToken {

    private final String id;
    private final String username;
    private final String tokenName;
    private final String tokenPrefix;
    private final String tokenHash;
    private String status;
    private final LocalDateTime expiresAt;
    private LocalDateTime lastUsedAt;
    private final LocalDateTime createdAt;

    public PersonalAccessToken(
            String id,
            String username,
            String tokenName,
            String tokenPrefix,
            String tokenHash,
            String status,
            LocalDateTime expiresAt,
            LocalDateTime lastUsedAt,
            LocalDateTime createdAt) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.username = Objects.requireNonNull(username, "username must not be null");
        this.tokenName = Objects.requireNonNull(tokenName, "tokenName must not be null");
        this.tokenPrefix = Objects.requireNonNull(tokenPrefix, "tokenPrefix must not be null");
        this.tokenHash = Objects.requireNonNull(tokenHash, "tokenHash must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.expiresAt = Objects.requireNonNull(expiresAt, "expiresAt must not be null");
        this.lastUsedAt = lastUsedAt;
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
    }

    /**
     * 토큰 신규 생성용 도메인 생성자
     */
    public static PersonalAccessToken create(
            String id,
            String username,
            String tokenName,
            String tokenPrefix,
            String tokenHash,
            LocalDateTime expiresAt,
            LocalDateTime createdAt) {
        return new PersonalAccessToken(
                id, username, tokenName, tokenPrefix, tokenHash, "ACTIVE", expiresAt, null, createdAt);
    }

    /**
     * 토큰 폐기 (Revoke) 비즈니스 메서드
     */
    public void revoke() {
        this.status = "REVOKED";
    }

    /**
     * 현재 시각 기준 만료 여부를 판별하여 실질적 상태(ACTIVE, REVOKED, EXPIRED)를 반환합니다.
     */
    public String getEffectiveStatus(LocalDateTime now) {
        if ("ACTIVE".equals(this.status) && expiresAt != null && expiresAt.isBefore(now)) {
            return "EXPIRED";
        }
        return this.status;
    }

    /**
     * 토큰 사용 일시 업데이트
     */
    public void markUsed(LocalDateTime usedAt) {
        this.lastUsedAt = usedAt;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getTokenName() {
        return tokenName;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getLastUsedAt() {
        return lastUsedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
