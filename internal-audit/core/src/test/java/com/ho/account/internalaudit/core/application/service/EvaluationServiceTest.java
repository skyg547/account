package com.ho.account.internalaudit.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.CommandReceiptPort;
import com.ho.account.internalaudit.core.domain.CommandReceipt;
import com.ho.account.internalaudit.core.domain.IdempotencyConflictException;
import com.ho.account.internalaudit.core.application.port.out.EvaluationPersistencePort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationPersistencePort persistencePort;

    @Mock
    private AuditLogPersistencePort auditLogPersistencePort;

    @Mock
    private CommandReceiptPort receiptPort;

    private final Map<String, CommandReceipt> receipts = new HashMap<>();
    private final Map<String, AuditLogEntry> storedAudits = new HashMap<>();
    private EvaluationService service;

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
        service = new EvaluationService(persistencePort,
                new IdempotentCommandExecutor(receiptPort, auditLogPersistencePort, new ObjectMapper()));
    }

    @AfterEach
    void tearDown() {
        AuditActorContext.clear();
    }

    @Test
    void designEvaluationRequiresExistingControl() {
        when(persistencePort.controlActivityExists("missing-control")).thenReturn(false);
        DesignEvaluation command = new DesignEvaluation(
                "design-a", "missing-control", "evaluator", "2026-08-11", "EFFECTIVE", null);

        assertThatThrownBy(() -> service.submitDesignEvaluation(command))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("control activity");
        verify(persistencePort, never()).saveDesignEvaluation(any());
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void submitDesignEvaluationProducesAppendOnlyAuditRecord() {
        when(persistencePort.controlActivityExists("ctrl-1")).thenReturn(true);
        DesignEvaluation command = new DesignEvaluation(
                "design-1", "ctrl-1", "auditor_kim", "2026-08-11", "EFFECTIVE", "Adequate control");
        when(persistencePort.saveDesignEvaluation(any())).thenAnswer(inv -> inv.getArgument(0));

        AuditActorContext.setCorrelationId("corr-eval-1");
        AuditActorContext.setIdempotencyKey("idem-eval-1");

        DesignEvaluation result = service.submitDesignEvaluation(command);

        assertThat(result).isNotNull();
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogPersistencePort).append(captor.capture());

        AuditLogEntry entry = captor.getValue();
        assertThat(entry.actor()).isEqualTo("auditor_kim");
        assertThat(entry.action()).isEqualTo("SUBMIT_DESIGN_EVALUATION");
        assertThat(entry.aggregateType()).isEqualTo("DESIGN_EVALUATION");
        assertThat(entry.aggregateId()).isEqualTo("design-1");
        assertThat(entry.correlationId()).isEqualTo("corr-eval-1");
        assertThat(entry.idempotencyKey()).isEqualTo("idem-eval-1");
        assertThat(entry.actionTimestamp()).isNotNull();
        assertThat(entry.detailsJson()).contains("design-1").contains("EFFECTIVE");
    }

    @Test
    void submitOperatingEvaluationProducesAppendOnlyAuditRecord() {
        when(persistencePort.controlActivityExists("ctrl-1")).thenReturn(true);
        OperatingEvaluation command = new OperatingEvaluation(
                "op-1", "ctrl-1", "auditor_lee", "2026-08-11", 25, 0, List.of("/path/file.pdf"), "EFFECTIVE", "Passed");
        when(persistencePort.saveOperatingEvaluation(any())).thenAnswer(inv -> inv.getArgument(0));

        OperatingEvaluation result = service.submitOperatingEvaluation(command);

        assertThat(result).isNotNull();
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogPersistencePort).append(captor.capture());

        AuditLogEntry entry = captor.getValue();
        assertThat(entry.actor()).isEqualTo("auditor_lee");
        assertThat(entry.action()).isEqualTo("SUBMIT_OPERATING_EVALUATION");
        assertThat(entry.aggregateType()).isEqualTo("OPERATING_EVALUATION");
        assertThat(entry.aggregateId()).isEqualTo("op-1");
        assertThat(entry.actionTimestamp()).isNotNull();
        assertThat(entry.detailsJson()).contains("op-1").contains("25");
    }

    @Test
    void registerDeficiencyProducesAppendOnlyAuditRecordWithContextActor() {
        when(persistencePort.evaluationExists("design-1")).thenReturn(true);
        Deficiency command = new Deficiency("def-1", "design-1", "Sample gap", "Improve logs", "IDENTIFIED");
        when(persistencePort.saveDeficiency(any())).thenAnswer(inv -> inv.getArgument(0));

        AuditActorContext.setActor("auditor_park");
        AuditActorContext.setCorrelationId("corr-def-1");
        AuditActorContext.setIdempotencyKey("idem-def-1");

        Deficiency result = service.registerDeficiency(command);

        assertThat(result).isNotNull();
        ArgumentCaptor<AuditLogEntry> captor = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogPersistencePort).append(captor.capture());

        AuditLogEntry entry = captor.getValue();
        assertThat(entry.actor()).isEqualTo("auditor_park");
        assertThat(entry.action()).isEqualTo("REGISTER_DEFICIENCY");
        assertThat(entry.aggregateType()).isEqualTo("DEFICIENCY");
        assertThat(entry.aggregateId()).isEqualTo("def-1");
        assertThat(entry.correlationId()).isEqualTo("corr-def-1");
        assertThat(entry.idempotencyKey()).isEqualTo("idem-def-1");
        assertThat(entry.detailsJson()).contains("Sample gap");
    }

    @Test
    void operatingEvaluationRequiresExistingControl() {
        when(persistencePort.controlActivityExists("missing-control")).thenReturn(false);
        OperatingEvaluation command = new OperatingEvaluation(
                "operating-a",
                "missing-control",
                "evaluator",
                "2026-08-11",
                10,
                0,
                List.of(),
                "EFFECTIVE",
                null);

        assertThatThrownBy(() -> service.submitOperatingEvaluation(command))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("control activity");
        verify(persistencePort, never()).saveOperatingEvaluation(any());
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void deficiencyRequiresExistingDesignOrOperatingEvaluation() {
        when(persistencePort.evaluationExists("missing-evaluation")).thenReturn(false);
        Deficiency command = new Deficiency(
                "deficiency-a", "missing-evaluation", "Gap", "Remediate", "OPEN");

        assertThatThrownBy(() -> service.registerDeficiency(command))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("evaluation");
        verify(persistencePort, never()).saveDeficiency(any());
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void deficiencyIdentifiersAreRequiredBeforeLookup() {
        Deficiency command = new Deficiency("deficiency-a", " ", "Gap", "Remediate", "OPEN");

        assertThatThrownBy(() -> service.registerDeficiency(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evaluationId");
        verify(persistencePort, never()).evaluationExists(any());
        verify(persistencePort, never()).saveDeficiency(any());
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void designEvaluationRejectsInvalidResultFailClosed() {
        assertThatThrownBy(() -> new DesignEvaluation(
                "design-a", "ctrl-1", "evaluator", "2026-08-11", "INVALID_RESULT", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EFFECTIVE or INEFFECTIVE");
    }

    @Test
    void designEvaluationRejectsNullResultFailClosed() {
        DesignEvaluation command = new DesignEvaluation(
                "design-a", "ctrl-1", "evaluator", "2026-08-11", null, null);

        assertThatThrownBy(() -> service.submitDesignEvaluation(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evaluation result is required");
        verify(persistencePort, never()).saveDesignEvaluation(any());
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void operatingEvaluationRejectsInvalidResultFailClosed() {
        assertThatThrownBy(() -> new OperatingEvaluation(
                "operating-a", "ctrl-1", "evaluator", "2026-08-11", 10, 0, List.of(), "MAYBE", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("EFFECTIVE or INEFFECTIVE");
    }

    @Test
    void operatingEvaluationRejectsNullResultFailClosed() {
        OperatingEvaluation command = new OperatingEvaluation(
                "operating-a", "ctrl-1", "evaluator", "2026-08-11", 10, 0, List.of(), null, null);

        assertThatThrownBy(() -> service.submitOperatingEvaluation(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Evaluation result is required");
        verify(persistencePort, never()).saveOperatingEvaluation(any());
        verify(auditLogPersistencePort, never()).append(any());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "-1,-2", "-1,0", "0,-1", "-1,NULL", "NULL,-1", "10,11", "0,1"
    }, nullValues = "NULL")
    void invalidCountsCannotConstructAServiceCommandOrReachEitherPersistencePort(
            Integer sampleSize, Integer exceptionCount) {
        // Constructor validation prevents the invalid command from ever entering the service.
        assertThatThrownBy(() -> service.submitOperatingEvaluation(new OperatingEvaluation(
                "op-invalid", "ctrl-1", "auditor", "2026-09-11", sampleSize, exceptionCount,
                List.of(), "EFFECTIVE", null)))
                .isInstanceOf(IllegalArgumentException.class);

        verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
    }

    @ParameterizedTest
    @CsvSource(value = {
            "25,2", "10,10", "10,0", "0,0", "NULL,NULL", "0,NULL", "NULL,0",
            "10,NULL", "NULL,10", "2147483647,2147483647"
    }, nullValues = "NULL")
    void acceptedCountsAreSavedUnchangedAndAudited(Integer sampleSize, Integer exceptionCount) throws Exception {
        when(persistencePort.controlActivityExists("ctrl-1")).thenReturn(true);
        when(persistencePort.saveOperatingEvaluation(any())).thenAnswer(inv -> inv.getArgument(0));
        OperatingEvaluation command = new OperatingEvaluation(
                "op-boundary", "ctrl-1", " auditor ", "2026-09-11", sampleSize, exceptionCount,
                List.of(), " effective ", null);

        OperatingEvaluation saved = service.submitOperatingEvaluation(command);

        assertThat(saved).isEqualTo(command);
        verify(persistencePort).saveOperatingEvaluation(command);
        ArgumentCaptor<AuditLogEntry> audit = ArgumentCaptor.forClass(AuditLogEntry.class);
        verify(auditLogPersistencePort).append(audit.capture());
        assertThat(audit.getValue().actor()).isEqualTo("auditor");
        OperatingEvaluation audited = new ObjectMapper()
                .readValue(audit.getValue().detailsJson(), OperatingEvaluation.class);
        assertThat(audited).isEqualTo(command);
        assertThat(saved.result()).isEqualTo("EFFECTIVE");
    }


    @Test
    void designReplayNormalizesResultAndPreservesFirstResponseWithoutParentLookup() {
        AuditActorContext.setActor("trusted");
        AuditActorContext.setIdempotencyKey("design-key");
        when(persistencePort.controlActivityExists("c")).thenReturn(true);
        DesignEvaluation command = new DesignEvaluation("d", "c", "trusted", "2026-09-12", " effective ", null);
        DesignEvaluation saved = new DesignEvaluation("d", "c", "trusted", "2026-09-12", "EFFECTIVE", "enriched");
        when(persistencePort.saveDesignEvaluation(command)).thenReturn(saved);
        assertThat(service.submitDesignEvaluation(command)).isEqualTo(saved);
        clearInvocations(persistencePort, auditLogPersistencePort);
        AuditActorContext.setCorrelationId("later-trace");

        assertThat(service.submitDesignEvaluation(new DesignEvaluation(
                "d", "c", "trusted", "2026-09-12", "EFFECTIVE", null))).isEqualTo(saved);
        verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
        AuditActorContext.setActor("different");
        assertThatThrownBy(() -> service.submitDesignEvaluation(command))
                .isInstanceOf(IdempotencyConflictException.class);
        verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void operatingReplayPreservesNullCountsEvidenceOrderAndFirstResponse() {
        AuditActorContext.setIdempotencyKey("operating-key");
        when(persistencePort.controlActivityExists("c")).thenReturn(true);
        OperatingEvaluation command = new OperatingEvaluation("o", "c", "actor", "2026-09-12",
                null, 0, List.of("b", "a"), "effective", null);
        when(persistencePort.saveOperatingEvaluation(command)).thenReturn(command);
        assertThat(service.submitOperatingEvaluation(command)).isEqualTo(command);
        clearInvocations(persistencePort, auditLogPersistencePort);

        assertThat(service.submitOperatingEvaluation(command)).isEqualTo(command);
        verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
        assertThatThrownBy(() -> service.submitOperatingEvaluation(new OperatingEvaluation(
                "o", "c", "actor", "2026-09-12", 0, 0, List.of("b", "a"), "effective", null)))
                .isInstanceOf(IdempotencyConflictException.class);
        assertThatThrownBy(() -> service.submitOperatingEvaluation(new OperatingEvaluation(
                "o", "c", "actor", "2026-09-12", null, 0, List.of("a", "b"), "effective", null)))
                .isInstanceOf(IdempotencyConflictException.class);
        verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void deficiencyReplayReturnsFirstSavedResultAndSkipsMutableEvaluationLookup() {
        AuditActorContext.setIdempotencyKey("deficiency-key");
        when(persistencePort.evaluationExists("evaluation")).thenReturn(true);
        Deficiency command = new Deficiency("def", "evaluation", "description", null, "IDENTIFIED");
        Deficiency saved = new Deficiency("def", "evaluation", "description", "saved plan", "IDENTIFIED");
        when(persistencePort.saveDeficiency(command)).thenReturn(saved);
        assertThat(service.registerDeficiency(command)).isEqualTo(saved);
        clearInvocations(persistencePort, auditLogPersistencePort);

        assertThat(service.registerDeficiency(command)).isEqualTo(saved);
        verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
        assertThatThrownBy(() -> service.registerDeficiency(new Deficiency(
                "def", "other-evaluation", "description", null, "IDENTIFIED")))
                .isInstanceOf(IdempotencyConflictException.class);
        verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
    }

    @Test
    void keyedReplayStillRejectsMissingResultBeforeReceiptLookup() {
        AuditActorContext.setIdempotencyKey("existing-key");
        assertThatThrownBy(() -> service.submitDesignEvaluation(
                new DesignEvaluation("d", "c", "actor", "2026-09-12", null, null)))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(receiptPort, persistencePort, auditLogPersistencePort);
    }

    @Test
    void legacyConvenienceConstructorCannotExecuteKeyedCommand() {
        EvaluationService legacy = new EvaluationService(persistencePort, auditLogPersistencePort);
        AuditActorContext.setIdempotencyKey("key");
        assertThatThrownBy(() -> legacy.registerDeficiency(new Deficiency("d", "e", null, null, null)))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(persistencePort);
        verify(auditLogPersistencePort, never()).append(any());
    }
}
