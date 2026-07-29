package com.ho.account.internalaudit.core.application.port.out;

import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import java.util.List;
import java.util.Optional;

public interface RcmPersistencePort {
    RcmProcess saveProcess(RcmProcess process);
    RcmRisk saveRisk(RcmRisk risk);
    ControlActivity saveControlActivity(ControlActivity controlActivity);

    Optional<RcmProcess> findProcessById(String processId);
    Optional<RcmRisk> findRiskById(String riskId);
    Optional<ControlActivity> findControlActivityById(String controlId);

    List<RcmProcess> findAllProcesses();
    List<RcmRisk> findRisksByProcessId(String processId);
    List<ControlActivity> findControlActivitiesByRiskId(String riskId);
}
