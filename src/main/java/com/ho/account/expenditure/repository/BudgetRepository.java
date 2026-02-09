package com.ho.account.expenditure.repository;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.Department;
import com.ho.account.expenditure.domain.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByYearMonthAndDepartmentAndAccountSubject(String yearMonth, Department department, AccountSubject accountSubject);
}
