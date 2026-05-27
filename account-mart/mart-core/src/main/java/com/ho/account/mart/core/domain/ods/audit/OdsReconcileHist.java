package com.ho.account.mart.core.domain.ods.audit;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * [순수 도메인 모델] 원장 대사 이력 (Reconciliation History)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OdsReconcileHist {
    private Long id;
    private LocalDate baseDate;
    private String sourceSystem;
    private String targetSystem;
    private String reconcileType; // 추가 (reconcileItem 대용 혹은 병행)
    private String reconcileItem;
    private BigDecimal sourceAmount;
    private BigDecimal targetAmount;
    private BigDecimal diffAmount;
    private String status;
    private LocalDateTime auditTimestamp;
}
