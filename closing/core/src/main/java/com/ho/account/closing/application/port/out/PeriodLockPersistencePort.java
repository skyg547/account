package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.PeriodLock;
import java.util.Optional;

public interface PeriodLockPersistencePort {
    PeriodLock save(PeriodLock periodLock);
    Optional<PeriodLock> findByFiscalPeriodId(Long fiscalPeriodId);
    void delete(PeriodLock periodLock);
}
