package com.ho.account.reconciliation.api.dto;

import com.ho.account.reconciliation.application.port.in.ReconciliationRuleCommand;
import com.ho.account.reconciliation.domain.ReconciliationRule;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.Data;

/**
 * 대사 규칙(ReconciliationRule) HTTP 요청 DTO입니다.
 *
 * <p>초보자용 설명: API는 단위 ID만 받습니다. core 서비스가 해당 단위를 조회한 뒤 규칙을 연결하므로,
 * 컨트롤러가 JPA 엔티티를 직접 조립하지 않습니다.</p>
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

    public ReconciliationRuleCommand toCommand() {
        return new ReconciliationRuleCommand(
                reconciliationUnitId,
                name,
                ruleDefinitionJson,
                toleranceType,
                toleranceValue,
                priority,
                isActive);
    }
}