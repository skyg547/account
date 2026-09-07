package com.ho.account.expenditure.payable.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PayableAccountingPeriodStatusAdapterTest {

    private final FiscalPeriodControlPort controlPort = mock(FiscalPeriodControlPort.class);
    private final PayableAccountingPeriodStatusAdapter adapter = new PayableAccountingPeriodStatusAdapter(controlPort);

    @Test
    void returnsTrueWhenPeriodIsClosedOrPermanentlyClosed() {
        LocalDate dateClosed = LocalDate.of(2026, 5, 15);
        when(controlPort.findFiscalPeriod("2026", "05"))
                .thenReturn(Optional.of(new FiscalPeriodRef(5L, "2026", "05",
                        LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31), "CLOSED")));

        assertThat(adapter.isClosed(dateClosed)).isTrue();

        LocalDate datePermClosed = LocalDate.of(2026, 4, 15);
        when(controlPort.findFiscalPeriod("2026", "04"))
                .thenReturn(Optional.of(new FiscalPeriodRef(4L, "2026", "04",
                        LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 30), "PERMANENTLY_CLOSED")));

        assertThat(adapter.isClosed(datePermClosed)).isTrue();
    }

    @Test
    void returnsFalseWhenPeriodIsOpen() {
        LocalDate dateOpen = LocalDate.of(2026, 6, 15);
        when(controlPort.findFiscalPeriod("2026", "06"))
                .thenReturn(Optional.of(new FiscalPeriodRef(6L, "2026", "06",
                        LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), "OPEN")));

        assertThat(adapter.isClosed(dateOpen)).isFalse();
    }

    @Test
    void throwsIllegalStateExceptionWhenFiscalPeriodMissing() {
        LocalDate missingDate = LocalDate.of(2026, 7, 15);
        when(controlPort.findFiscalPeriod("2026", "07")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.isClosed(missingDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fiscal period is missing for accounting date 2026-07-15");
    }

    @Test
    void rejectsNullAccountingDate() {
        assertThatThrownBy(() -> adapter.isClosed(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("accountingDate must not be null");
    }
}
