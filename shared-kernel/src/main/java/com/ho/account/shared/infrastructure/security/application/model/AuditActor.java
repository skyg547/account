package com.ho.account.shared.infrastructure.security.application.model;

/**
 * Actor context used for audit trails.
 */
public record AuditActor(
        String userId,
        String ipAddress) {
}

