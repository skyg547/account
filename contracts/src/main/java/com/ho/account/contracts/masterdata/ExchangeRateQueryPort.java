package com.ho.account.contracts.masterdata;

import java.time.LocalDate;
import java.util.Optional;

/** Provider query contract for the latest rate effective on or before a business date. */
public interface ExchangeRateQueryPort {

    Optional<ExchangeRateRef> findLatestRateAt(
            String fromCurrencyCode,
            String toCurrencyCode,
            LocalDate effectiveDate);
}
