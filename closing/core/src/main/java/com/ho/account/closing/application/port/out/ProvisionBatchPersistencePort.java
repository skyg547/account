package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ProvisionBatch;
import java.util.Optional;

public interface ProvisionBatchPersistencePort {
    ProvisionBatch save(ProvisionBatch provisionBatch);
    Optional<ProvisionBatch> findById(Long id);
    Optional<ProvisionBatch> findByExecutionKey(String executionKey);
}
