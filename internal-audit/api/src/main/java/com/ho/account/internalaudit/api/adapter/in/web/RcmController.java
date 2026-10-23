package com.ho.account.internalaudit.api.adapter.in.web;

import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.in.RcmUseCase;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/internalaudit/rcms")
public class RcmController {

    public static final String AUTH_USER_HEADER = "X-Auth-User";
    public static final String AUTH_ROLES_HEADER = "X-Auth-Roles";
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String IDEMPOTENCY_KEY_HEADER = "X-Idempotency-Key";

    private final RcmUseCase rcmUseCase;

    public RcmController(RcmUseCase rcmUseCase) {
        this.rcmUseCase = rcmUseCase;
    }

    @PostMapping("/processes")
    public ResponseEntity<RcmProcess> createProcess(
            @RequestHeader(value = AUTH_USER_HEADER, required = false) String authUser,
            @RequestHeader(value = AUTH_ROLES_HEADER, required = false) String authRoles,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestBody RcmProcess command) {
        String actor = InternalAuditIdentityFilter.requireActor();
        try {
            AuditActorContext.setActor(actor);
            AuditActorContext.setCorrelationId(correlationId);
            AuditActorContext.setIdempotencyKey(idempotencyKey);
            RcmProcess toCreate = new RcmProcess(
                    command.processId(),
                    command.processName(),
                    command.description(),
                    command.ownerId() != null && !command.ownerId().isBlank() ? command.ownerId() : actor);
            return ResponseEntity.ok(rcmUseCase.createProcess(toCreate));
        } finally {
            AuditActorContext.clear();
        }
    }

    @PostMapping("/processes/{processId}/risks")
    public ResponseEntity<RcmRisk> addRisk(
            @PathVariable String processId,
            @RequestHeader(value = AUTH_USER_HEADER, required = false) String authUser,
            @RequestHeader(value = AUTH_ROLES_HEADER, required = false) String authRoles,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestBody RcmRisk command) {
        String actor = InternalAuditIdentityFilter.requireActor();
        try {
            AuditActorContext.setActor(actor);
            AuditActorContext.setCorrelationId(correlationId);
            AuditActorContext.setIdempotencyKey(idempotencyKey);
            return ResponseEntity.ok(rcmUseCase.addRisk(processId, command));
        } finally {
            AuditActorContext.clear();
        }
    }

    @PostMapping("/risks/{riskId}/controls")
    public ResponseEntity<ControlActivity> addControl(
            @PathVariable String riskId,
            @RequestHeader(value = AUTH_USER_HEADER, required = false) String authUser,
            @RequestHeader(value = AUTH_ROLES_HEADER, required = false) String authRoles,
            @RequestHeader(value = CORRELATION_ID_HEADER, required = false) String correlationId,
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey,
            @RequestBody ControlActivity command) {
        String actor = InternalAuditIdentityFilter.requireActor();
        try {
            AuditActorContext.setActor(actor);
            AuditActorContext.setCorrelationId(correlationId);
            AuditActorContext.setIdempotencyKey(idempotencyKey);
            ControlActivity toCreate = new ControlActivity(
                    command.controlId(),
                    command.riskId(),
                    command.controlDescription(),
                    command.controlType(),
                    command.executionMethod(),
                    command.frequency(),
                    command.ownerId() != null && !command.ownerId().isBlank() ? command.ownerId() : actor);
            return ResponseEntity.ok(rcmUseCase.addControl(riskId, toCreate));
        } finally {
            AuditActorContext.clear();
        }
    }

    @GetMapping("/processes")
    public ResponseEntity<List<RcmProcess>> getAllProcesses(
            @RequestHeader(value = AUTH_USER_HEADER, required = false) String authUser,
            @RequestHeader(value = AUTH_ROLES_HEADER, required = false) String authRoles) {
        InternalAuditIdentityFilter.requireActor();
        return ResponseEntity.ok(rcmUseCase.getAllProcesses());
    }

    @GetMapping("/processes/{processId}/risks")
    public ResponseEntity<List<RcmRisk>> getRisksByProcess(
            @PathVariable String processId,
            @RequestHeader(value = AUTH_USER_HEADER, required = false) String authUser,
            @RequestHeader(value = AUTH_ROLES_HEADER, required = false) String authRoles) {
        InternalAuditIdentityFilter.requireActor();
        return ResponseEntity.ok(rcmUseCase.getRisksByProcess(processId));
    }

    @GetMapping("/risks/{riskId}/controls")
    public ResponseEntity<List<ControlActivity>> getControlsByRisk(
            @PathVariable String riskId,
            @RequestHeader(value = AUTH_USER_HEADER, required = false) String authUser,
            @RequestHeader(value = AUTH_ROLES_HEADER, required = false) String authRoles) {
        InternalAuditIdentityFilter.requireActor();
        return ResponseEntity.ok(rcmUseCase.getControlsByRisk(riskId));
    }

}
