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
}
