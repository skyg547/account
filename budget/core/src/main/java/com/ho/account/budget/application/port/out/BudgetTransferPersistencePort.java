package com.ho.account.budget.application.port.out;

import com.ho.account.budget.domain.BudgetTransfer;
import java.util.Optional;

public interface BudgetTransferPersistencePort {

    BudgetTransfer save(BudgetTransfer transfer);

    Optional<BudgetTransfer> findById(Long id);

    Optional<BudgetTransfer> findByIdForUpdate(Long id);

    Optional<BudgetTransfer> findByRequestKey(String requestKey);

    Optional<BudgetTransfer> findByRequestKeyForUpdate(String requestKey);
}
