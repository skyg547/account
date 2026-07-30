package com.ho.account.budget.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 예산 금액의 단일 정밀도 정책입니다.
 *
 * <p>DB의 {@code DECIMAL(19,2)}와 도메인 계산의 표현 범위가 다르면 저장 시점에
 * 조용한 반올림이나 overflow가 생길 수 있습니다. 따라서 금액이 도메인에 들어오는
 * 순간 scale 2로 정확히 표현 가능한지 확인하며, 반올림은 절대 수행하지 않습니다.</p>
 */
public final class BudgetPrecision {

    public static final int PRECISION = 19;
    public static final int SCALE = 2;

    private BudgetPrecision() {
    }

    public static BigDecimal amount(BigDecimal value, String fieldName) {
        if (value == null) {
            throw new IllegalArgumentException(fieldName + "은(는) 필수입니다.");
        }

        final BigDecimal normalized;
        try {
            normalized = value.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) 소수점 둘째 자리까지만 허용합니다: " + value,
                    exception);
        }
        if (normalized.precision() > PRECISION) {
            throw new IllegalArgumentException(
                    fieldName + "은(는) DECIMAL(19,2) 범위를 초과합니다: " + value);
        }
        return normalized;
    }

    public static BigDecimal positive(BigDecimal value, String fieldName) {
        BigDecimal normalized = amount(value, fieldName);
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + "은(는) 0보다 커야 합니다.");
        }
        return normalized;
    }

    public static BigDecimal nonNegative(BigDecimal value, String fieldName) {
        BigDecimal normalized = amount(value, fieldName);
        if (normalized.signum() < 0) {
            throw new IllegalArgumentException(fieldName + "은(는) 음수일 수 없습니다.");
        }
        return normalized;
    }

    public static BigDecimal zero() {
        return BigDecimal.ZERO.setScale(SCALE);
    }
}
