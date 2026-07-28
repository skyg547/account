package com.ho.account.masterdata.core.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MonolithFiscalPeriodControlAdapterTest {

    @Test
    void statusUpdateLocksRowAndUsesAuditedDomainTransition() {
        FiscalPeriodPersistencePort persistencePort = mock(FiscalPeriodPersistencePort.class);
        FiscalPeriod period = period();
        when(persistencePort.findByIdForUpdate(7L)).thenReturn(Optional.of(period));
        when(persistencePort.save(any(FiscalPeriod.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        MonolithFiscalPeriodControlAdapter adapter =
                new MonolithFiscalPeriodControlAdapter(persistencePort);

        FiscalPeriodRef result = adapter.updateClosingStatus(7L, " closed ", "  closer  ");

        assertThat(result.closingStatus()).isEqualTo("CLOSED");
        assertThat(period.getAuditUser()).isEqualTo("closer");
        verify(persistencePort).findByIdForUpdate(7L);
        verify(persistencePort).save(period);
    }

    private FiscalPeriod period() {
        FiscalPeriod period = new FiscalPeriod();
        period.setId(7L);
        period.setFiscalYear("2026");
        period.setFiscalPeriod("07");
        period.setStartDate(LocalDate.of(2026, 7, 1));
        period.setEndDate(LocalDate.of(2026, 7, 31));
        return period;
    }
}
