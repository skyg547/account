package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.Loan.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Loan ?뷀떚?곕? ?꾪븳 Spring Data JPA Repository
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findByLoanNumber(String loanNumber);
    List<Loan> findByStatus(LoanStatus status);
}
