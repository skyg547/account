package com.ho.account.loan.infrastructure.persistence;

import com.ho.account.loan.domain.LoanAmortizationScheduleEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanAmortizationScheduleEntryRepository extends JpaRepository<LoanAmortizationScheduleEntry, Long> {
    List<LoanAmortizationScheduleEntry> findByLoanContractIdOrderByPeriodNumberAsc(Long loanContractId);

    java.util.Optional<LoanAmortizationScheduleEntry> findByLoanContractIdAndPaymentDate(Long loanContractId,
            java.time.LocalDate paymentDate);
}
