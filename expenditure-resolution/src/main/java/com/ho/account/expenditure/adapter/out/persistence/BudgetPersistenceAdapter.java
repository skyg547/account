package com.ho.account.expenditure.adapter.out.persistence;

import com.ho.account.expenditure.application.port.out.BudgetPersistencePort;
import com.ho.account.expenditure.domain.Budget;
import com.ho.account.expenditure.repository.BudgetRepository;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Department;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class BudgetPersistenceAdapter implements BudgetPersistencePort {

    private final BudgetRepository repository;

    public BudgetPersistenceAdapter(BudgetRepository repository) {
        this.repository = repository;
    }

    @Override
    public Optional<Budget> findByYearMonthAndDepartmentAndAccountSubject(
            String yearMonth, Department department, AccountSubject accountSubject) {
        return repository.findByYearMonthAndDepartmentAndAccountSubject(yearMonth, department, accountSubject);
    }

    @Override
    public Budget save(Budget budget) {
        return repository.save(budget);
    }
}
