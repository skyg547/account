package com.ho.account.internalaudit.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.EvaluationPersistencePort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationPersistencePort persistencePort;

    @Mock
    private AuditLogPersistencePort auditLogPersistencePort;

    private EvaluationService service;

    @BeforeEach
    void setUp() {
        service = new EvaluationService(persistencePort, auditLogPersistencePort);
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
}
