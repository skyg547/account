package com.ho.account.audit.application.port.in;

import com.ho.account.audit.domain.AuditLog;
import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogUseCase {

    AuditLog logEvent(LogCommand command);

    List<AuditLog> getLogsByEventType(String eventType);

    List<AuditLog> getLogsByUser(String userId);

    List<AuditLog> getLogsByTarget(String targetEntity, String targetId);

    List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end);

    record LogCommand(
            String eventType,
            String userId,
            String targetEntity,
            String targetId,
            String beforeData,
            String afterData,
            String status,
            String remarks,
            String ipAddress) {
    }
}

