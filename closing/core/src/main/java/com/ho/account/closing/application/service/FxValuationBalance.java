package com.ho.account.closing.application.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * Signed FX balance where debit is positive and credit is negative.
 *
 * <p>Both transaction-currency and reporting-currency balances must use the same sign convention.
 * Keeping the sign lets the core handle normal and abnormal balances without guessing from account type.</p>
 */
public record FxValuationBalance(
        String accountCode,
        String currencyCode,
        BigDecimal foreignEndingBalance,
        BigDecimal bookReportingAmount) {

    public FxValuationBalance {
        if (isBlank(accountCode)) {
            throw new IllegalArgumentException("accountCode must not be blank");
        }
        if (isBlank(currencyCode)) {
            throw new IllegalArgumentException("currencyCode must not be blank");
        }
        Objects.requireNonNull(foreignEndingBalance, "foreignEndingBalance must not be null");
        accountCode = accountCode.trim();
        currencyCode = currencyCode.trim().toUpperCase(Locale.ROOT);
        if (!currencyCode.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("currencyCode must be a 3-letter currency code");
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
