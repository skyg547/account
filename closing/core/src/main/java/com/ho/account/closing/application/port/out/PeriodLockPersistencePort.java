package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.PeriodLock;
import java.util.Optional;

public interface PeriodLockPersistencePort {
    PeriodLock save(PeriodLock periodLock);
    PeriodLock saveAndFlush(PeriodLock periodLock);
    /** Returns the sole active lock; released rows remain as history. */
    Optional<PeriodLock> findByFiscalPeriodId(Long fiscalPeriodId);
}
