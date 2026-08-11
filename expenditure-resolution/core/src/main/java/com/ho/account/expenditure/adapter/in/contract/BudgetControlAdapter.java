package com.ho.account.expenditure.adapter.in.contract;

import com.ho.account.contracts.expenditure.BudgetControlPort;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.expenditure.application.port.out.BudgetPersistencePort;
import com.ho.account.expenditure.application.service.BudgetService;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BudgetControlAdapter implements BudgetControlPort {

    private final BudgetService budgetService;
    private final BudgetPersistencePort budgetPersistencePort;
    private final MasterDataQueryPort masterDataQueryPort;

    public BudgetControlAdapter(
            BudgetService budgetService,
            BudgetPersistencePort budgetPersistencePort,
            MasterDataQueryPort masterDataQueryPort) {
        this.budgetService = budgetService;
        this.budgetPersistencePort = budgetPersistencePort;
        this.masterDataQueryPort = masterDataQueryPort;
    }

    @Override
    public void checkBudgetAvailability(String yearMonth, String departmentCode, String accountCode, BigDecimal amount) {
        masterDataQueryPort.findDepartment(departmentCode)
                .orElseThrow(() -> new IllegalArgumentException("Department not found. code=" + departmentCode));
        masterDataQueryPort.findAccountSubject(accountCode)
                .orElseThrow(() -> new IllegalArgumentException("Account subject not found. code=" + accountCode));

        if (budgetPersistencePort
                .findByYearMonthAndDepartmentCodeAndAccountCode(yearMonth, departmentCode, accountCode)
                .isEmpty()) {
            return;
        }
        budgetService.checkBudgetAvailability(yearMonth, departmentCode, accountCode, amount);
    }

    @Override
    public void useBudget(String yearMonth, String departmentCode, String accountCode, BigDecimal amount) {
        budgetService.useBudget(yearMonth, departmentCode, accountCode, amount);
    }

    @Override
    public void restoreBudget(String yearMonth, String departmentCode, String accountCode, BigDecimal amount) {
        budgetService.restoreBudget(yearMonth, departmentCode, accountCode, amount);
    }
}