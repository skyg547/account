package com.ho.account.shared.infrastructure.security.web.dto;

import com.ho.account.shared.infrastructure.security.domain.AuditLog;
import java.time.LocalDateTime;

public record AuditLogResponse(
        Long id,
        String eventType,
        LocalDateTime eventDateTime,
        String userId,
        String targetEntity,
        String targetId,
        String beforeData,
        String afterData,
        String status,
        String remarks,
        String ipAddress,
        String auditUser) {

    public static AuditLogResponse from(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getEventType(),
                auditLog.getEventDateTime(),
                auditLog.getUserId(),
                auditLog.getTargetEntity(),
                auditLog.getTargetId(),
                auditLog.getBeforeData(),
                auditLog.getAfterData(),
                auditLog.getStatus(),
                auditLog.getRemarks(),
                auditLog.getIpAddress(),
                auditLog.getAuditUser());
    }
}
