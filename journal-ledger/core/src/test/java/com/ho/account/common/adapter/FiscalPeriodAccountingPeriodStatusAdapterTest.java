package com.ho.account.common.adapter;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FiscalPeriodAccountingPeriodStatusAdapterTest {

    private final FiscalPeriodControlPort fiscalPeriodControlPort = mock(FiscalPeriodControlPort.class);
    private final FiscalPeriodAccountingPeriodStatusAdapter adapter =
            new FiscalPeriodAccountingPeriodStatusAdapter(fiscalPeriodControlPort);

    @Test
    void missingFiscalPeriodFailsClosed() {
        LocalDate accountingDate = LocalDate.of(2026, 6, 15);
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "06")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.isClosed(accountingDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fiscal period is missing");
    }

    @Test
    void closedAndPermanentPeriodsAreLocked() {
        LocalDate accountingDate = LocalDate.of(2026, 6, 15);
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "06"))
                .thenReturn(Optional.of(period("CLOSED")));
        assertThat(adapter.isClosed(accountingDate)).isTrue();

        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "06"))
                .thenReturn(Optional.of(period("PERMANENTLY_CLOSED")));
        assertThat(adapter.isClosed(accountingDate)).isTrue();
    }

    @Test
    void openPeriodIsNotLocked() {
        LocalDate accountingDate = LocalDate.of(2026, 6, 15);
        when(fiscalPeriodControlPort.findFiscalPeriod("2026", "06"))
                .thenReturn(Optional.of(period("OPEN")));

        assertThat(adapter.isClosed(accountingDate)).isFalse();
    }

    private FiscalPeriodRef period(String status) {
        return new FiscalPeriodRef(
                6L,
                "2026",
                "06",
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
                status);
    }
}
