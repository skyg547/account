package com.ho.account.closing.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ClosingSlipNoFactoryTest {

    @Test
    void fxValuationSlipNoIsDeterministicAndFitsJournalColumn() {
        LocalDate valuationDate = LocalDate.of(2026, 5, 22);

        String slipNo = ClosingSlipNoFactory.fxValuation(valuationDate, "USD-CASH", 123456789L);

        assertThat(slipNo)
                .isEqualTo(ClosingSlipNoFactory.fxValuation(valuationDate, "USD-CASH", 123456789L))
                .startsWith("FXV20260522")
                .hasSize(20);
    }

    @Test
    void fxValuationSlipNoUsesAccountDiscriminator() {
        LocalDate valuationDate = LocalDate.of(2026, 5, 22);

        String cashSlipNo = ClosingSlipNoFactory.fxValuation(valuationDate, "USD-CASH", 123456789L);
        String loanSlipNo = ClosingSlipNoFactory.fxValuation(valuationDate, "USD-LOAN", 123456789L);

        assertThat(cashSlipNo).isNotEqualTo(loanSlipNo);
    }

    @Test
    void fxValuationSlipNoUsesSourceCurrencyDiscriminator() {
        LocalDate valuationDate = LocalDate.of(2026, 5, 22);

        String eurSlipNo = ClosingSlipNoFactory.fxValuation(
                valuationDate, "11000|EUR", 123456789L);
        String usdSlipNo = ClosingSlipNoFactory.fxValuation(
                valuationDate, "11000|USD", 123456789L);

        assertThat(eurSlipNo).isNotEqualTo(usdSlipNo);
    }

    @Test
    void eclProvisionSlipNoIsDeterministicAndFitsJournalColumn() {
        LocalDate closingDate = LocalDate.of(2026, 5, 22);

        String slipNo = ClosingSlipNoFactory.eclProvision(closingDate, "12001", 44L);

        assertThat(slipNo)
                .isEqualTo(ClosingSlipNoFactory.eclProvision(closingDate, "12001", 44L))
                .startsWith("ECL20260522")
                .hasSize(20);
    }

    @Test
    void annualClosingSlipUsesRetainedEarningsAccount() {
        LocalDate date = LocalDate.of(2026, 12, 31);

        assertThat(ClosingSlipNoFactory.annualClosing(date, 2026, "35000"))
                .isNotEqualTo(ClosingSlipNoFactory.annualClosing(date, 2026, "35100"));
    }

    @Test
    void slipNumberRejectsMissingIdentity() {
        LocalDate date = LocalDate.of(2026, 5, 22);

        assertThatThrownBy(() -> ClosingSlipNoFactory.fxValuation(date, " ", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("discriminator");
        assertThatThrownBy(() -> ClosingSlipNoFactory.fxValuation(date, "11000|USD", 0L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("batchId");
    }
}
