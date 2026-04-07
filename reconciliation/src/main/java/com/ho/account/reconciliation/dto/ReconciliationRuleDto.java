package com.ho.account.reconciliation.dto;

import com.ho.account.reconciliation.domain.ReconciliationRule;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 대사 규칙 (ReconciliationRule) 응답 DTO
 */
@Data
@Builder
public class ReconciliationRuleDto {
    private Long id;
    private Long reconciliationUnitId; // FK
    private String name;
    private String ruleDefinitionJson;
    private ReconciliationRule.ToleranceType toleranceType;
    private BigDecimal toleranceValue;
    private Integer priority;
    private boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String auditUser;

    public static ReconciliationRuleDto fromEntity(ReconciliationRule entity) {
        return ReconciliationRuleDto.builder()
                .id(entity.getId())
                .reconciliationUnitId(entity.getReconciliationUnit() != null ? entity.getReconciliationUnit().getId() : null)
                .name(entity.getName())
                .ruleDefinitionJson(entity.getRuleDefinitionJson())
                .toleranceType(entity.getToleranceType())
                .toleranceValue(entity.getToleranceValue())
                .priority(entity.getPriority())
                .isActive(entity.isActive())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .auditUser(entity.getAuditUser())
                .build();
    }
}
