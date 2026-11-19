package com.ho.account.auth.api.web;

import com.ho.account.auth.core.application.exception.GovernanceException;
import com.ho.account.auth.core.application.port.in.GovernanceUseCase;
import com.ho.account.shared.infrastructure.security.application.port.in.AuthorizationUseCase.*;
import com.ho.account.shared.infrastructure.security.application.port.in.MasterApprovalUseCase.RequestApprovalCommand;
import com.ho.account.shared.infrastructure.security.domain.AccessType;
import com.ho.account.shared.infrastructure.security.domain.MasterApproval;
import com.ho.account.shared.infrastructure.security.web.dto.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Narrow Auth-owned ingress; legacy body/header actor hints never become authority. */
@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class GovernanceController {
    private final GovernanceUseCase governance;

    @PostMapping("/approvals/requests")
    public MasterApprovalResponse request(HttpServletRequest http, @RequestBody ApprovalRequest body) {
        return MasterApprovalResponse.from(governance.request(credential(http), new RequestApprovalCommand(
                body.masterType(), body.masterKey(), body.requestType(), body.payload(), null,
                body.effectiveDate(), body.requestedVersion())));
    }

    @GetMapping("/approvals/pending")
    public List<MasterApprovalResponse> pending(HttpServletRequest http) {
        return governance.pending(credential(http)).stream().map(MasterApprovalResponse::from).toList();
    }

    @PostMapping("/approvals/{id}/approve")
    public MasterApprovalResponse approve(HttpServletRequest http, @PathVariable Long id, @RequestBody Decision body) {
        return MasterApprovalResponse.from(governance.decide(credential(http), id, true, body.remarks()));
    }

    @PostMapping("/approvals/{id}/reject")
    public MasterApprovalResponse reject(HttpServletRequest http, @PathVariable Long id, @RequestBody Decision body) {
        return MasterApprovalResponse.from(governance.decide(credential(http), id, false, body.remarks()));
    }

    @GetMapping("/roles")
    public List<SystemRoleResponse> roles(HttpServletRequest http) {
        return governance.roles(credential(http)).stream().map(SystemRoleResponse::from).toList();
    }

    @GetMapping("/roles/{roleCode}")
    public SystemRoleResponse role(HttpServletRequest http, @PathVariable String roleCode) {
        return SystemRoleResponse.from(governance.role(credential(http), roleCode));
    }

    @PostMapping("/roles")
    public ResponseEntity<MasterApprovalResponse> createRole(HttpServletRequest http, @Valid @RequestBody CreateRoleRequest body) {
        return ResponseEntity.accepted().body(MasterApprovalResponse.from(governance.createRole(credential(http),
                new CreateRoleCommand(body.roleCode(), body.roleName(), body.description()))));
    }

    @GetMapping("/roles/{roleCode}/authorizations")
    public List<AuthorizationResponse> authorizations(HttpServletRequest http, @PathVariable String roleCode) {
        return governance.authorizations(credential(http), roleCode).stream().map(AuthorizationResponse::from).toList();
    }

    @PostMapping("/roles/{roleCode}/authorizations")
    public ResponseEntity<MasterApprovalResponse> grant(HttpServletRequest http, @PathVariable String roleCode,
            @Valid @RequestBody GrantAuthorizationRequest body) {
        return ResponseEntity.accepted().body(MasterApprovalResponse.from(governance.grant(credential(http),
                new GrantAuthorizationCommand(roleCode, body.functionCode(), body.accessType(), body.dataScope()))));
    }

    @DeleteMapping("/authorizations/{id}")
    public ResponseEntity<MasterApprovalResponse> revoke(HttpServletRequest http, @PathVariable Long id) {
        return ResponseEntity.accepted().body(MasterApprovalResponse.from(governance.revoke(credential(http), id)));
    }

    @GetMapping("/permissions/check")
    public boolean permission(HttpServletRequest http, @RequestParam String roleCode,
            @RequestParam String functionCode, @RequestParam AccessType accessType) {
        return governance.permission(credential(http), roleCode, functionCode, accessType);
    }

    @GetMapping("/logs")
    public List<AuditLogResponse> logs(HttpServletRequest http,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return governance.logs(credential(http), "date", null, null, start, end).stream().map(AuditLogResponse::from).toList();
    }

    @GetMapping("/logs/user/{userId}")
    public List<AuditLogResponse> userLogs(HttpServletRequest http, @PathVariable String userId) {
        return governance.logs(credential(http), "user", userId, null, null, null).stream().map(AuditLogResponse::from).toList();
    }

    @GetMapping("/logs/type/{eventType}")
    public List<AuditLogResponse> typeLogs(HttpServletRequest http, @PathVariable String eventType) {
        return governance.logs(credential(http), "type", eventType, null, null, null).stream().map(AuditLogResponse::from).toList();
    }

    @GetMapping("/logs/target")
    public List<AuditLogResponse> targetLogs(HttpServletRequest http, @RequestParam String entity, @RequestParam String targetId) {
        return governance.logs(credential(http), "target", entity, targetId, null, null).stream().map(AuditLogResponse::from).toList();
    }

    private String credential(HttpServletRequest request) {
        List<String> values = Collections.list(request.getHeaders("Authorization"));
        if (values.size() != 1 || values.get(0).contains(","))
            throw new GovernanceException(GovernanceException.Reason.UNAUTHENTICATED);
        return values.get(0);
    }

    public record ApprovalRequest(String masterType, String masterKey, MasterApproval.ChangeRequestType requestType,
            String payload, String requestUser, LocalDate effectiveDate, Integer requestedVersion) {}
    public record Decision(String approverUser, String remarks) {}
}
