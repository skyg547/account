package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ClosingAuditLog;

public interface ClosingAuditLogPersistencePort {
    void save(ClosingAuditLog auditLog);
}
