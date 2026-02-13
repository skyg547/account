package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.BankStatement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface BankStatementRepository extends JpaRepository<BankStatement, Long> {
    List<BankStatement> findByTransactionDateAndAccountNo(LocalDate transactionDate, String accountNo);

    List<BankStatement> findByReconciliationStatus(String reconciliationStatus);
}
