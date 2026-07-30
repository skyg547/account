package com.ho.account.budget.application.port.in;

import com.ho.account.budget.domain.BudgetExecution;
import com.ho.account.budget.domain.BudgetPlan;
import com.ho.account.budget.domain.BudgetTransfer;
import java.util.Optional;

/**
 * API와 Batch가 공유하는 예산 통제 진입점입니다.
 */
public interface BudgetManagementUseCase {

    BudgetPlan createPlan(CreateBudgetPlanCommand command);

    BudgetPlan approvePlan(Long planId, String actor);

    Optional<BudgetPlan> findPlan(String yearMonth, String departmentCode, String accountCode);

    BudgetTransfer requestTransfer(RequestBudgetTransferCommand command);

    BudgetTransfer approveTransfer(Long transferId, String actor);

    BudgetExecution execute(ExecuteBudgetCommand command);

    BudgetExecution cancelExecution(Long executionId, String actor);
}
