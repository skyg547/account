package com.ho.account.common.adapter;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Department;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.DepartmentRepository;
import com.ho.account.contracts.expenditure.BudgetControlPort;
import com.ho.account.expenditure.repository.BudgetRepository;
import com.ho.account.expenditure.service.BudgetService;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class BudgetControlAdapter implements BudgetControlPort {

    private final BudgetService budgetService;
    private final DepartmentRepository departmentRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final BudgetRepository budgetRepository;

    public BudgetControlAdapter(
            BudgetService budgetService,
            DepartmentRepository departmentRepository,
            AccountSubjectRepository accountSubjectRepository,
            BudgetRepository budgetRepository) {
        this.budgetService = budgetService;
        this.departmentRepository = departmentRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.budgetRepository = budgetRepository;
    }

    @Override
    public void checkBudgetAvailability(String yearMonth, String departmentCode, String accountCode, BigDecimal amount) {
        Department department = departmentRepository.findByCode(departmentCode)
                .orElseThrow(() -> new IllegalArgumentException("부서를 찾을 수 없습니다. code=" + departmentCode));
        AccountSubject accountSubject = accountSubjectRepository.findByCode(accountCode)
                .orElseThrow(() -> new IllegalArgumentException("계정과목을 찾을 수 없습니다. code=" + accountCode));
        if (budgetRepository.findByYearMonthAndDepartmentAndAccountSubject(yearMonth, department, accountSubject).isEmpty()) {
            return;
        }
        budgetService.checkBudgetAvailability(yearMonth, department, accountSubject, amount);
    }
}
