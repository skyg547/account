package com.ho.account.reconciliation.application.port.in;

import com.ho.account.reconciliation.domain.ReconciliationRule;
import java.math.BigDecimal;
import java.util.Objects;

/**
 * 대사 규칙 생성/수정 유즈케이스 입력값입니다.
 *
 * <p>초보자용 설명: 규칙은 어떤 대사 단위에 붙을지, 어떤 JSON 조건을 쓸지, 얼마까지 차이를
 * 허용할지를 정의합니다. core는 이 command를 기준으로 단위 조회와 규칙 저장 순서를 처리합니다.</p>
 */
public record ReconciliationRuleCommand(
        Long reconciliationUnitId,
        String name,
        String ruleDefinitionJson,
        ReconciliationRule.ToleranceType toleranceType,
        BigDecimal toleranceValue,
        Integer priority,
        boolean active
) {
    public ReconciliationRuleCommand {
        if (reconciliationUnitId == null || reconciliationUnitId <= 0) {
            throw new IllegalArgumentException("reconciliationUnitId must be greater than zero");
        }
        name = requireText(name, "name");
        ruleDefinitionJson = trimToNull(ruleDefinitionJson);
        toleranceType = Objects.requireNonNull(toleranceType, "toleranceType is required");
        priority = Objects.requireNonNull(priority, "priority is required");
        if (priority < 0) {
            throw new IllegalArgumentException("priority must be zero or greater");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}