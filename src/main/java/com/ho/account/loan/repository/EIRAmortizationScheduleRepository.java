package com.ho.account.loan.repository;

import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * EIRAmortizationSchedule 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface EIRAmortizationScheduleRepository extends JpaRepository<EIRAmortizationSchedule, Long> {
    List<EIRAmortizationSchedule> findByLoanOrderByScheduleDateAsc(Loan loan);
    Optional<EIRAmortizationSchedule> findFirstByLoanOrderByScheduleDateDesc(Loan loan);
    List<EIRAmortizationSchedule> findByLoanAndScheduleDateGreaterThanEqualOrderByScheduleDateAsc(Loan loan, LocalDate scheduleDate);
}
