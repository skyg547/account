package com.ho.account.audit.infrastructure.persistence;

import com.ho.account.audit.application.port.out.AuditLogPersistencePort;
import com.ho.account.audit.domain.AuditLog;
import com.ho.account.audit.repository.AuditLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogJpaAdapter implements AuditLogPersistencePort {

    private final AuditLogRepository auditLogRepository;

    @Override
    public AuditLog save(AuditLog auditLog) {
        return auditLogRepository.save(auditLog);
    }

    @Override
    public List<AuditLog> findByEventType(String eventType) {
        return auditLogRepository.findByEventType(eventType);
    }

    @Override
    public List<AuditLog> findByUserId(String userId) {
        return auditLogRepository.findByUserId(userId);
    }

    @Override
    public List<AuditLog> findByTargetEntityAndTargetId(String targetEntity, String targetId) {
        return auditLogRepository.findByTargetEntityAndTargetId(targetEntity, targetId);
    }

    @Override
    public List<AuditLog> findByEventDateTimeBetween(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByEventDateTimeBetween(start, end);
    }
}

