package com.ho.account.audit.application.model;

/**
 * Actor context used for audit trails.
 */
public record AuditActor(
        String userId,
        String ipAddress) {
}

