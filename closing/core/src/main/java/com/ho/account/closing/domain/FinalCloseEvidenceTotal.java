package com.ho.account.closing.domain;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * One account/currency reconciliation dimension in a final-close evidence snapshot.
 * Amounts retain their original {@link BigDecimal} precision; equality is evaluated numerically.
 */
public record FinalCloseEvidenceTotal(
        String accountCode,
        String currencyCode,
        BigDecimal sourceTotal,
        BigDecimal postedTotal) {

    public FinalCloseEvidenceTotal {
        accountCode = requireText(accountCode, "accountCode");
        currencyCode = requireText(currencyCode, "currencyCode").toUpperCase(Locale.ROOT);
        sourceTotal = requirePersistableAmount(sourceTotal, "sourceTotal");
        postedTotal = requirePersistableAmount(postedTotal, "postedTotal");
    }

    public String dimensionKey() {
        return accountCode + '\u0000' + currencyCode;
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        return value.trim();
    }

    private static BigDecimal requirePersistableAmount(BigDecimal value, String field) {
        Objects.requireNonNull(value, field + " must not be null");
        BigDecimal normalized = value.stripTrailingZeros();
        int fractionalDigits = Math.max(normalized.scale(), 0);
        int integerDigits = Math.max(normalized.precision() - normalized.scale(), 0);
        // Fail before persistence: NUMERIC(38,18) permits 18 fractional and 20 integer digits.
        // Trailing zeros may be removed, but financial amounts are never rounded to fit the column.
        if (fractionalDigits > 18 || integerDigits > 20) {
            throw new IllegalArgumentException(
                    field + " exceeds PostgreSQL NUMERIC(38,18) without rounding");
        }
        return value;
    }
}
