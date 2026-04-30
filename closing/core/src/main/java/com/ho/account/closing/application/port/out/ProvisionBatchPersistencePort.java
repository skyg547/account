package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ProvisionBatch;

public interface ProvisionBatchPersistencePort {
    ProvisionBatch save(ProvisionBatch provisionBatch);
}
