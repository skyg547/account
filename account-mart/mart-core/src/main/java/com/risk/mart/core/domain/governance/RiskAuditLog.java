package com.risk.mart.core.domain.governance;

import lombok.*;
import java.time.LocalDateTime;

/**
 * [Governance] 통합 리스크 감사 로그 (Risk Audit Log) 도메인 모델
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskAuditLog {
    private Long id;
    private String serviceName;
    private String actionType;
    private String status;
    private String executionParam;
    private String executedBy;
    private String errorMessage;
    private LocalDateTime createdAt;
    private Long durationMs;
}
