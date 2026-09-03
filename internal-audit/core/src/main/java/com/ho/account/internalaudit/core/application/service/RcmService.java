package com.ho.account.internalaudit.core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.in.RcmUseCase;
import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.application.port.out.RcmPersistencePort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class RcmService implements RcmUseCase {

    private final RcmPersistencePort persistencePort;
    private final AuditLogPersistencePort auditLogPersistencePort;
    private final ObjectMapper objectMapper;

    public RcmService(RcmPersistencePort persistencePort) {
        this(persistencePort, null, new ObjectMapper());
    }

    public RcmService(RcmPersistencePort persistencePort,
                      AuditLogPersistencePort auditLogPersistencePort) {
        this(persistencePort, auditLogPersistencePort, new ObjectMapper());
    }

    @org.springframework.beans.factory.annotation.Autowired
    public RcmService(RcmPersistencePort persistencePort,
                      AuditLogPersistencePort auditLogPersistencePort,
                      @org.springframework.beans.factory.annotation.Autowired(required = false) ObjectMapper objectMapper) {
        this.persistencePort = persistencePort;
        this.auditLogPersistencePort = auditLogPersistencePort;
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
    }

    @Override
    public RcmProcess createProcess(RcmProcess command) {
        requireIdentifier(command.processId(), "processId");
        RcmProcess saved = persistencePort.saveProcess(command);
        appendAuditLog(
                resolveActor(command.ownerId()),
                "CREATE_PROCESS",
                "RCM_PROCESS",
                saved.processId(),
                saved);
        return saved;
    }

    @Override
    public RcmRisk addRisk(String processId, RcmRisk command) {
        requireIdentifier(processId, "processId");
        requireIdentifier(command.riskId(), "riskId");
        if (command.processId() != null && !Objects.equals(processId, command.processId())) {
            throw new IllegalArgumentException("processId in path and request body must match");
        }
        persistencePort.findProcessById(processId)
                .orElseThrow(() -> new NoSuchElementException("RCM process was not found"));
        RcmRisk saved = persistencePort.saveRisk(new RcmRisk(
                command.riskId(),
                processId,
                command.riskDescription(),
                command.impactLevel(),
                command.likelihood()));
        appendAuditLog(
                resolveActor(null),
                "ADD_RISK",
                "RCM_RISK",
                saved.riskId(),
                saved);
        return saved;
    }

    @Override
    public ControlActivity addControl(String riskId, ControlActivity command) {
        requireIdentifier(riskId, "riskId");
        requireIdentifier(command.controlId(), "controlId");
        if (command.riskId() != null && !Objects.equals(riskId, command.riskId())) {
            throw new IllegalArgumentException("riskId in path and request body must match");
        }
        persistencePort.findRiskById(riskId)
                .orElseThrow(() -> new NoSuchElementException("RCM risk was not found"));
        ControlActivity saved = persistencePort.saveControlActivity(new ControlActivity(
                command.controlId(),
                riskId,
                command.controlDescription(),
                command.controlType(),
                command.executionMethod(),
                command.frequency(),
                command.ownerId()));
        appendAuditLog(
                resolveActor(command.ownerId()),
                "ADD_CONTROL",
                "CONTROL_ACTIVITY",
                saved.controlId(),
                saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RcmProcess> getAllProcesses() {
        return persistencePort.findAllProcesses();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RcmRisk> getRisksByProcess(String processId) {
        return persistencePort.findRisksByProcessId(processId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ControlActivity> getControlsByRisk(String riskId) {
        return persistencePort.findControlActivitiesByRiskId(riskId);
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

    private void requireIdentifier(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
