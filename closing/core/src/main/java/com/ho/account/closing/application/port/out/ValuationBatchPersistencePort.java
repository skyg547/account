package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ValuationBatch;

public interface ValuationBatchPersistencePort {
    ValuationBatch save(ValuationBatch valuationBatch);
}
