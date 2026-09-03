package com.ho.account.internalaudit.core.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Builder;

/**
 * Immutable audit trail record capturing actor lineage, action, aggregate target,
 * and correlation/idempotency keys for internal audit mutations.
 */
@Builder(toBuilder = true)
public record AuditLogEntry(
        Long id,
        String actor,
        String action,
        String aggregateType,
        String aggregateId,
        LocalDateTime actionTimestamp,
        String correlationId,
        String idempotencyKey,
        String detailsJson
) {
    public AuditLogEntry {
        if (actor == null || actor.isBlank()) {
            actor = "SYSTEM";
        }
        Objects.requireNonNull(action, "action must not be null");
        Objects.requireNonNull(aggregateType, "aggregateType must not be null");
        Objects.requireNonNull(aggregateId, "aggregateId must not be null");
        if (actionTimestamp == null) {
            actionTimestamp = LocalDateTime.now();
        }
    }
}
