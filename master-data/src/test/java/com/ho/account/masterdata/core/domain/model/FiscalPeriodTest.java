package com.ho.account.masterdata.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class FiscalPeriodTest {

    @Test
    void closedPeriodCanReopenWithAuditedDomainTransition() {
        FiscalPeriod period = new FiscalPeriod();
        period.changeClosingStatus(FiscalPeriod.ClosingStatus.CLOSED, "closer");

        period.changeClosingStatus(FiscalPeriod.ClosingStatus.OPEN, "  approver  ");

        assertThat(period.getClosingStatus()).isEqualTo(FiscalPeriod.ClosingStatus.OPEN);
        assertThat(period.getAuditUser()).isEqualTo("approver");
    }

    @Test
    void permanentlyClosedPeriodCannotReopen() {
        FiscalPeriod period = new FiscalPeriod();
        period.changeClosingStatus(FiscalPeriod.ClosingStatus.CLOSED, "closer");
        period.changeClosingStatus(FiscalPeriod.ClosingStatus.PERMANENTLY_CLOSED, "controller");

        assertThatThrownBy(() -> period.changeClosingStatus(
                FiscalPeriod.ClosingStatus.OPEN, "approver"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Permanently closed");
    }

    @Test
    void openPeriodCannotSkipDirectlyToPermanentClosing() {
        FiscalPeriod period = new FiscalPeriod();

        assertThatThrownBy(() -> period.changeClosingStatus(
                FiscalPeriod.ClosingStatus.PERMANENTLY_CLOSED, "controller"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be closed");
    }
}
