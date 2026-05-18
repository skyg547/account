package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.LoanAmortizationScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanAmortizationScheduleEntryRepository extends JpaRepository<LoanAmortizationScheduleEntry, Long> {
    List<LoanAmortizationScheduleEntry> findByLoanIdOrderByPeriodNumberAsc(Long loanId);

    java.util.Optional<LoanAmortizationScheduleEntry> findByLoanIdAndPaymentDate(Long loanId,
            java.time.LocalDate paymentDate);
}
