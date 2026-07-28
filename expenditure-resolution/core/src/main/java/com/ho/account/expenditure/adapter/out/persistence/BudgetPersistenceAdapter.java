package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.BudgetPersistencePort;
import com.ho.account.expenditure.domain.Budget;
import com.ho.account.expenditure.repository.BudgetRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class BudgetPersistenceAdapter implements BudgetPersistencePort {

    private final BudgetRepository repository;

    public BudgetPersistenceAdapter(BudgetRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Budget> findByYearMonthAndDepartmentCodeAndAccountCode(
            String yearMonth,
            String departmentCode,
            String accountCode) {
        return repository.findByYearMonthAndDeptCodeAndAccountCode(yearMonth, departmentCode, accountCode);
    }

    @Override
    public Budget save(Budget budget) {
        return repository.save(budget);
    }
}