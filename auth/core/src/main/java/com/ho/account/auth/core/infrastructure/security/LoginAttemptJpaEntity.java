package com.ho.account.auth.core.infrastructure.security;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "auth_login_attempts")
class LoginAttemptJpaEntity {

    @Id
    @Column(name = "username", nullable = false, length = 80)
    private String username;

    @Column(name = "failure_count", nullable = false)
    private int failureCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_failure_reason", length = 120)
    private String lastFailureReason;

    @Column(name = "last_failure_at")
    private Instant lastFailureAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LoginAttemptJpaEntity() {
    }

    LoginAttemptJpaEntity(String username) {
        this.username = username;
        this.failureCount = 0;
        this.updatedAt = Instant.now();
    }

    void recordFailure(String reason, int maxFailures, long lockDurationMinutes, Clock clock) {
        Instant now = clock.instant();
        // An expired lock starts a new failure window; reads never remove the shared row.
        if (this.lockedUntil != null && !now.isBefore(this.lockedUntil)) {
            this.failureCount = 0;
            this.lockedUntil = null;
        }
        this.failureCount = this.failureCount + 1;
        this.lastFailureReason = normalizeReason(reason);
        this.lastFailureAt = now;
        this.updatedAt = now;
        if (this.failureCount >= maxFailures) {
            this.lockedUntil = now.plus(lockDurationMinutes, ChronoUnit.MINUTES);
        }
    }

    boolean isLocked(Clock clock) {
        return lockedUntil != null && clock.instant().isBefore(lockedUntil);
    }

    int getFailureCount() {
        return failureCount;
    }

    Instant getLockedUntil() {
        return lockedUntil;
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "UNKNOWN";
        }
        String text = reason.trim();
        return text.length() > 120 ? text.substring(0, 120) : text;
    }
}
