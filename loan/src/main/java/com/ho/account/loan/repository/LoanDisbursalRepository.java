package com.ho.account.loan.repository;

import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * LoanDisbursal 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface LoanDisbursalRepository extends JpaRepository<LoanDisbursal, Long> {
    List<LoanDisbursal> findByLoan(Loan loan);
}
