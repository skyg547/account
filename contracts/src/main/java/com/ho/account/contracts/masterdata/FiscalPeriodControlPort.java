package com.ho.account.contracts.masterdata;

import java.util.Optional;

/**
 * Contract for querying and updating fiscal period closing status without exposing master-data entities.
 */
public interface FiscalPeriodControlPort {

    Optional<FiscalPeriodRef> findFiscalPeriodById(Long id);

    Optional<FiscalPeriodRef> findFiscalPeriod(String fiscalYear, String fiscalPeriod);

    FiscalPeriodRef updateClosingStatus(Long fiscalPeriodId, String closingStatus, String auditUser);
}
