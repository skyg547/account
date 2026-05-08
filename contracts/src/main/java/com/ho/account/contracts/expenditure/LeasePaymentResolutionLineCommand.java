package com.ho.account.contracts.expenditure;

import java.math.BigDecimal;

public record LeasePaymentResolutionLineCommand(
        String debitAccountCode,
        BigDecimal amount,
        String detailDescription
) {
    public LeasePaymentResolutionLineCommand {
        if (debitAccountCode == null || debitAccountCode.isBlank()) {
            throw new IllegalArgumentException("debitAccountCode is required.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException("amount must be positive.");
        }
    }
}
