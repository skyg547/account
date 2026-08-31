package com.ho.account.expenditure.application.service;

import com.ho.account.expenditure.application.port.out.BudgetPersistencePort;
import com.ho.account.expenditure.domain.Budget;
import java.math.BigDecimal;
import java.util.Optional;
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

    public Budget assignBudget(String yearMonth, String departmentCode, String accountCode, BigDecimal assignedAmount) {
        Budget budget = budgetPersistencePort
                .findByYearMonthAndDepartmentCodeAndAccountCode(yearMonth, departmentCode, accountCode)
                .orElseGet(() -> {
                    Budget b = new Budget();
                    b.setYearMonth(yearMonth);
                    b.setDeptCode(departmentCode);
                    b.setAccountCode(accountCode);
                    b.setAssignedAmount(BigDecimal.ZERO);
                    b.setUsedAmount(BigDecimal.ZERO);
                    return b;
                });
        budget.setAssignedAmount(assignedAmount != null ? assignedAmount : BigDecimal.ZERO);
        return budgetPersistencePort.save(budget);
    }

    public void useBudget(String yearMonth, String departmentCode, String accountCode, BigDecimal amount) {
        Optional<Budget> optionalBudget = budgetPersistencePort
                .findByYearMonthAndDepartmentCodeAndAccountCode(yearMonth, departmentCode, accountCode);
        if (optionalBudget.isEmpty()) {
            return;
        }
        Budget budget = optionalBudget.get();
        budget.useBudget(amount);
        budgetPersistencePort.save(budget);
    }

    /**
     * 지정된 연월, 부서, 계정과목의 차감된 예산을 복원(환원)합니다.
     * 
     * 🎓 [교육적 설명 / Financial Control LifeCycle]
     * 결의서의 수정(updateResolution)이나 반려(rejectResolution)가 발생하면, 기존에 차감 처리되었던
     * 예산을 즉시 복원해야 합니다. 
     * 이를 통해 회계 시스템은 '실제 유효한 결의'에 대해서만 예산을 차감(Deduction) 상태로 유지하며,
     * 취소되거나 변경된 거래 내역에 의한 예산 과다 잠금(Over-locking) 현상을 방지하여 
     * 자금 집행의 정확성과 통제 정합성(Consistency)을 보장합니다.
     *
     * @param yearMonth 연월 (YYYYMM)
     * @param departmentCode 부서 코드
     * @param accountCode 계정과목 코드
     * @param amount 복원할 예산 금액
     */
    public void restoreBudget(String yearMonth, String departmentCode, String accountCode, BigDecimal amount) {
        Optional<Budget> optionalBudget = budgetPersistencePort
                .findByYearMonthAndDepartmentCodeAndAccountCode(yearMonth, departmentCode, accountCode);
        if (optionalBudget.isEmpty()) {
            return;
        }
        Budget budget = optionalBudget.get();
        budget.restoreBudget(amount);
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
        Optional<Budget> optionalBudget = budgetPersistencePort
                .findByYearMonthAndDepartmentCodeAndAccountCode(yearMonth, departmentCode, accountCode);
        if (optionalBudget.isEmpty()) {
            return;
        }
        Budget budget = optionalBudget.get();
        if (budget.getRemainingAmount().compareTo(amount) < 0) {
            throw new IllegalStateException(
                    String.format("예산이 초과됩니다. [연월: %s, 부서: %s, 계정과목: %s, 요청금액: %s, 잔여예산: %s]",
                            yearMonth, departmentCode, accountCode, amount, budget.getRemainingAmount()));
        }
    }
}