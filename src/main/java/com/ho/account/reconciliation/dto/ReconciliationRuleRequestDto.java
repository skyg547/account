package com.ho.account.reconciliation.dto;

import com.ho.account.reconciliation.domain.ReconciliationRule;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 대사 규칙 (ReconciliationRule) 요청 DTO
 */
@Data
public class ReconciliationRuleRequestDto {
    @NotNull
    private Long reconciliationUnitId; // 연관된 ReconciliationUnit의 ID

    @NotBlank
    @Size(max = 100)
    private String name;

    private String ruleDefinitionJson; // JSON string for flexible rule definition

    @NotNull
    private ReconciliationRule.ToleranceType toleranceType;

    private BigDecimal toleranceValue;

    @NotNull
    @Min(0)
    private Integer priority;

    private boolean isActive = true;

    // Method to convert DTO to Entity can be useful, but requires fetching ReconciliationUnit
}
