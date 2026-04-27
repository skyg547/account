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
                .orElseThrow(() -> new IllegalArgumentException("遺?쒕? 李얠쓣 ???놁뒿?덈떎. code=" + departmentCode));
        AccountSubject accountSubject = accountSubjectRepository.findByCode(accountCode)
                .orElseThrow(() -> new IllegalArgumentException("怨꾩젙怨쇰ぉ??李얠쓣 ???놁뒿?덈떎. code=" + accountCode));
        if (budgetRepository.findByYearMonthAndDepartmentAndAccountSubject(yearMonth, department, accountSubject).isEmpty()) {
            return;
        }
        budgetService.checkBudgetAvailability(yearMonth, department, accountSubject, amount);
    }
}
