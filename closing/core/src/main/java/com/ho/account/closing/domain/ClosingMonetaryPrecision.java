package com.ho.account.closing.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Closing's posting boundary mirrors Journal's amount (19,2) and exchange-rate (19,8) contract.
 * Model rounding belongs to the calculation; this boundary never silently rounds a posted value.
 */
public final class ClosingMonetaryPrecision {

    private ClosingMonetaryPrecision() {
    }

    public static BigDecimal amount(BigDecimal value) {
        return normalize(value, 2, "amount");
    }

    public static BigDecimal exchangeRate(BigDecimal value) {
        BigDecimal normalized = normalize(value, 8, "exchangeRate");
        if (normalized.signum() <= 0) {
            throw new IllegalArgumentException("exchangeRate must be positive");
        }
        return normalized;
    }

    private static BigDecimal normalize(BigDecimal value, int scale, String name) {
        if (value == null) {
            throw new IllegalArgumentException(name + " must not be null");
        }
        final BigDecimal normalized;
        try {
            normalized = value.setScale(scale, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(name + " must be exactly representable at scale " + scale, exception);
        }
        if (normalized.precision() > 19) {
            throw new IllegalArgumentException(name + " must not exceed precision 19");
        }
        return normalized;
    }
}
