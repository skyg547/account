package com.ho.account.expenditure.adapter.in.contract;

import com.ho.account.contracts.expenditure.BudgetControlPort;
import com.ho.account.expenditure.application.port.out.BudgetPersistencePort;
import com.ho.account.expenditure.application.service.BudgetService;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.DepartmentPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Department;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class BudgetControlAdapter implements BudgetControlPort {

    private final BudgetService budgetService;
    private final BudgetPersistencePort budgetPersistencePort;
    private final DepartmentPersistencePort departmentPersistencePort;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;

    public BudgetControlAdapter(
            BudgetService budgetService,
            BudgetPersistencePort budgetPersistencePort,
            DepartmentPersistencePort departmentPersistencePort,
            AccountSubjectPersistencePort accountSubjectPersistencePort) {
        this.budgetService = budgetService;
        this.budgetPersistencePort = budgetPersistencePort;
        this.departmentPersistencePort = departmentPersistencePort;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
    }

    @Override
    public void checkBudgetAvailability(String yearMonth, String departmentCode, String accountCode,
            BigDecimal amount) {
        Department department = departmentPersistencePort.findByCode(departmentCode)
                .orElseThrow(() -> new IllegalArgumentException("Department not found. code=" + departmentCode));
        AccountSubject accountSubject = accountSubjectPersistencePort.findByCode(accountCode)
                .orElseThrow(() -> new IllegalArgumentException("Account subject not found. code=" + accountCode));

        if (budgetPersistencePort
                .findByYearMonthAndDepartmentAndAccountSubject(yearMonth, department, accountSubject)
                .isEmpty()) {
            return;
        }
        budgetService.checkBudgetAvailability(yearMonth, department, accountSubject, amount);
    }
}
