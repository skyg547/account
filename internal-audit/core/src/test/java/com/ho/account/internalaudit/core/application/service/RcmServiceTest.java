package com.ho.account.internalaudit.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.RcmPersistencePort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RcmServiceTest {

    @Mock
    private RcmPersistencePort persistencePort;

    @Mock
    private AuditLogPersistencePort auditLogPersistencePort;

    private RcmService service;

    @BeforeEach
    void setUp() {
        service = new RcmService(persistencePort, auditLogPersistencePort);
    }

    @AfterEach
    void tearDown() {
        AuditActorContext.clear();
    }

    @Test
    void createProcessProducesAppendOnlyAuditRecord() {
        RcmProcess command = new RcmProcess("proc-1", "Financial Reporting", "Controls over financial closing", "auditor_kim");
        when(persistencePort.saveProcess(any())).thenAnswer(inv -> inv.getArgument(0));

        AuditActorContext.setCorrelationId("corr-proc-1");
        AuditActorContext.setIdempotencyKey("idem-proc-1");

        RcmProcess result = service.createProcess(command);

        assertThat(result).isNotNull();
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogPersistencePort).append(captor.capture());

        AuditLogEntry entry = captor.getValue();
        assertThat(entry.actor()).isEqualTo("auditor_kim");
        assertThat(entry.action()).isEqualTo("CREATE_PROCESS");
        assertThat(entry.aggregateType()).isEqualTo("RCM_PROCESS");
        assertThat(entry.aggregateId()).isEqualTo("proc-1");
        assertThat(entry.correlationId()).isEqualTo("corr-proc-1");
        assertThat(entry.idempotencyKey()).isEqualTo("idem-proc-1");
        assertThat(entry.actionTimestamp()).isNotNull();
        assertThat(entry.detailsJson()).contains("Financial Reporting");
    }

    @Test
    void addRiskProducesAppendOnlyAuditRecord() {
        when(persistencePort.findProcessById("proc-1"))
                .thenReturn(Optional.of(new RcmProcess("proc-1", "Process", null, "owner")));
        RcmRisk command = new RcmRisk("risk-1", "proc-1", "Risk of material misstatement", "HIGH", "LIKELY");
        when(persistencePort.saveRisk(any())).thenAnswer(inv -> inv.getArgument(0));

        AuditActorContext.setActor("auditor_park");
        AuditActorContext.setCorrelationId("corr-risk-1");
        AuditActorContext.setIdempotencyKey("idem-risk-1");

        RcmRisk result = service.addRisk("proc-1", command);

        assertThat(result).isNotNull();
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogPersistencePort).append(captor.capture());

        AuditLogEntry entry = captor.getValue();
        assertThat(entry.actor()).isEqualTo("auditor_park");
        assertThat(entry.action()).isEqualTo("ADD_RISK");
        assertThat(entry.aggregateType()).isEqualTo("RCM_RISK");
        assertThat(entry.aggregateId()).isEqualTo("risk-1");
        assertThat(entry.correlationId()).isEqualTo("corr-risk-1");
        assertThat(entry.idempotencyKey()).isEqualTo("idem-risk-1");
        assertThat(entry.detailsJson()).contains("material misstatement");
    }

    @Test
    void addControlProducesAppendOnlyAuditRecord() {
        when(persistencePort.findRiskById("risk-1"))
                .thenReturn(Optional.of(new RcmRisk("risk-1", "proc-1", "Risk", "HIGH", "LIKELY")));
        ControlActivity command = new ControlActivity(
                "ctrl-1", "risk-1", "Quarterly balance review", "PREVENTIVE", "MANUAL", "QUARTERLY", "auditor_choi");
        when(persistencePort.saveControlActivity(any())).thenAnswer(inv -> inv.getArgument(0));

        ControlActivity result = service.addControl("risk-1", command);

        assertThat(result).isNotNull();
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogPersistencePort).append(captor.capture());

        AuditLogEntry entry = captor.getValue();
        assertThat(entry.actor()).isEqualTo("auditor_choi");
        assertThat(entry.action()).isEqualTo("ADD_CONTROL");
        assertThat(entry.aggregateType()).isEqualTo("CONTROL_ACTIVITY");
        assertThat(entry.aggregateId()).isEqualTo("ctrl-1");
        assertThat(entry.detailsJson()).contains("Quarterly balance review");
    }

    @Test
    void pathProcessIdIsAuthoritativeWhenRiskBodyOmitsIt() {
        when(persistencePort.findProcessById("process-a"))
                .thenReturn(Optional.of(new RcmProcess("process-a", "Process", null, null)));
        RcmRisk command = new RcmRisk("risk-a", null, "Risk", "HIGH", "LIKELY");
        when(persistencePort.saveRisk(any())).thenAnswer(invocation -> invocation.getArgument(0));

        RcmRisk saved = service.addRisk("process-a", command);

        assertThat(saved.processId()).isEqualTo("process-a");
        ArgumentCaptor<RcmRisk> captor = ArgumentCaptor.forClass(RcmRisk.class);
        verify(persistencePort).saveRisk(captor.capture());
        assertThat(captor.getValue().processId()).isEqualTo("process-a");
    }

    @Test
    void mismatchedRiskParentIsRejectedBeforePersistence() {
        RcmRisk command = new RcmRisk("risk-a", "process-b", "Risk", "HIGH", "LIKELY");

        assertThatThrownBy(() -> service.addRisk("process-a", command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("processId");
        verify(persistencePort, never()).saveRisk(any());
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void missingRiskParentIsRejectedBeforePersistence() {
        when(persistencePort.findProcessById("missing")).thenReturn(Optional.empty());
        RcmRisk command = new RcmRisk("risk-a", null, "Risk", "HIGH", "LIKELY");

        assertThatThrownBy(() -> service.addRisk("missing", command))
                .isInstanceOf(NoSuchElementException.class);
        verify(persistencePort, never()).saveRisk(any());
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void pathRiskIdIsAuthoritativeWhenControlBodyOmitsIt() {
        when(persistencePort.findRiskById("risk-a"))
                .thenReturn(Optional.of(new RcmRisk("risk-a", "process-a", null, null, null)));
        ControlActivity command = new ControlActivity(
                "control-a", null, "Control", "PREVENTIVE", "MANUAL", "DAILY", "owner");
        when(persistencePort.saveControlActivity(any()))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ControlActivity saved = service.addControl("risk-a", command);

        assertThat(saved.riskId()).isEqualTo("risk-a");
    }

    @Test
    void mismatchedControlParentIsRejectedBeforePersistence() {
        ControlActivity command = new ControlActivity(
                "control-a", "risk-b", "Control", "PREVENTIVE", "MANUAL", "DAILY", "owner");

        assertThatThrownBy(() -> service.addControl("risk-a", command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("riskId");
        verify(persistencePort, never()).saveControlActivity(any());
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void identifiersAreRequiredBeforePersistence() {
        RcmRisk command = new RcmRisk(" ", null, "Risk", "HIGH", "LIKELY");

        assertThatThrownBy(() -> service.addRisk("process-a", command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("riskId");
        verify(persistencePort, never()).saveRisk(any());
        verify(auditLogPersistencePort, never()).append(any());
    }
}
