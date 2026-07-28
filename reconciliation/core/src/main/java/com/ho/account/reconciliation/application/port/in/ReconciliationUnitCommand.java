package com.ho.account.reconciliation.application.port.in;

import com.ho.account.reconciliation.domain.ReconciliationUnit;
import java.util.Objects;

/**
 * 대사 단위 생성/수정 유즈케이스 입력값입니다.
 *
 * <p>초보자용 설명: API DTO는 HTTP JSON과 화면 검증을 담당하고, 이 command는 core 업무가
 * 실제로 필요한 값만 담습니다. Batch나 내부 호출이 API를 거치지 않아도 같은 정합성 검증을 적용합니다.</p>
 */
public record ReconciliationUnitCommand(
        String name,
        String description,
        ReconciliationUnit.ReconciliationFrequency frequency,
        ReconciliationUnit.ReconciliationType reconciliationType,
        String criteriaJson,
        boolean active
) {
    public ReconciliationUnitCommand {
        name = requireText(name, "name");
        description = trimToNull(description);
        frequency = Objects.requireNonNull(frequency, "frequency is required");
        reconciliationType = Objects.requireNonNull(reconciliationType, "reconciliationType is required");
        criteriaJson = trimToNull(criteriaJson);
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