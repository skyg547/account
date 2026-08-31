package com.ho.account.internalaudit.core.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.internalaudit.core.application.port.out.EvaluationPersistencePort;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationPersistencePort persistencePort;

    private EvaluationService service;

    @BeforeEach
    void setUp() {
        service = new EvaluationService(persistencePort);
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
    }

    @Test
    void deficiencyIdentifiersAreRequiredBeforeLookup() {
        Deficiency command = new Deficiency("deficiency-a", " ", "Gap", "Remediate", "OPEN");

        assertThatThrownBy(() -> service.registerDeficiency(command))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("evaluationId");
        verify(persistencePort, never()).evaluationExists(any());
        verify(persistencePort, never()).saveDeficiency(any());
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
    }
}
