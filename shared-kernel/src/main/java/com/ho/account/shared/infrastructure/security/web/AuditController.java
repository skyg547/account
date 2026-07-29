package com.ho.account.shared.infrastructure.security.web;

import com.ho.account.shared.infrastructure.security.application.port.in.AuditLogUseCase;
import com.ho.account.shared.infrastructure.security.application.port.in.AuthorizationUseCase;
import com.ho.account.shared.infrastructure.security.application.port.in.MasterApprovalUseCase;
import com.ho.account.shared.infrastructure.security.domain.AccessType;
import com.ho.account.shared.infrastructure.security.domain.MasterApproval;
import com.ho.account.shared.infrastructure.security.web.dto.AuditLogResponse;
import com.ho.account.shared.infrastructure.security.web.dto.AuthorizationResponse;
import com.ho.account.shared.infrastructure.security.web.dto.CreateRoleRequest;
import com.ho.account.shared.infrastructure.security.web.dto.GrantAuthorizationRequest;
import com.ho.account.shared.infrastructure.security.web.dto.MasterApprovalDecisionRequest;
import com.ho.account.shared.infrastructure.security.web.dto.MasterApprovalRequest;
import com.ho.account.shared.infrastructure.security.web.dto.MasterApprovalResponse;
import com.ho.account.shared.infrastructure.security.web.dto.SystemRoleResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditLogUseCase auditLogUseCase;
    private final AuthorizationUseCase authorizationUseCase;
    private final MasterApprovalUseCase masterApprovalUseCase;

    // ===== 감사 로그 =====

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLogResponse>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(auditLogUseCase.getLogsByDateRange(start, end).stream()
                .map(AuditLogResponse::from)
                .toList());
    }

    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<List<AuditLogResponse>> getLogsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(auditLogUseCase.getLogsByUser(userId).stream()
                .map(AuditLogResponse::from)
                .toList());
    }

    @GetMapping("/logs/target")
    public ResponseEntity<List<AuditLogResponse>> getLogsByTarget(
            @RequestParam String entity, @RequestParam String targetId) {
        return ResponseEntity.ok(auditLogUseCase.getLogsByTarget(entity, targetId).stream()
                .map(AuditLogResponse::from)
                .toList());
    }

    @GetMapping("/logs/type/{eventType}")
    public ResponseEntity<List<AuditLogResponse>> getLogsByEventType(@PathVariable String eventType) {
        return ResponseEntity.ok(auditLogUseCase.getLogsByEventType(eventType).stream()
                .map(AuditLogResponse::from)
                .toList());
    }

    // ===== 역할 관리 =====

    @GetMapping("/roles")
    public ResponseEntity<List<SystemRoleResponse>> getAllRoles() {
        return ResponseEntity.ok(authorizationUseCase.getAllRoles().stream()
                .map(SystemRoleResponse::from)
                .toList());
    }

    @GetMapping("/roles/{roleCode}")
    public ResponseEntity<SystemRoleResponse> getRoleByCode(@PathVariable String roleCode) {
        return ResponseEntity.ok(SystemRoleResponse.from(authorizationUseCase.getRoleByCode(roleCode)));
    }

    @PostMapping("/roles")
    public ResponseEntity<SystemRoleResponse> createRole(@Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.ok(SystemRoleResponse.from(authorizationUseCase.createRole(
                new AuthorizationUseCase.CreateRoleCommand(
                        request.roleCode(),
                        request.roleName(),
                        request.description()))));
    }

    // ===== 권한 관리 =====

    @GetMapping("/roles/{roleCode}/authorizations")
    public ResponseEntity<List<AuthorizationResponse>> getAuthorizationsByRole(@PathVariable String roleCode) {
        return ResponseEntity.ok(authorizationUseCase.getAuthorizationsByRole(roleCode).stream()
                .map(AuthorizationResponse::from)
                .toList());
    }

    @PostMapping("/roles/{roleCode}/authorizations")
    public ResponseEntity<AuthorizationResponse> grantAuthorization(
            @PathVariable String roleCode,
            @Valid @RequestBody GrantAuthorizationRequest request) {
        return ResponseEntity.ok(AuthorizationResponse.from(authorizationUseCase.grantAuthorization(
                new AuthorizationUseCase.GrantAuthorizationCommand(
                        roleCode,
                        request.functionCode(),
                        request.accessType(),
                        request.dataScope()))));
    }

    @DeleteMapping("/authorizations/{authorizationId}")
    public ResponseEntity<MasterApprovalResponse> revokeAuthorization(@PathVariable Long authorizationId) {
        MasterApproval approval = authorizationUseCase.revokeAuthorization(authorizationId);
        return ResponseEntity.accepted().body(MasterApprovalResponse.from(approval));
    }

    @GetMapping("/permissions/check")
    public ResponseEntity<Boolean> checkPermission(
            @RequestParam String roleCode,
            @RequestParam String functionCode,
            @RequestParam AccessType accessType) {
        return ResponseEntity.ok(authorizationUseCase.hasPermission(roleCode, functionCode, accessType));
    }

    // ===== 마스터 승인 =====

    @GetMapping("/approvals/pending")
    public ResponseEntity<List<MasterApprovalResponse>> getPendingApprovals() {
        return ResponseEntity.ok(masterApprovalUseCase.getPendingRequests().stream()
                .map(MasterApprovalResponse::from)
                .toList());
    }

    @PostMapping("/approvals/requests")
    public ResponseEntity<MasterApprovalResponse> requestApproval(@Valid @RequestBody MasterApprovalRequest body) {
        MasterApproval approval = masterApprovalUseCase.requestApproval(new MasterApprovalUseCase.RequestApprovalCommand(
                body.masterType(),
                body.masterKey(),
                body.requestType(),
                body.payload(),
                body.requestUser(),
                body.effectiveDate(),
                body.requestedVersion()));
        return ResponseEntity.ok(MasterApprovalResponse.from(approval));
    }

    @PostMapping("/approvals/{approvalId}/approve")
    public ResponseEntity<MasterApprovalResponse> approve(
            @PathVariable Long approvalId,
            @Valid @RequestBody MasterApprovalDecisionRequest body) {
        MasterApproval approval = masterApprovalUseCase.approve(new MasterApprovalUseCase.ApproveCommand(
                approvalId,
                body.approverUser(),
                body.remarks()));
        return ResponseEntity.ok(MasterApprovalResponse.from(approval));
    }

    @PostMapping("/approvals/{approvalId}/reject")
    public ResponseEntity<MasterApprovalResponse> reject(
            @PathVariable Long approvalId,
            @Valid @RequestBody MasterApprovalDecisionRequest body) {
        MasterApproval approval = masterApprovalUseCase.reject(new MasterApprovalUseCase.RejectCommand(
                approvalId,
                body.approverUser(),
                body.remarks()));
        return ResponseEntity.ok(MasterApprovalResponse.from(approval));
    }
}
