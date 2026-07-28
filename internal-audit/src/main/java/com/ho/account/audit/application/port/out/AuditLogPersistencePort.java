package com.ho.account.audit.application.port.out;

import com.ho.account.audit.domain.AuditLog;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogPersistencePort {

    AuditLog save(AuditLog auditLog);

    List<AuditLog> findByEventType(String eventType);

    List<AuditLog> findByUserId(String userId);

    List<AuditLog> findByTargetEntityAndTargetId(String targetEntity, String targetId);

    List<AuditLog> findByEventDateTimeBetween(LocalDateTime start, LocalDateTime end);
}

