package com.ho.account.cashflow.core.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/** Shared precision rules for cashflow monetary values. */
public final class CashflowAmounts {

    public static final int SCALE = 2;
    public static final BigDecimal ZERO = BigDecimal.ZERO.setScale(SCALE);

    private CashflowAmounts() {
    }

    /**
     * Normalizes exact monetary input without silently discarding fractional value.
     * Values with more than two non-zero decimal places are rejected.
     */
    public static BigDecimal normalize(BigDecimal amount, String fieldName) {
        Objects.requireNonNull(amount, fieldName + " must not be null");
        try {
            return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException(fieldName + " must have at most two decimal places", exception);
        }
    }

    public static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            throw new IllegalArgumentException("currency is required");
        }
        String normalized = currency.trim().toUpperCase(Locale.ROOT);
        try {
            return Currency.getInstance(normalized).getCurrencyCode();
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("currency must be a valid ISO-4217 code", exception);
        }
    }
}
