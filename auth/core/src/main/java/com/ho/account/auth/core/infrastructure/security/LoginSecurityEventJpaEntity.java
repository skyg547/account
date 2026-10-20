package com.ho.account.auth.core.infrastructure.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/** A credential-free snapshot of one committed login-attempt transition. */
@Entity
@Table(name = "auth_login_security_events")
class LoginSecurityEventJpaEntity {
    @Id
    @Column(name = "event_id", nullable = false, length = 36)
    private String eventId;

    @Column(name = "claimed_username", nullable = false, length = 80)
    private String claimedUsername;

    @Column(name = "authenticated_username", length = 80)
    private String authenticatedUsername;

    @Column(name = "event_type", nullable = false, length = 16)
    private String eventType;

    @Column(name = "reason_code", nullable = false, length = 120)
    private String reasonCode;

    @Column(name = "before_failure_count", nullable = false)
    private int beforeFailureCount;

    @Column(name = "after_failure_count", nullable = false)
    private int afterFailureCount;

    @Column(name = "before_locked_until")
    private Instant beforeLockedUntil;

    @Column(name = "after_locked_until")
    private Instant afterLockedUntil;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    protected LoginSecurityEventJpaEntity() {}

    LoginSecurityEventJpaEntity(String claimedUsername, String authenticatedUsername,
            String eventType, String reasonCode, int beforeFailureCount, int afterFailureCount,
            Instant beforeLockedUntil, Instant afterLockedUntil, Instant occurredAt) {
        this.eventId = UUID.randomUUID().toString();
        this.claimedUsername = claimedUsername;
        this.authenticatedUsername = authenticatedUsername;
        this.eventType = eventType;
        this.reasonCode = reasonCode;
        this.beforeFailureCount = beforeFailureCount;
        this.afterFailureCount = afterFailureCount;
        this.beforeLockedUntil = beforeLockedUntil;
        this.afterLockedUntil = afterLockedUntil;
        this.occurredAt = occurredAt;
    }

    String getClaimedUsername() { return claimedUsername; }
    String getEventType() { return eventType; }
    String getReasonCode() { return reasonCode; }
    Instant getOccurredAt() { return occurredAt; }
    int getBeforeFailureCount() { return beforeFailureCount; }
    int getAfterFailureCount() { return afterFailureCount; }
}
