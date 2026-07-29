package com.ho.account.internalaudit.core.application.port.in;

import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import java.util.List;

public interface RcmUseCase {
    RcmProcess createProcess(RcmProcess command);
    RcmRisk addRisk(String processId, RcmRisk command);
    ControlActivity addControl(String riskId, ControlActivity command);

    List<RcmProcess> getAllProcesses();
    List<RcmRisk> getRisksByProcess(String processId);
    List<ControlActivity> getControlsByRisk(String riskId);
}
