package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ClosingTask;
import java.util.Optional;

public interface ClosingTaskPersistencePort {
    ClosingTask save(ClosingTask closingTask);
    Optional<ClosingTask> findById(Long id);
}
