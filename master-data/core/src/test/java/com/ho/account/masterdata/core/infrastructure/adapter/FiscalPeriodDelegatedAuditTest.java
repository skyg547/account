package com.ho.account.masterdata.core.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort;
import com.ho.account.masterdata.core.application.port.out.MasterDataAuditPort;
import com.ho.account.masterdata.core.application.service.MasterDataActorContext;
import com.ho.account.masterdata.core.domain.audit.MasterDataAuditEvent;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class FiscalPeriodDelegatedAuditTest {
    @Test
    void recordsVerifiedServiceAndBusinessActorSeparately() {
        FiscalPeriodPersistencePort persistence = mock(FiscalPeriodPersistencePort.class);
        MasterDataAuditPort audit = mock(MasterDataAuditPort.class);
        FiscalPeriod period = FiscalPeriod.reconstitute(9L, "2026", "10",
                LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31),
                FiscalPeriod.ClosingStatus.OPEN, null, null, "prior-actor");
        when(persistence.findByIdForUpdate(9L)).thenReturn(Optional.of(period));
        when(persistence.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        try (MasterDataActorContext.Scope ignored = MasterDataActorContext.enter("closing", "business-924")) {
            new MonolithFiscalPeriodControlAdapter(persistence, audit)
                    .updateClosingStatus(9L, "CLOSED", "closing:business-924");
        }

        ArgumentCaptor<MasterDataAuditEvent> captured = ArgumentCaptor.forClass(MasterDataAuditEvent.class);
        verify(audit).append(captured.capture());
        assertThat(captured.getValue().serviceActor()).isEqualTo("closing");
        assertThat(captured.getValue().businessActor()).isEqualTo("business-924");
        assertThat(captured.getValue().reason()).isEqualTo("OPEN -> CLOSED");
        assertThat(captured.getValue().beforeVersionId()).isEqualTo(9L);
        assertThat(captured.getValue().afterVersionId()).isEqualTo(9L);
    }
}
