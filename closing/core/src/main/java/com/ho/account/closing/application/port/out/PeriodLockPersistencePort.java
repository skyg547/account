package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.PeriodLock;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import java.util.Optional;

public interface PeriodLockPersistencePort {
    PeriodLock save(PeriodLock periodLock);
    Optional<PeriodLock> findByFiscalPeriod(FiscalPeriod fiscalPeriod);
    void delete(PeriodLock periodLock);
}
