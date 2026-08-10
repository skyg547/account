package com.ho.account.internalaudit.core.application.service;

import com.ho.account.internalaudit.core.application.port.in.EvaluationUseCase;
import com.ho.account.internalaudit.core.application.port.out.EvaluationPersistencePort;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.List;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class EvaluationService implements EvaluationUseCase {

    private final EvaluationPersistencePort persistencePort;

    @Override
    public DesignEvaluation submitDesignEvaluation(DesignEvaluation command) {
        requireIdentifier(command.evaluationId(), "evaluationId");
        requireIdentifier(command.controlId(), "controlId");
        requireControl(command.controlId());
        return persistencePort.saveDesignEvaluation(command);
    }

    @Override
    public OperatingEvaluation submitOperatingEvaluation(OperatingEvaluation command) {
        requireIdentifier(command.evaluationId(), "evaluationId");
        requireIdentifier(command.controlId(), "controlId");
        requireControl(command.controlId());
        return persistencePort.saveOperatingEvaluation(command);
    }

    @Override
    public Deficiency registerDeficiency(Deficiency command) {
        requireIdentifier(command.deficiencyId(), "deficiencyId");
        requireIdentifier(command.evaluationId(), "evaluationId");
        if (!persistencePort.evaluationExists(command.evaluationId())) {
            throw new NoSuchElementException("Design or operating evaluation was not found");
        }
        return persistencePort.saveDeficiency(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DesignEvaluation> getDesignEvaluationsByControl(String controlId) {
        return persistencePort.findDesignEvaluationsByControlId(controlId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OperatingEvaluation> getOperatingEvaluationsByControl(String controlId) {
        return persistencePort.findOperatingEvaluationsByControlId(controlId);
    }

    private void requireControl(String controlId) {
        if (!persistencePort.controlActivityExists(controlId)) {
            throw new NoSuchElementException("RCM control activity was not found");
        }
    }

    private void requireIdentifier(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
