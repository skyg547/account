package com.ho.account.internalaudit.core.application.port.out;

import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import java.util.List;
import java.util.Optional;

/**
 * Outbound persistence port for audit logs.
 * Enforces strict append-only semantics: records can only be appended and queried.
 * No update or delete operations are exposed.
 */
public interface AuditLogPersistencePort {

    AuditLogEntry append(AuditLogEntry entry);

    List<AuditLogEntry> findByAggregate(String aggregateType, String aggregateId);

    List<AuditLogEntry> findByCorrelationId(String correlationId);

    Optional<AuditLogEntry> findByIdempotencyKey(String idempotencyKey);

    List<AuditLogEntry> findAll();
}
