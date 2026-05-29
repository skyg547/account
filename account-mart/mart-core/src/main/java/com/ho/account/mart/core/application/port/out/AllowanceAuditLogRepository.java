package com.ho.account.mart.core.application.port.out;

import com.ho.account.mart.core.domain.governance.AllowanceAuditLog;
import java.util.List;

/**
 * [Outbound Port] 감사 로그 데이터 접근 인터페이스
 */
public interface AllowanceAuditLogRepository {
    List<AllowanceAuditLog> findTop10ByOrderByCreatedAtDesc();
    List<AllowanceAuditLog> findByServiceNameOrderByCreatedAtDesc(String serviceName);
    AllowanceAuditLog save(AllowanceAuditLog auditLog);
}
