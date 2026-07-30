package com.ho.account.budget.application.port.out;

import com.ho.account.budget.domain.BudgetExecution;
import java.util.Optional;

public interface BudgetExecutionPersistencePort {

    BudgetExecution save(BudgetExecution execution);

    Optional<BudgetExecution> findById(Long id);

    Optional<BudgetExecution> findByIdForUpdate(Long id);

    Optional<BudgetExecution> findBySource(
            String sourceType, String sourceId, String sourceLineId);

    Optional<BudgetExecution> findBySourceForUpdate(
            String sourceType, String sourceId, String sourceLineId);
}
