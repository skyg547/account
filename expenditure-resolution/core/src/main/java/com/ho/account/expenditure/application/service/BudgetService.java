package com.ho.account.expenditure.application.service;

import com.ho.account.expenditure.application.port.out.BudgetPersistencePort;
import com.ho.account.expenditure.domain.Budget;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class BudgetService {

    private final BudgetPersistencePort budgetPersistencePort;

    public BudgetService(BudgetPersistencePort budgetPersistencePort) {
        this.budgetPersistencePort = budgetPersistencePort;
    }

    public Budget assignBudget(Budget budget) {
        return budgetPersistencePort.save(budget);
    }

    public void useBudget(String yearMonth, String departmentCode, String accountCode, BigDecimal amount) {
        Budget budget = budgetPersistencePort
                .findByYearMonthAndDepartmentCodeAndAccountCode(yearMonth, departmentCode, accountCode)
                .orElseThrow(() -> new IllegalArgumentException("해당 부서와 계정의 예산이 설정되지 않았습니다."));
        budget.useBudget(amount);
        budgetPersistencePort.save(budget);
    }

    @Transactional(readOnly = true)
    public BigDecimal getRemainingBudget(String yearMonth, String departmentCode, String accountCode) {
        return budgetPersistencePort
                .findByYearMonthAndDepartmentCodeAndAccountCode(yearMonth, departmentCode, accountCode)
                .map(Budget::getRemainingAmount)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly = true)
    public void checkBudgetAvailability(String yearMonth, String departmentCode, String accountCode, BigDecimal amount) {
        Budget budget = budgetPersistencePort
                .findByYearMonthAndDepartmentCodeAndAccountCode(yearMonth, departmentCode, accountCode)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("예산이 설정되지 않았습니다. [연월: %s, 부서: %s, 계정과목: %s]",
                                yearMonth, departmentCode, accountCode)));
        if (budget.getRemainingAmount().compareTo(amount) < 0) {
            throw new IllegalStateException(
                    String.format("예산이 초과됩니다. [연월: %s, 부서: %s, 계정과목: %s, 요청금액: %s, 잔여예산: %s]",
                            yearMonth, departmentCode, accountCode, amount, budget.getRemainingAmount()));
        }
    }
}