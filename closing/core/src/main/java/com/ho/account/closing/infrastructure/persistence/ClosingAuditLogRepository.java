package com.ho.account.closing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ClosingAuditLogRepository extends JpaRepository<ClosingAuditLogEntity, Long> {
}
