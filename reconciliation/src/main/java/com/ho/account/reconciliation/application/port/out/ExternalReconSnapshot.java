package com.ho.account.reconciliation.application.port.out;

import java.math.BigDecimal;

public record ExternalReconSnapshot(long count, BigDecimal amount) {

    public ExternalReconSnapshot {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative");
        }
        amount = amount == null ? BigDecimal.ZERO : amount;
    }

    public static ExternalReconSnapshot zero() {
        return new ExternalReconSnapshot(0L, BigDecimal.ZERO);
    }
}
