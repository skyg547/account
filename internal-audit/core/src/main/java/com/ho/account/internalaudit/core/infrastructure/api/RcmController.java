package com.ho.account.internalaudit.core.infrastructure.api;

import com.ho.account.internalaudit.core.application.port.in.RcmUseCase;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internalaudit/rcms")
@RequiredArgsConstructor
public class RcmController {

    private final RcmUseCase rcmUseCase;

    @PostMapping("/processes")
    public ResponseEntity<RcmProcess> createProcess(@RequestBody RcmProcess command) {
        return ResponseEntity.ok(rcmUseCase.createProcess(command));
    }

    @PostMapping("/processes/{processId}/risks")
    public ResponseEntity<RcmRisk> addRisk(@PathVariable String processId, @RequestBody RcmRisk command) {
        return ResponseEntity.ok(rcmUseCase.addRisk(processId, command));
    }

    @PostMapping("/risks/{riskId}/controls")
    public ResponseEntity<ControlActivity> addControl(@PathVariable String riskId, @RequestBody ControlActivity command) {
        return ResponseEntity.ok(rcmUseCase.addControl(riskId, command));
    }

    @GetMapping("/processes")
    public ResponseEntity<List<RcmProcess>> getAllProcesses() {
        return ResponseEntity.ok(rcmUseCase.getAllProcesses());
    }

    @GetMapping("/processes/{processId}/risks")
    public ResponseEntity<List<RcmRisk>> getRisksByProcess(@PathVariable String processId) {
        return ResponseEntity.ok(rcmUseCase.getRisksByProcess(processId));
    }

    @GetMapping("/risks/{riskId}/controls")
    public ResponseEntity<List<ControlActivity>> getControlsByRisk(@PathVariable String riskId) {
        return ResponseEntity.ok(rcmUseCase.getControlsByRisk(riskId));
    }
}
