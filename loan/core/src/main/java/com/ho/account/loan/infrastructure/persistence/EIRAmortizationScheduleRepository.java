package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * EIRAmortizationSchedule ?뷀떚?곕? ?꾪븳 Spring Data JPA Repository
 */
@Repository
public interface EIRAmortizationScheduleRepository extends JpaRepository<EIRAmortizationSchedule, Long> {
    List<EIRAmortizationSchedule> findByLoan(Loan loan);
    List<EIRAmortizationSchedule> findByLoanOrderByScheduleDateAsc(Loan loan);
    List<EIRAmortizationSchedule> findByLoanAndScheduleDateGreaterThanEqualOrderByScheduleDateAsc(Loan loan, LocalDate scheduleDate);
    Optional<EIRAmortizationSchedule> findByLoanIdAndScheduleDate(Long loanId, LocalDate scheduleDate);
}
