package com.ho.account.internalaudit.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.CommandReceiptPort;
import com.ho.account.internalaudit.core.domain.CommandReceipt;
import com.ho.account.internalaudit.core.domain.IdempotencyConflictException;
import com.ho.account.internalaudit.core.application.port.out.RcmPersistencePort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import java.util.NoSuchElementException;
import java.util.HashMap;
import java.util.Map;
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

    @Mock
    private CommandReceiptPort receiptPort;

    private final Map<String, CommandReceipt> receipts = new HashMap<>();
    private final Map<String, AuditLogEntry> storedAudits = new HashMap<>();
    private RcmService service;

    @BeforeEach
    void setUp() {
        // This fixture stores only receipts; business and audit writes remain observable mocks.
        lenient().when(receiptPort.findByKey(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(receipts.get(inv.getArgument(0))));
        lenient().doAnswer(inv -> {
            String key = inv.getArgument(0);
            receipts.put(key, new CommandReceipt(key, inv.getArgument(1), inv.getArgument(2), 0, null));
            return null;
        }).when(receiptPort).reserve(anyString(), anyInt(), anyString());
        lenient().doAnswer(inv -> {
            String key = inv.getArgument(0);
            CommandReceipt receipt = receipts.get(key);
            receipts.put(key, new CommandReceipt(key, receipt.fingerprintVersion(), receipt.fingerprint(),
                    inv.getArgument(1), inv.getArgument(2)));
            return null;
        }).when(receiptPort).complete(anyString(), anyInt(), anyString());
        lenient().when(auditLogPersistencePort.findByIdempotencyKey(anyString()))
                .thenAnswer(inv -> Optional.ofNullable(storedAudits.get(inv.getArgument(0))));
        lenient().when(auditLogPersistencePort.append(any())).thenAnswer(inv -> {
            AuditLogEntry entry = inv.getArgument(0);
            if (entry.idempotencyKey() != null) {
                storedAudits.put(entry.idempotencyKey(), entry);
            }
            return entry;
        });
        service = new RcmService(persistencePort,
                new IdempotentCommandExecutor(receiptPort, auditLogPersistencePort, new ObjectMapper()));
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

    @Test
    void processReplayReturnsFirstSavedResponseAfterLaterCommandWithoutWritingAgain() {
        AuditActorContext.setActor("trusted");
        AuditActorContext.setIdempotencyKey(" first ");
        RcmProcess command = new RcmProcess("p", "requested", null, "owner");
        RcmProcess firstSaved = new RcmProcess("p", "saved first", "enriched", "owner");
        RcmProcess later = new RcmProcess("p", "later", null, "owner");
        when(persistencePort.saveProcess(command)).thenReturn(firstSaved);
        when(persistencePort.saveProcess(later)).thenReturn(later);

        assertThat(service.createProcess(command)).isEqualTo(firstSaved);
        AuditActorContext.setIdempotencyKey("later");
        service.createProcess(later);
        AuditActorContext.setIdempotencyKey("first");
        AuditActorContext.setCorrelationId("retry-trace");

        assertThat(service.createProcess(command)).isEqualTo(firstSaved);
        verify(persistencePort, times(2)).saveProcess(any());
        verify(auditLogPersistencePort, times(2)).append(any());
        assertThat(receipts).hasSize(2);
    }

    @Test
    void riskReplayNormalizesOmittedParentAndSkipsMutableParentLookup() {
        AuditActorContext.setIdempotencyKey("risk-replay");
        when(persistencePort.findProcessById("p"))
                .thenReturn(Optional.of(new RcmProcess("p", "parent", null, null)));
        when(persistencePort.saveRisk(any())).thenAnswer(inv -> inv.getArgument(0));
        RcmRisk first = service.addRisk("p", new RcmRisk("r", null, "description", null, null));
        clearInvocations(persistencePort, auditLogPersistencePort);

        assertThat(service.addRisk("p", new RcmRisk("r", "p", "description", null, null)))
                .isEqualTo(first);
        org.mockito.Mockito.verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
        assertThatThrownBy(() -> service.addRisk("another-parent",
                new RcmRisk("r", null, "description", null, null)))
                .isInstanceOf(IdempotencyConflictException.class);
        org.mockito.Mockito.verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void controlReplayNormalizesOmittedParentAndSkipsMutableParentLookup() {
        AuditActorContext.setIdempotencyKey("control-replay");
        when(persistencePort.findRiskById("r"))
                .thenReturn(Optional.of(new RcmRisk("r", "p", null, null, null)));
        when(persistencePort.saveControlActivity(any())).thenAnswer(inv -> inv.getArgument(0));
        ControlActivity first = service.addControl("r",
                new ControlActivity("c", null, "description", "PREVENTIVE", "MANUAL", "DAILY", "owner"));
        clearInvocations(persistencePort, auditLogPersistencePort);

        assertThat(service.addControl("r",
                new ControlActivity("c", "r", "description", "PREVENTIVE", "MANUAL", "DAILY", "owner")))
                .isEqualTo(first);
        org.mockito.Mockito.verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
        assertThatThrownBy(() -> service.addControl("r",
                new ControlActivity("c", "r", "changed", "PREVENTIVE", "MANUAL", "DAILY", "owner")))
                .isInstanceOf(IdempotencyConflictException.class);
        org.mockito.Mockito.verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void keyedReplayStillRejectsInvalidPathBodyPairBeforeLookingUpReceipt() {
        AuditActorContext.setIdempotencyKey("existing-key");
        assertThatThrownBy(() -> service.addRisk("p", new RcmRisk("r", "other", null, null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        org.mockito.Mockito.verifyNoInteractions(receiptPort, persistencePort, auditLogPersistencePort);
    }

    @Test
    void legacyConvenienceConstructorCannotExecuteKeyedCommand() {
        RcmService legacy = new RcmService(persistencePort, auditLogPersistencePort);
        AuditActorContext.setIdempotencyKey("key");
        assertThatThrownBy(() -> legacy.createProcess(new RcmProcess("p", "name", null, null)))
                .isInstanceOf(IllegalStateException.class);
        org.mockito.Mockito.verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
    }
}
