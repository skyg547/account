package com.ho.account.audit.web;

import com.ho.account.audit.application.port.in.AuditLogUseCase;
import com.ho.account.audit.application.port.in.AuthorizationUseCase;
import com.ho.account.audit.application.port.in.MasterApprovalUseCase;
import com.ho.account.audit.domain.AccessType;
import com.ho.account.audit.domain.AuditLog;
import com.ho.account.audit.domain.Authorization;
import com.ho.account.audit.domain.MasterApproval;
import com.ho.account.audit.domain.SystemRole;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
    public ResponseEntity<List<AuditLog>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(auditLogUseCase.getLogsByDateRange(start, end));
    }

    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<List<AuditLog>> getLogsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(auditLogUseCase.getLogsByUser(userId));
    }

    @GetMapping("/logs/target")
    public ResponseEntity<List<AuditLog>> getLogsByTarget(
            @RequestParam String entity, @RequestParam String targetId) {
        return ResponseEntity.ok(auditLogUseCase.getLogsByTarget(entity, targetId));
    }

    @GetMapping("/logs/type/{eventType}")
    public ResponseEntity<List<AuditLog>> getLogsByEventType(@PathVariable String eventType) {
        return ResponseEntity.ok(auditLogUseCase.getLogsByEventType(eventType));
    }

    // ===== 역할 관리 =====

    @GetMapping("/roles")
    public ResponseEntity<List<SystemRole>> getAllRoles() {
        return ResponseEntity.ok(authorizationUseCase.getAllRoles());
    }

    @GetMapping("/roles/{roleCode}")
    public ResponseEntity<SystemRole> getRoleByCode(@PathVariable String roleCode) {
        return ResponseEntity.ok(authorizationUseCase.getRoleByCode(roleCode));
    }

    @PostMapping("/roles")
    public ResponseEntity<SystemRole> createRole(@RequestBody Map<String, String> body) {
        SystemRole role = authorizationUseCase.createRole(new AuthorizationUseCase.CreateRoleCommand(
                body.get("roleCode"),
                body.get("roleName"),
                body.get("description")));
        return ResponseEntity.ok(role);
    }

    // ===== 권한 관리 =====

    @GetMapping("/roles/{roleCode}/authorizations")
    public ResponseEntity<List<Authorization>> getAuthorizationsByRole(@PathVariable String roleCode) {
        return ResponseEntity.ok(authorizationUseCase.getAuthorizationsByRole(roleCode));
    }

    @PostMapping("/roles/{roleCode}/authorizations")
    public ResponseEntity<Authorization> grantAuthorization(
            @PathVariable String roleCode,
            @RequestBody Map<String, String> body) {
        Authorization auth = authorizationUseCase.grantAuthorization(new AuthorizationUseCase.GrantAuthorizationCommand(
                roleCode,
                body.get("functionCode"),
                AccessType.valueOf(body.get("accessType")),
                body.get("dataScope")));
        return ResponseEntity.ok(auth);
    }

    @DeleteMapping("/authorizations/{authorizationId}")
    public ResponseEntity<Void> revokeAuthorization(@PathVariable Long authorizationId) {
        authorizationUseCase.revokeAuthorization(authorizationId);
        return ResponseEntity.noContent().build();
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
    public ResponseEntity<List<MasterApproval>> getPendingApprovals() {
        return ResponseEntity.ok(masterApprovalUseCase.getPendingRequests());
    }

    @PostMapping("/approvals/requests")
    public ResponseEntity<MasterApproval> requestApproval(@RequestBody MasterApprovalRequest body) {
        MasterApproval approval = masterApprovalUseCase.requestApproval(new MasterApprovalUseCase.RequestApprovalCommand(
                body.masterType(),
                body.masterKey(),
                body.requestType(),
                body.payload(),
                body.requestUser(),
                body.effectiveDate(),
                body.requestedVersion()));
        return ResponseEntity.ok(approval);
    }

    @PostMapping("/approvals/{approvalId}/approve")
    public ResponseEntity<MasterApproval> approve(
            @PathVariable Long approvalId,
            @RequestBody MasterApprovalDecision body) {
        MasterApproval approval = masterApprovalUseCase.approve(new MasterApprovalUseCase.ApproveCommand(
                approvalId,
                body.approverUser(),
                body.remarks()));
        return ResponseEntity.ok(approval);
    }

    @PostMapping("/approvals/{approvalId}/reject")
    public ResponseEntity<MasterApproval> reject(
            @PathVariable Long approvalId,
            @RequestBody MasterApprovalDecision body) {
        MasterApproval approval = masterApprovalUseCase.reject(new MasterApprovalUseCase.RejectCommand(
                approvalId,
                body.approverUser(),
                body.remarks()));
        return ResponseEntity.ok(approval);
    }

    public record MasterApprovalRequest(
            String masterType,
            String masterKey,
            MasterApproval.ChangeRequestType requestType,
            String payload,
            String requestUser,
            LocalDate effectiveDate,
            Integer requestedVersion) {
    }

    public record MasterApprovalDecision(
            String approverUser,
            String remarks) {
    }
}
