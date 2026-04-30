package com.ho.account.expenditure.application.port.out;

import com.ho.account.expenditure.domain.Budget;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Department;
import java.util.Optional;

public interface BudgetPersistencePort {
    Optional<Budget> findByYearMonthAndDepartmentAndAccountSubject(
            String yearMonth, Department department, AccountSubject accountSubject);
    Budget save(Budget budget);
}
