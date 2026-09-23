package com.ho.account.cashflow.core.domain;

import java.math.BigDecimal;
import java.util.Objects;

public record CashflowLineItem(
        String lineCode,
        String category,
        CashflowActivity activity,
        BigDecimal amount,
        String currency,
        String description) {

    public CashflowLineItem {
        lineCode = requireText(lineCode, "lineCode");
        category = requireText(category, "category");
        activity = Objects.requireNonNull(activity, "activity must not be null");
        amount = CashflowAmounts.normalize(amount, "amount");
        currency = CashflowAmounts.normalizeCurrency(currency);
        description = description == null ? "" : description.trim();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
