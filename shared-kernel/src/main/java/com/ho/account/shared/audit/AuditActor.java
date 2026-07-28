package com.ho.account.shared.audit;

/**
 * Actor context used for audit trails.
 */
public record AuditActor(
        String userId,
        String ipAddress) {
}

