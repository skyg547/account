package com.ho.account.mart.core.domain.governance;

import lombok.*;
import java.time.LocalDateTime;

/**
 * [Governance] IFRS 9 대손충당금 감사 로그 도메인 모델
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AllowanceAuditLog {
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
