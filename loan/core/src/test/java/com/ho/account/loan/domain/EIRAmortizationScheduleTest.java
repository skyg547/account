package com.ho.account.loan.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        assertThat(schedules.get(0).getPrincipalRepayment()).isEqualByComparingTo("100");
        assertThat(schedules.get(0).getInterestIncome()).isEqualByComparingTo("12");
        assertThat(schedules.get(1).getInterestIncome()).isEqualByComparingTo("11");
        assertThat(schedules.stream()
                .map(EIRAmortizationSchedule::getPrincipalRepayment)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("1200");
        assertThat(schedules.stream()
                .map(EIRAmortizationSchedule::getDeferredItemAmortization)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("120.00");
    }

    @Test
    void rejectsEveryUnsupportedFrequencyBeforeContractCreation() {
        for (Loan.PaymentFrequency frequency : Loan.PaymentFrequency.values()) {
            if (frequency == Loan.PaymentFrequency.MONTHLY) {
                continue;
            }
            assertThatThrownBy(() -> Loan.create(
                    "LN-UNSUPPORTED", 100L, "KRW", Loan.LoanType.TERM_LOAN,
                    new BigDecimal("1200.00"), new BigDecimal("0.1200"),
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 4, 1),
                    frequency, "tester"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("paymentFrequency " + frequency)
                    .hasMessageContaining("only MONTHLY");
        }
    }

    @Test
    void rejectsExistingNonMonthlyContractsBeforeGeneratingMonthlyCashFlows() {
        Loan loan = activeLoan();
        for (Loan.PaymentFrequency frequency : Loan.PaymentFrequency.values()) {
            if (frequency == Loan.PaymentFrequency.MONTHLY) {
                continue;
            }
            // Simulate a contract loaded from existing storage, bypassing the new-contract guard.
            loan.setPaymentFrequency(frequency);
            assertThatThrownBy(() -> EIRAmortizationSchedule.generateMonthly(
                    loan, LocalDate.of(2026, 1, 1), new BigDecimal("0.1200"),
                    BigDecimal.ZERO, false, "tester"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("paymentFrequency " + frequency)
                    .hasMessageContaining("only MONTHLY");
        }
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
