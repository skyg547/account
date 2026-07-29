package com.ho.account.contracts.masterdata;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;

/** Immutable exchange-rate snapshot exposed without leaking the provider entity. */
public record ExchangeRateRef(
        String fromCurrencyCode,
        String toCurrencyCode,
        BigDecimal rate,
        LocalDate effectiveDate) {

    public ExchangeRateRef {
        fromCurrencyCode = normalizeCurrency(fromCurrencyCode, "fromCurrencyCode");
        toCurrencyCode = normalizeCurrency(toCurrencyCode, "toCurrencyCode");
        if (rate == null || rate.signum() <= 0) {
            throw new IllegalArgumentException("rate must be positive");
        }
        Objects.requireNonNull(effectiveDate, "effectiveDate must not be null");
    }

    private static String normalizeCurrency(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException(fieldName + " must be a 3-letter currency code");
        }
        return normalized;
    }
}
