package com.ho.account.internalaudit.core.application.service;

import com.ho.account.internalaudit.core.application.port.in.RcmUseCase;
import com.ho.account.internalaudit.core.application.port.out.RcmPersistencePort;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import java.util.List;
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
        return persistencePort.saveProcess(command);
    }

    @Override
    public RcmRisk addRisk(String processId, RcmRisk command) {
        // In a real scenario, we might want to check if the process exists first
        return persistencePort.saveRisk(command);
    }

    @Override
    public ControlActivity addControl(String riskId, ControlActivity command) {
        // Similar check for risk existence
        return persistencePort.saveControlActivity(command);
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
}
