package com.risk.mart.core.application.port.out;

import com.risk.mart.core.domain.governance.RiskAuditLog;
import java.util.List;

/**
 * [Outbound Port] 감사 로그 데이터 접근 인터페이스
 */
public interface RiskAuditLogRepository {
    List<RiskAuditLog> findTop10ByOrderByCreatedAtDesc();
    List<RiskAuditLog> findByServiceNameOrderByCreatedAtDesc(String serviceName);
    RiskAuditLog save(RiskAuditLog auditLog);
}
