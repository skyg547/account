package com.ho.account.common.adapter;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Department;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.contracts.expenditure.BudgetControlPort;
import com.ho.account.expenditure.repository.BudgetRepository;
import com.ho.account.expenditure.service.BudgetService;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BudgetControlAdapter implements BudgetControlPort {

    private final BudgetService budgetService;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final BudgetRepository budgetRepository;

    public BudgetControlAdapter(
            BudgetService budgetService,
            DepartmentPersistencePort departmentPersistencePort,
            AccountSubjectPersistencePort accountSubjectPersistencePort,
            BudgetRepository budgetRepository) {
        this.budgetService = budgetService;
        this.departmentPersistencePort = departmentPersistencePort;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.budgetRepository = budgetRepository;
    }

    @Override
    public void checkBudgetAvailability(String yearMonth, String departmentCode, String accountCode, BigDecimal amount) {
        Department department = departmentPersistencePort.findByCode(departmentCode)
                .orElseThrow(() -> new IllegalArgumentException("Department not found. code=" + departmentCode));
        AccountSubject accountSubject = accountSubjectPersistencePort.findByCode(accountCode)
                .orElseThrow(() -> new IllegalArgumentException("Account subject not found. code=" + accountCode));
        if (budgetRepository.findByYearMonthAndDepartmentAndAccountSubject(yearMonth, department, accountSubject).isEmpty()) {
            return;
        }
        budgetService.checkBudgetAvailability(yearMonth, department, accountSubject, amount);
    }
}
