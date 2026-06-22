package com.ho.account.mart.core.infrastructure.persistence.jpa;

import com.ho.account.mart.core.infrastructure.persistence.entity.governance.AllowanceAuditLogEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaAllowanceAuditLogRepository extends JpaRepository<AllowanceAuditLogEntity, Long> {

    List<AllowanceAuditLogEntity> findTop10ByOrderByCreatedAtDesc();

    List<AllowanceAuditLogEntity> findByServiceNameOrderByCreatedAtDesc(String serviceName);
}
