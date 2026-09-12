package com.ho.account.internalaudit.core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.in.EvaluationUseCase;
import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.EvaluationPersistencePort;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EvaluationService implements EvaluationUseCase {

    private static final Set<String> VALID_RESULTS = Set.of("EFFECTIVE", "INEFFECTIVE");

    private final EvaluationPersistencePort persistencePort;
    private final IdempotentCommandExecutor commandExecutor;

    public EvaluationService(EvaluationPersistencePort persistencePort) {
        this(persistencePort, null, new ObjectMapper());
    }

    public EvaluationService(EvaluationPersistencePort persistencePort,
                             AuditLogPersistencePort auditLogPersistencePort) {
        this(persistencePort, auditLogPersistencePort, new ObjectMapper());
    }

    public EvaluationService(EvaluationPersistencePort persistencePort,
                             AuditLogPersistencePort auditLogPersistencePort, ObjectMapper objectMapper) {
        this(persistencePort, IdempotentCommandExecutor.unkeyedOnly(auditLogPersistencePort, objectMapper));
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EvaluationService(EvaluationPersistencePort persistencePort, IdempotentCommandExecutor commandExecutor) {
        this.persistencePort = Objects.requireNonNull(persistencePort);
        this.commandExecutor = Objects.requireNonNull(commandExecutor);
    }

    @Override
    public DesignEvaluation submitDesignEvaluation(DesignEvaluation command) {
        requireIdentifier(command.evaluationId(), "evaluationId");
        requireIdentifier(command.controlId(), "controlId");
        requireEvaluationResult(command.result());
        return commandExecutor.execute(resolveActor(command.evaluatorId()), "SUBMIT_DESIGN_EVALUATION",
                "DESIGN_EVALUATION", command.evaluationId(), null, command, DesignEvaluation.class, () -> {
                    requireControl(command.controlId());
                    return persistencePort.saveDesignEvaluation(command);
                });
    }

    @Override
    public OperatingEvaluation submitOperatingEvaluation(OperatingEvaluation command) {
        requireIdentifier(command.evaluationId(), "evaluationId");
        requireIdentifier(command.controlId(), "controlId");
        requireEvaluationResult(command.result());
        return commandExecutor.execute(resolveActor(command.evaluatorId()), "SUBMIT_OPERATING_EVALUATION",
                "OPERATING_EVALUATION", command.evaluationId(), null, command, OperatingEvaluation.class, () -> {
                    requireControl(command.controlId());
                    return persistencePort.saveOperatingEvaluation(command);
                });
    }

    @Override
    public Deficiency registerDeficiency(Deficiency command) {
        requireIdentifier(command.deficiencyId(), "deficiencyId");
        requireIdentifier(command.evaluationId(), "evaluationId");
        return commandExecutor.execute(resolveActor(null), "REGISTER_DEFICIENCY", "DEFICIENCY",
                command.deficiencyId(), null, command, Deficiency.class, () -> {
                    if (!persistencePort.evaluationExists(command.evaluationId())) {
                        throw new NoSuchElementException("Design or operating evaluation was not found");
                    }
                    return persistencePort.saveDeficiency(command);
                });
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

    private String resolveActor(String fallbackActor) {
        String contextActor = AuditActorContext.getActor();
        if (contextActor != null && !contextActor.isBlank()) {
            return contextActor;
        }
        if (fallbackActor != null && !fallbackActor.isBlank()) {
            return fallbackActor.trim();
        }
        return "SYSTEM";
    }

    private void requireControl(String controlId) {
        if (!persistencePort.controlActivityExists(controlId)) {
            throw new NoSuchElementException("RCM control activity was not found");
        }
    }

    private void requireEvaluationResult(String result) {
        if (result == null || result.isBlank()) {
            throw new IllegalArgumentException("Evaluation result is required");
        }
        String normalized = result.trim().toUpperCase(Locale.ROOT);
        if (!VALID_RESULTS.contains(normalized)) {
            throw new IllegalArgumentException("Evaluation result must be EFFECTIVE or INEFFECTIVE: " + result);
        }
    }

    private void requireIdentifier(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
