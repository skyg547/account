package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Loan ?”í‹°?°ë? ?„í•œ Spring Data JPA Repository
 */
@Repository
public interface LoanRepository extends JpaRepository<Loan, Long> {
    Optional<Loan> findByLoanNumber(String loanNumber);
}
