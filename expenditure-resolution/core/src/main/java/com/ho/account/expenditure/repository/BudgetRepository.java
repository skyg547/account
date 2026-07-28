package com.ho.account.expenditure.repository;

import com.ho.account.expenditure.domain.Budget;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
    Optional<Budget> findByYearMonthAndDeptCodeAndAccountCode(String yearMonth, String deptCode, String accountCode);
}