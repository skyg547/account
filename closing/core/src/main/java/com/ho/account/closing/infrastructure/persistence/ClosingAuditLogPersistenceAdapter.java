package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ClosingAuditLogPersistencePort;
import com.ho.account.closing.domain.ClosingAuditLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ClosingAuditLogPersistenceAdapter implements ClosingAuditLogPersistencePort {
    private final ClosingAuditLogRepository closingAuditLogRepository;

    @Override
    public void save(ClosingAuditLog auditLog) {
        closingAuditLogRepository.save(auditLog);
    }
}
