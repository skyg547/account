package com.ho.account.loan.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoanTest {

    @Test
    void generateAmortizationScheduleHandlesZeroInterest() {
        Loan loan = baseLoan();
        loan.setInterestRate(BigDecimal.ZERO);

        List<LoanAmortizationScheduleEntry> schedule = loan.generateAmortizationSchedule(3);

        assertThat(schedule).hasSize(3);
        assertThat(schedule.get(0).getScheduledPaymentAmount()).isEqualByComparingTo("333.33");
        assertThat(schedule.get(1).getScheduledPaymentAmount()).isEqualByComparingTo("333.33");
        assertThat(schedule.get(2).getScheduledPaymentAmount()).isEqualByComparingTo("333.34");
        assertThat(schedule.get(2).getEndingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(schedule).allSatisfy(entry -> assertThat(entry.getLoan()).isSameAs(loan));
    }

    @Test
    void generateAmortizationScheduleRejectsInvalidPeriods() {
        Loan loan = baseLoan();

        assertThatThrownBy(() -> loan.generateAmortizationSchedule(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("totalPeriods");
    }

    private Loan baseLoan() {
        Loan loan = new Loan();
        loan.setLoanNumber("LN-DOMAIN-1");
        loan.setPrincipalAmount(new BigDecimal("1000.00"));
        loan.setInterestRate(new BigDecimal("0.1200"));
        loan.setDisbursalDate(LocalDate.of(2026, 1, 1));
        return loan;
    }
}
