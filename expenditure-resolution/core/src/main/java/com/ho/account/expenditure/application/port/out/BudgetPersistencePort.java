package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.Budget;
import java.util.Optional;

public interface BudgetPersistencePort {
    Optional<Budget> findByYearMonthAndDepartmentCodeAndAccountCode(
            String yearMonth,
            String departmentCode,
            String accountCode);

    Budget save(Budget budget);
}