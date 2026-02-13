package com.ho.account.loan.repository;

import com.ho.account.loan.domain.LoanAccrualLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface LoanAccrualLogRepository extends JpaRepository<LoanAccrualLog, Long> {
    Optional<LoanAccrualLog> findByLoanContractIdAndAccrualDate(Long loanContractId, LocalDate accrualDate);
}
