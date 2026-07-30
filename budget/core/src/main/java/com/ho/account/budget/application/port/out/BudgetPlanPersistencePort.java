package com.ho.account.budget.application.port.out;

import com.ho.account.budget.domain.BudgetPlan;
import java.util.List;
import java.util.Optional;

public interface BudgetPlanPersistencePort {

    BudgetPlan save(BudgetPlan plan);

    Optional<BudgetPlan> findById(Long id);

    Optional<BudgetPlan> findByIdForUpdate(Long id);

    Optional<BudgetPlan> findByPlanCode(String planCode);

    Optional<BudgetPlan> findByBusinessKey(
            String yearMonth, String departmentCode, String accountCode);

    Optional<BudgetPlan> findByBusinessKeyForUpdate(
            String yearMonth, String departmentCode, String accountCode);

    /**
     * DB가 id 오름차순으로 행 잠금을 획득해 연말 마감끼리 교착하지 않게 합니다.
     */
    List<BudgetPlan> findApprovedByFiscalYearForUpdateOrderById(String fiscalYear);
}
