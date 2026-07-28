package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.mart.core.application.port.out.AllowanceAuditLogRepository;
import com.ho.account.mart.core.domain.governance.AllowanceAuditLog;
import com.ho.account.mart.core.infrastructure.persistence.entity.governance.AllowanceAuditLogEntity;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaAllowanceAuditLogRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AllowanceAuditLogPersistenceAdapter implements AllowanceAuditLogRepository {

    private final JpaAllowanceAuditLogRepository jpaRepository;

    @Override
    public List<AllowanceAuditLog> findTop10ByOrderByCreatedAtDesc() {
        return jpaRepository.findTop10ByOrderByCreatedAtDesc().stream()
                .map(this::main)
                .toList();
    }

    @Override
    public List<AllowanceAuditLog> findByServiceNameOrderByCreatedAtDesc(String serviceName) {
        return jpaRepository.findByServiceNameOrderByCreatedAtDesc(serviceName).stream()
                .map(this::main)
                .toList();
    }

    @Override
    public AllowanceAuditLog save(AllowanceAuditLog auditLog) {
        return main(jpaRepository.save(toEntity(auditLog)));
    }

    private AllowanceAuditLog main(AllowanceAuditLogEntity entity) {
        if (entity == null) {
            return null;
        }
        return AllowanceAuditLog.builder()
                .id(entity.getId())
                .serviceName(entity.getServiceName())
                .actionType(entity.getActionType())
                .status(entity.getStatus())
                .executionParam(entity.getExecutionParam())
                .executedBy(entity.getExecutedBy())
                .errorMessage(entity.getErrorMessage())
                .createdAt(entity.getCreatedAt())
                .durationMs(entity.getDurationMs())
                .build();
    }

    private AllowanceAuditLogEntity toEntity(AllowanceAuditLog domain) {
        if (domain == null) {
            return null;
        }
        LocalDateTime createdAt = domain.getCreatedAt() != null
                ? domain.getCreatedAt()
                : LocalDateTime.now();
        return AllowanceAuditLogEntity.builder()
                .id(domain.getId())
                .serviceName(domain.getServiceName())
                .actionType(domain.getActionType())
                .status(domain.getStatus())
                .executionParam(domain.getExecutionParam())
                .executedBy(domain.getExecutedBy())
                .errorMessage(domain.getErrorMessage())
                .createdAt(createdAt)
                .durationMs(domain.getDurationMs())
                .build();
    }
}
