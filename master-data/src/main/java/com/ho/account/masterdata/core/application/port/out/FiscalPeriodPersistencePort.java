package com.ho.account.masterdata.core.application.port.out;

import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import java.util.Optional;

public interface FiscalPeriodPersistencePort {
    Optional<FiscalPeriod> findById(Long id);
    Optional<FiscalPeriod> findByIdForUpdate(Long id);
    Optional<FiscalPeriod> findByFiscalYearAndFiscalPeriod(String fiscalYear, String fiscalPeriod);
    FiscalPeriod save(FiscalPeriod fiscalPeriod);
}
