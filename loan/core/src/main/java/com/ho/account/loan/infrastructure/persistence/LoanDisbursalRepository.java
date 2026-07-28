package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.LoanDisbursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoanDisbursalRepository extends JpaRepository<LoanDisbursal, Long> {
    boolean existsByLoanId(Long loanId);
}
