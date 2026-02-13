package com.ho.account.audit.service;

import com.ho.account.audit.domain.*;
import com.ho.account.audit.repository.AuditLogRepository;
import com.ho.account.audit.repository.AuthorizationRepository;
import com.ho.account.audit.repository.SystemRoleRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final SystemRoleRepository systemRoleRepository;
    private final AuthorizationRepository authorizationRepository;

    @Autowired
    public AuditService(AuditLogRepository auditLogRepository,
            SystemRoleRepository systemRoleRepository,
            AuthorizationRepository authorizationRepository) {
        this.auditLogRepository = auditLogRepository;
        this.systemRoleRepository = systemRoleRepository;
        this.authorizationRepository = authorizationRepository;
    }

    // ===== 감사 로그 =====

    public AuditLog logEvent(String eventType, String userId, String targetEntity,
            String targetId, String beforeData, String afterData, String status, String remarks, String ipAddress) {
        AuditLog log = new AuditLog();
        log.setEventType(eventType);
        log.setUserId(userId);
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setBeforeData(beforeData);
        log.setAfterData(afterData);
        log.setStatus(status);
        log.setRemarks(remarks);
        log.setIpAddress(ipAddress);
        return auditLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByEventType(String eventType) {
        return auditLogRepository.findByEventType(eventType);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUser(String userId) {
        return auditLogRepository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByTarget(String targetEntity, String targetId) {
        return auditLogRepository.findByTargetEntityAndTargetId(targetEntity, targetId);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogRepository.findByEventDateTimeBetween(start, end);
    }

    // ===== 역할 관리 =====

    public SystemRole createRole(String roleCode, String roleName, String description) {
        if (systemRoleRepository.existsByRoleCode(roleCode)) {
            throw new IllegalArgumentException("이미 존재하는 역할 코드: " + roleCode);
        }
        SystemRole role = new SystemRole();
        role.setRoleCode(roleCode);
        role.setRoleName(roleName);
        role.setDescription(description);
        return systemRoleRepository.save(role);
    }

    @Transactional(readOnly = true)
    public List<SystemRole> getAllRoles() {
        return systemRoleRepository.findAll();
    }

    @Transactional(readOnly = true)
    public SystemRole getRoleByCode(String roleCode) {
        return systemRoleRepository.findByRoleCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("역할을 찾을 수 없습니다: " + roleCode));
    }

    // ===== 권한 관리 =====

    public Authorization grantAuthorization(String roleCode, String functionCode,
            AccessType accessType, String dataScope) {
        SystemRole role = getRoleByCode(roleCode);

        if (authorizationRepository.existsByRoleIdAndFunctionCodeAndAccessType(
                role.getId(), functionCode, accessType)) {
            throw new IllegalArgumentException("이미 부여된 권한: " + roleCode + " / " + functionCode + " / " + accessType);
        }

        Authorization auth = new Authorization();
        auth.setRole(role);
        auth.setFunctionCode(functionCode);
        auth.setAccessType(accessType);
        auth.setDataScope(dataScope);
        return authorizationRepository.save(auth);
    }

    public void revokeAuthorization(Long authorizationId) {
        authorizationRepository.deleteById(authorizationId);
    }

    @Transactional(readOnly = true)
    public List<Authorization> getAuthorizationsByRole(String roleCode) {
        SystemRole role = getRoleByCode(roleCode);
        return authorizationRepository.findByRoleId(role.getId());
    }

    /**
     * SOD(직무분리) 검사: 특정 역할이 특정 기능에 대해 특정 접근 유형의 권한을 가지고 있는지 확인
     */
    @Transactional(readOnly = true)
    public boolean hasPermission(String roleCode, String functionCode, AccessType accessType) {
        SystemRole role = systemRoleRepository.findByRoleCode(roleCode).orElse(null);
        if (role == null)
            return false;
        return authorizationRepository.existsByRoleIdAndFunctionCodeAndAccessType(
                role.getId(), functionCode, accessType);
    }
}
