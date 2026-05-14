package com.ho.account.contracts.masterdata;

import java.time.LocalDate;

public record FiscalPeriodRef(
        Long id,
        String fiscalYear,
        String fiscalPeriod,
        LocalDate startDate,
        LocalDate endDate,
        String closingStatus) {
}
