package com.ho.account.shared.infrastructure.security.application.port.out;

import com.ho.account.shared.infrastructure.security.domain.AuditLog;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogPersistencePort {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByEventType(String eventType);

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByTargetEntityAndTargetId(String targetEntity, String targetId);

    List<AuditLog> findByEventDateTimeBetween(LocalDateTime start, LocalDateTime end);
}

