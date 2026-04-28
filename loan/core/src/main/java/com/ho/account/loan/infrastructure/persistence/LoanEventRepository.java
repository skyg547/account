package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * LoanEvent ?뷀떚?곕? ?꾪븳 Spring Data JPA Repository
 */
@Repository
public interface LoanEventRepository extends JpaRepository<LoanEvent, Long> {
    List<LoanEvent> findByLoanOrderByEventDateAsc(Loan loan);
}
