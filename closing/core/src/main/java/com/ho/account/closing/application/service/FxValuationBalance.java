package com.ho.account.closing.application.service;

import java.math.BigDecimal;

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
        foreignEndingBalance = foreignEndingBalance == null ? BigDecimal.ZERO : foreignEndingBalance;
        accountCode = accountCode.trim();
        currencyCode = currencyCode.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}