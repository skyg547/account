package com.ho.account.cashflow.api.dto;

import com.ho.account.cashflow.core.domain.CashflowActivity;
import com.ho.account.cashflow.core.domain.CashflowLineItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CashflowLineItemRequest(
        @NotBlank String lineCode,
        @NotBlank String category,
        @NotNull CashflowActivity activity,
        @NotNull BigDecimal amount,
        @NotBlank String currency,
        String description) {

    public CashflowLineItem toDomain() {
        return new CashflowLineItem(lineCode, category, activity, amount, currency, description);
    }
}
