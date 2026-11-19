package com.ho.account.contracts.journal;

import java.time.LocalDate;
import java.util.Objects;

/** Authoritative Master fiscal-period identity and inclusive accounting-date range. */
public record JournalPeriodScope(Long fiscalPeriodId, LocalDate startDate, LocalDate endDate) {
    public JournalPeriodScope {
        if (fiscalPeriodId == null || fiscalPeriodId <= 0) {
            throw new IllegalArgumentException("fiscalPeriodId must be positive");
        }
        Objects.requireNonNull(startDate, "startDate must not be null");
        Objects.requireNonNull(endDate, "endDate must not be null");
        if (endDate.isBefore(startDate)) {
            throw new IllegalArgumentException("endDate must not precede startDate");
        }
    }
}
