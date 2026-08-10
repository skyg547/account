package com.ho.account.internalaudit.core.application.service;

import com.ho.account.internalaudit.core.application.port.in.RcmUseCase;
import com.ho.account.internalaudit.core.application.port.out.RcmPersistencePort;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RcmService implements RcmUseCase {

    private final RcmPersistencePort persistencePort;

    @Override
    public RcmProcess createProcess(RcmProcess command) {
        requireIdentifier(command.processId(), "processId");
        return persistencePort.saveProcess(command);
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
        return persistencePort.saveRisk(new RcmRisk(
                command.riskId(),
                processId,
                command.riskDescription(),
                command.impactLevel(),
                command.likelihood()));
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
        return persistencePort.saveControlActivity(new ControlActivity(
                command.controlId(),
                riskId,
                command.controlDescription(),
                command.controlType(),
                command.executionMethod(),
                command.frequency(),
                command.ownerId()));
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

    private void requireIdentifier(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
    }
}
