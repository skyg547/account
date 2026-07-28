package com.ho.account.loan.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class EIRAmortizationScheduleTest {

    @Test
    void monthlyScheduleStartsAfterDisbursalAndEndsAtMaturityWithoutResiduals() {
        Loan loan = activeLoan();

        List<EIRAmortizationSchedule> schedules = EIRAmortizationSchedule.generateMonthly(
                loan,
                LocalDate.of(2026, 1, 1),
                new BigDecimal("0.1200"),
                new BigDecimal("120.00"),
                false,
                "tester");

        assertThat(schedules).hasSize(12);
        assertThat(schedules.get(0).getScheduleDate()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(schedules.get(11).getScheduleDate()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(schedules.get(11).getEndingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(schedules.stream()
                .map(EIRAmortizationSchedule::getDeferredItemAmortization)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("120.00");
    }

    private Loan activeLoan() {
        Loan loan = Loan.create(
                "LN-SCHEDULE-1",
                100L,
                "KRW",
                Loan.LoanType.TERM_LOAN,
                new BigDecimal("1200.00"),
                new BigDecimal("0.1200"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                Loan.PaymentFrequency.MONTHLY,
                "tester");
        loan.activateAfterDisbursal(
                LocalDate.of(2026, 1, 1), new BigDecimal("1200.00"), "tester");
        return loan;
    }
}
