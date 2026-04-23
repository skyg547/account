package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.RecalculationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * RecalculationRun ?”í‹°?°ë? ?„í•œ Spring Data JPA Repository
 */
@Repository
public interface RecalculationRunRepository extends JpaRepository<RecalculationRun, Long> {
    List<RecalculationRun> findByLoanOrderByRecalculationDateDesc(Loan loan);
}
