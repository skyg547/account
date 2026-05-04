package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.domain.ClosingAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClosingAuditLogRepository extends JpaRepository<ClosingAuditLog, Long> {
}
