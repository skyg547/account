package com.ho.account.audit.web;

import com.ho.account.audit.domain.*;
import com.ho.account.audit.service.AuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/audit")
public class AuditController {

    private final AuditService auditService;

    @Autowired
    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    // ===== 감사 로그 =====

    @GetMapping("/logs")
    public ResponseEntity<List<AuditLog>> getLogsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end) {
        return ResponseEntity.ok(auditService.getLogsByDateRange(start, end));
    }

    @GetMapping("/logs/user/{userId}")
    public ResponseEntity<List<AuditLog>> getLogsByUser(@PathVariable String userId) {
        return ResponseEntity.ok(auditService.getLogsByUser(userId));
    }

    @GetMapping("/logs/target")
    public ResponseEntity<List<AuditLog>> getLogsByTarget(
            @RequestParam String entity, @RequestParam String targetId) {
        return ResponseEntity.ok(auditService.getLogsByTarget(entity, targetId));
    }

    @GetMapping("/logs/type/{eventType}")
    public ResponseEntity<List<AuditLog>> getLogsByEventType(@PathVariable String eventType) {
        return ResponseEntity.ok(auditService.getLogsByEventType(eventType));
    }

    // ===== 역할 관리 =====

    @GetMapping("/roles")
    public ResponseEntity<List<SystemRole>> getAllRoles() {
        return ResponseEntity.ok(auditService.getAllRoles());
    }

    @GetMapping("/roles/{roleCode}")
    public ResponseEntity<SystemRole> getRoleByCode(@PathVariable String roleCode) {
        return ResponseEntity.ok(auditService.getRoleByCode(roleCode));
    }

    @PostMapping("/roles")
    public ResponseEntity<SystemRole> createRole(@RequestBody Map<String, String> body) {
        SystemRole role = auditService.createRole(
                body.get("roleCode"), body.get("roleName"), body.get("description"));
        return ResponseEntity.ok(role);
    }

    // ===== 권한 관리 =====

    @GetMapping("/roles/{roleCode}/authorizations")
    public ResponseEntity<List<Authorization>> getAuthorizationsByRole(@PathVariable String roleCode) {
        return ResponseEntity.ok(auditService.getAuthorizationsByRole(roleCode));
    }

    @PostMapping("/roles/{roleCode}/authorizations")
    public ResponseEntity<Authorization> grantAuthorization(
            @PathVariable String roleCode,
            @RequestBody Map<String, String> body) {
        Authorization auth = auditService.grantAuthorization(
                roleCode,
                body.get("functionCode"),
                AccessType.valueOf(body.get("accessType")),
                body.get("dataScope"));
        return ResponseEntity.ok(auth);
    }

    @DeleteMapping("/authorizations/{authorizationId}")
    public ResponseEntity<Void> revokeAuthorization(@PathVariable Long authorizationId) {
        auditService.revokeAuthorization(authorizationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/permissions/check")
    public ResponseEntity<Boolean> checkPermission(
            @RequestParam String roleCode,
            @RequestParam String functionCode,
            @RequestParam AccessType accessType) {
        return ResponseEntity.ok(auditService.hasPermission(roleCode, functionCode, accessType));
    }
}
