package com.ho.account.internalaudit.core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.in.EvaluationUseCase;
import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.EvaluationPersistencePort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EvaluationService implements EvaluationUseCase {

    private static final Set<String> VALID_RESULTS = Set.of("EFFECTIVE", "INEFFECTIVE");

    private final EvaluationPersistencePort persistencePort;
    private final AuditLogPersistencePort auditLogPersistencePort;
    private final ObjectMapper objectMapper;

    public EvaluationService(EvaluationPersistencePort persistencePort) {
        this(persistencePort, null, new ObjectMapper());
    }

    public EvaluationService(EvaluationPersistencePort persistencePort,
                             AuditLogPersistencePort auditLogPersistencePort) {
        this(persistencePort, auditLogPersistencePort, new ObjectMapper());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public EvaluationService(EvaluationPersistencePort persistencePort,
                             AuditLogPersistencePort auditLogPersistencePort,
                             @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.persistencePort = persistencePort;
        this.auditLogPersistencePort = auditLogPersistencePort;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public DesignEvaluation submitDesignEvaluation(DesignEvaluation command) {
        requireIdentifier(command.evaluationId(), "evaluationId");
        requireIdentifier(command.controlId(), "controlId");
        requireEvaluationResult(command.result());
        requireControl(command.controlId());
        DesignEvaluation saved = persistencePort.saveDesignEvaluation(command);
        appendAuditLog(
                resolveActor(command.evaluatorId()),
                "SUBMIT_DESIGN_EVALUATION",
                "DESIGN_EVALUATION",
                saved.evaluationId(),
                saved);
        return saved;
    }

    @Override
    public OperatingEvaluation submitOperatingEvaluation(OperatingEvaluation command) {
        requireIdentifier(command.evaluationId(), "evaluationId");
        requireIdentifier(command.controlId(), "controlId");
        requireEvaluationResult(command.result());
        requireControl(command.controlId());
        OperatingEvaluation saved = persistencePort.saveOperatingEvaluation(command);
        appendAuditLog(
                resolveActor(command.evaluatorId()),
                "SUBMIT_OPERATING_EVALUATION",
                "OPERATING_EVALUATION",
                saved.evaluationId(),
                saved);
        return saved;
    }

    @Override
    public Deficiency registerDeficiency(Deficiency command) {
        requireIdentifier(command.deficiencyId(), "deficiencyId");
        requireIdentifier(command.evaluationId(), "evaluationId");
        if (!persistencePort.evaluationExists(command.evaluationId())) {
            throw new NoSuchElementException("Design or operating evaluation was not found");
        }
        Deficiency saved = persistencePort.saveDeficiency(command);
        appendAuditLog(
                resolveActor(null),
                "REGISTER_DEFICIENCY",
                "DEFICIENCY",
                saved.deficiencyId(),
                saved);
        return saved;
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

    private void appendAuditLog(String actor, String action, String aggregateType, String aggregateId, Object payload) {
        if (auditLogPersistencePort == null) {
            return;
        }
        String detailsJson = null;
        if (payload != null) {
            try {
                detailsJson = objectMapper.writeValueAsString(payload);
            } catch (Exception e) {
                detailsJson = String.valueOf(payload);
            }
        }
        AuditLogEntry entry = AuditLogEntry.builder()
                .actor(actor)
                .action(action)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .actionTimestamp(LocalDateTime.now())
                .correlationId(AuditActorContext.getCorrelationId())
                .idempotencyKey(AuditActorContext.getIdempotencyKey())
                .detailsJson(detailsJson)
                .build();
        auditLogPersistencePort.append(entry);
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
