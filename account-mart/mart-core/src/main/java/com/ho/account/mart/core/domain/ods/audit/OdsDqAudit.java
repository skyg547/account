package com.ho.account.mart.core.domain.ods.audit;

import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [순수 도메인 모델] 데이터 품질 검사 결과 (Data Quality Audit)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OdsDqAudit {
    private Long id;
    private LocalDate baseDate;
    private String tableName; // 추가
    private String accountNo;
    private String auditType;
    private String auditMessage;
    private String severity;
    private LocalDateTime auditTimestamp;
}
