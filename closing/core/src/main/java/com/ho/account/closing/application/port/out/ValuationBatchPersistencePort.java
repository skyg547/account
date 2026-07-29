package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ValuationBatch;
import java.util.Optional;

public interface ValuationBatchPersistencePort {
    ValuationBatch save(ValuationBatch valuationBatch);
    Optional<ValuationBatch> findById(Long id);
}
