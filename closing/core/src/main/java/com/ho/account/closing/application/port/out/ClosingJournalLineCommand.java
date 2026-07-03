package com.ho.account.closing.application.port.out;

import java.math.BigDecimal;
import java.util.Objects;

public record ClosingJournalLineCommand(
        ClosingJournalSide side,
        String accountCode,
        BigDecimal amount,
        BigDecimal baseAmount,
        String description) {

    public ClosingJournalLineCommand {
        Objects.requireNonNull(side, "side must not be null");
        if (isBlank(accountCode)) {
            throw new IllegalArgumentException("accountCode must not be blank");
        }
        amount = requirePositive(amount, "amount");
        baseAmount = baseAmount == null ? amount : requirePositive(baseAmount, "baseAmount");
        accountCode = accountCode.trim();
        description = description == null ? "" : description.trim();
    }

    private static BigDecimal requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.signum() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
        return value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}