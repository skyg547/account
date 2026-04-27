package com.ho.account.expenditure.service;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Department;
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

    // ?덉궛 ?몄꽦
    public Budget assignBudget(Budget budget) {
        return budgetRepository.save(budget);
    }

    // ?덉궛 ?ъ슜 (吏異?寃곗쓽 ???몄텧)
    public void useBudget(String yearMonth, Department department, AccountSubject accountSubject, BigDecimal amount) {
        Budget budget = budgetRepository.findByYearMonthAndDepartmentAndAccountSubject(yearMonth, department, accountSubject)
                .orElseThrow(() -> new IllegalArgumentException("?대떦 遺??怨꾩젙???덉궛???몄꽦?섏? ?딆븯?듬땲??"));
        
        budget.useBudget(amount);
        budgetRepository.save(budget);
    }

    // ?덉궛 ?붿븸 議고쉶
    @Transactional(readOnly = true)
    public BigDecimal getRemainingBudget(String yearMonth, Department department, AccountSubject accountSubject) {
        return budgetRepository.findByYearMonthAndDepartmentAndAccountSubject(yearMonth, department, accountSubject)
                .map(Budget::getRemainingAmount)
                .orElse(BigDecimal.ZERO);
    }

    // ?덉궛 ?ъ슜 媛???щ? ?뺤씤 (?꾪몴 ?좏슚??寃?????몄텧)
    @Transactional(readOnly = true)
    public void checkBudgetAvailability(String yearMonth, Department department, AccountSubject accountSubject, BigDecimal amount) {
        Budget budget = budgetRepository.findByYearMonthAndDepartmentAndAccountSubject(yearMonth, department, accountSubject)
                .orElseThrow(() -> new IllegalArgumentException(
                        String.format("?덉궛???몄꽦?섏? ?딆븯?듬땲?? [?곗썡: %s, 遺?? %s, 怨꾩젙怨쇰ぉ: %s]",
                                yearMonth, department.getName(), accountSubject.getName())));

        if (budget.getRemainingAmount().compareTo(amount) < 0) {
            throw new IllegalStateException(
                    String.format("?덉궛??遺議깊빀?덈떎. [?곗썡: %s, 遺?? %s, 怨꾩젙怨쇰ぉ: %s, ?붿껌 湲덉븸: %s, ?붿뿬 ?덉궛: %s]",
                            yearMonth, department.getName(), accountSubject.getName(), amount, budget.getRemainingAmount()));
        }
    }
}
