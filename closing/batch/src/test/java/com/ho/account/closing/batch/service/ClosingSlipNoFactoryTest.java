package com.ho.account.closing.batch.service;

import static org.assertj.core.api.Assertions.assertThat;

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
    void eclProvisionSlipNoIsDeterministicAndFitsJournalColumn() {
        LocalDate closingDate = LocalDate.of(2026, 5, 22);

        String slipNo = ClosingSlipNoFactory.eclProvision(closingDate, "12001", 44L);

        assertThat(slipNo)
                .isEqualTo(ClosingSlipNoFactory.eclProvision(closingDate, "12001", 44L))
                .startsWith("ECL20260522")
                .hasSize(20);
    }
}
