package com.accounting.system.expenditure.repository;

import com.accounting.system.basic.domain.AccountSubject;
import com.accounting.system.basic.domain.Department;
import com.accounting.system.expenditure.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByYearMonthAndDepartmentAndAccountSubject(String yearMonth, Department department, AccountSubject accountSubject);
}
