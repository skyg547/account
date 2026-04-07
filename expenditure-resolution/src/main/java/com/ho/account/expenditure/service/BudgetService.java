package com.ho.account.expenditure.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Department;
import com.ho.account.expenditure.domain.Budget;
import com.ho.account.expenditure.repository.BudgetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@Transactional
public class BudgetService {

    private final BudgetRepository budgetRepository;

    @Autowired
    public BudgetService(BudgetRepository budgetRepository) {
        this.budgetRepository = budgetRepository;
    }

    // 예산 편성
    public Budget assignBudget(Budget budget) {
        return budgetRepository.save(budget);
    }

    // 예산 사용 (지출 결의 시 호출)
    public void useBudget(String yearMonth, Department department, AccountSubject accountSubject, BigDecimal amount) {
        Budget budget = budgetRepository.findByYearMonthAndDepartmentAndAccountSubject(yearMonth, department, accountSubject)
                .orElseThrow(() -> new IllegalArgumentException("해당 부서/계정의 예산이 편성되지 않았습니다."));
        
        budget.useBudget(amount);
        budgetRepository.save(budget);
    }

    // 예산 잔액 조회
    @Transactional(readOnly = true)
    public BigDecimal getRemainingBudget(String yearMonth, Department department, AccountSubject accountSubject) {
        return budgetRepository.findByYearMonthAndDepartmentAndAccountSubject(yearMonth, department, accountSubject)
                .map(Budget::getRemainingAmount)
                .orElse(BigDecimal.ZERO);
    }

    // 예산 사용 가능 여부 확인 (전표 유효성 검사 시 호출)
    @Transactional(readOnly = true)
    public void checkBudgetAvailability(String yearMonth, Department department, AccountSubject accountSubject, BigDecimal amount) {
        Budget budget = budgetRepository.findByYearMonthAndDepartmentAndAccountSubject(yearMonth, department, accountSubject)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("예산이 편성되지 않았습니다. [연월: %s, 부서: %s, 계정과목: %s]",
                                yearMonth, department.getName(), accountSubject.getName())));

        if (budget.getRemainingAmount().compareTo(amount) < 0) {
            throw new IllegalStateException(
                    String.format("예산이 부족합니다. [연월: %s, 부서: %s, 계정과목: %s, 요청 금액: %s, 잔여 예산: %s]",
                            yearMonth, department.getName(), accountSubject.getName(), amount, budget.getRemainingAmount()));
        }
    }
}
