package com.ho.account.loan.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class LoanTest {

    @Test
    void createUsesPendingStateAndDecimalRateUnit() {
        Loan loan = pendingLoan();

        assertThat(loan.getStatus()).isEqualTo(Loan.LoanStatus.PENDING_DISBURSEMENT);
        assertThat(loan.getInitialEIR()).isEqualByComparingTo("0.1200");
        assertThat(loan.getCurrentEIR()).isEqualByComparingTo("0.1200");
        assertThat(loan.getOutstandingPrincipal()).isEqualByComparingTo("1000.00");
        assertThat(loan.getCurrencyCode()).isEqualTo("KRW");
    }

    @Test
    void fullDisbursalActivatesLoanExactlyOnce() {
        Loan loan = pendingLoan();
        loan.activateAfterDisbursal(
                LocalDate.of(2026, 1, 1), new BigDecimal("1000.00"), "loan-user");

        assertThat(loan.getStatus()).isEqualTo(Loan.LoanStatus.ACTIVE);
        assertThatThrownBy(() -> loan.activateAfterDisbursal(
                LocalDate.of(2026, 1, 1), new BigDecimal("1000.00"), "loan-user"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending");
    }

    @Test
    void partialDisbursalFailsBecauseTrancheBalanceIsNotModelled() {
        Loan loan = pendingLoan();

        assertThatThrownBy(() -> loan.activateAfterDisbursal(
                LocalDate.of(2026, 1, 1), new BigDecimal("500.00"), "loan-user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("one full disbursal");
    }

    @Test
    void recalculationChangesOutstandingBalanceWithoutErasingOriginalPrincipal() {
        Loan loan = activeLoan();
        loan.applyRecalculatedTerms(
                new BigDecimal("700.00"),
                LocalDate.of(2027, 6, 1),
                new BigDecimal("0.1100"),
                LocalDate.of(2026, 6, 1),
                "loan-user");

        assertThat(loan.getPrincipalAmount()).isEqualByComparingTo("1000.00");
        assertThat(loan.getOutstandingPrincipal()).isEqualByComparingTo("700.00");
        assertThat(loan.getCurrentEIR()).isEqualByComparingTo("0.1100");
    }

    @Test
    void recalculationRequiresMaturityAfterRecalculationDate() {
        Loan loan = activeLoan();
        LocalDate recalculationDate = LocalDate.of(2026, 6, 1);

        assertThatThrownBy(() -> loan.applyRecalculatedTerms(
                new BigDecimal("700.00"),
                recalculationDate,
                new BigDecimal("0.1100"),
                recalculationDate,
                "loan-user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be after");
    }

    @Test
    void createRejectsPercentStyleRateAndInvalidTerm() {
        assertThatThrownBy(() -> Loan.create(
                "LN-INVALID",
                10L,
                "KRW",
                Loan.LoanType.TERM_LOAN,
                new BigDecimal("1000.00"),
                new BigDecimal("12.0000"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2025, 12, 31),
                Loan.PaymentFrequency.MONTHLY,
                "loan-user"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("decimal rate");
    }

    @Test
    void scheduledPaymentRejectsForeignScheduleAndNonActiveLoan() {
        Loan loan = activeLoan();
        loan.setId(1L);
        EIRAmortizationSchedule schedule = repaymentSchedule(loan);
        Loan other = activeLoan();
        other.setId(2L);
        schedule.setLoan(other);
        assertThatThrownBy(() -> loan.applyScheduledRepayment(schedule, "SYSTEM"))
                .hasMessageContaining("belong");
        schedule.setLoan(loan);
        loan.markDefaulted("SYSTEM");
        assertThatThrownBy(() -> loan.applyScheduledRepayment(schedule, "SYSTEM"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void scheduledPaymentRejectsOverpaymentCurrencyFractionsAndDeferredAmortization() {
        Loan loan = activeLoan();
        loan.setId(1L);
        EIRAmortizationSchedule schedule = repaymentSchedule(loan);
        schedule.setPrincipalRepayment(new BigDecimal("1100"));
        schedule.setEndingBalance(new BigDecimal("-100"));
        schedule.setCashFlow(new BigDecimal("1110"));
        assertThatThrownBy(() -> loan.applyScheduledRepayment(schedule, "SYSTEM"))
                .hasMessageContaining("reconcile");
        schedule.setPrincipalRepayment(new BigDecimal("100.01"));
        schedule.setEndingBalance(new BigDecimal("899.99"));
        schedule.setCashFlow(new BigDecimal("110.01"));
        assertThatThrownBy(() -> loan.applyScheduledRepayment(schedule, "SYSTEM"))
                .hasMessageContaining("currency precision");
        EIRAmortizationSchedule deferred = repaymentSchedule(loan);
        deferred.setDeferredItemAmortization(BigDecimal.ONE);
        assertThatThrownBy(() -> loan.applyScheduledRepayment(deferred, "SYSTEM"))
                .hasMessageContaining("Deferred amortization");
        assertThat(loan.getOutstandingPrincipal()).isEqualByComparingTo("1000");
        assertThat(loan.getTotalPrincipalPaid()).isZero();
    }

    @Test
    void scheduledPaymentValidatesDateAndActorBeforeChangingBalance() {
        Loan loan = activeLoan();
        loan.setId(1L);
        EIRAmortizationSchedule schedule = repaymentSchedule(loan);
        schedule.setScheduleDate(loan.getMaturityDate().plusDays(1));
        assertThatThrownBy(() -> loan.applyScheduledRepayment(schedule, "SYSTEM"))
                .hasMessageContaining("contract period");
        schedule.setScheduleDate(loan.getDisbursalDate().plusMonths(1));
        assertThatThrownBy(() -> loan.applyScheduledRepayment(schedule, " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(loan.getOutstandingPrincipal()).isEqualByComparingTo("1000");
    }

    private EIRAmortizationSchedule repaymentSchedule(Loan loan) {
        EIRAmortizationSchedule schedule = new EIRAmortizationSchedule();
        schedule.setLoan(loan);
        schedule.setScheduleDate(loan.getDisbursalDate().plusMonths(1));
        schedule.setBeginningBalance(new BigDecimal("1000"));
        schedule.setPrincipalRepayment(new BigDecimal("100"));
        schedule.setInterestIncome(new BigDecimal("10"));
        schedule.setEndingBalance(new BigDecimal("900"));
        schedule.setCashFlow(new BigDecimal("110"));
        return schedule;
    }

    private Loan pendingLoan() {
        return Loan.create(
                "LN-DOMAIN-1",
                10L,
                "krw",
                Loan.LoanType.TERM_LOAN,
                new BigDecimal("1000.00"),
                new BigDecimal("0.1200"),
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 1, 1),
                Loan.PaymentFrequency.MONTHLY,
                "loan-user");
    }

    private Loan activeLoan() {
        Loan loan = pendingLoan();
        loan.activateAfterDisbursal(
                LocalDate.of(2026, 1, 1), new BigDecimal("1000.00"), "loan-user");
        return loan;
    }
}
