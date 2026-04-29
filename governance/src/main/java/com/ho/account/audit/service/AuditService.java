package com.ho.account.audit.service;

import com.ho.account.audit.application.port.in.AuditLogUseCase;
import com.ho.account.audit.application.port.in.AuthorizationUseCase;
import com.ho.account.audit.application.port.out.AuditLogPersistencePort;
import com.ho.account.audit.application.port.out.AuthorizationPersistencePort;
import com.ho.account.audit.application.port.out.SystemRolePersistencePort;
import com.ho.account.audit.domain.AccessType;
import com.ho.account.audit.domain.AuditLog;
import com.ho.account.audit.domain.Authorization;
import com.ho.account.audit.domain.SystemRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;

@Service
@Transactional
@RequiredArgsConstructor
public class AuditService implements AuditLogUseCase, AuthorizationUseCase {

    private static final int REMARKS_MAX_LENGTH = 1000;

    private final AuditLogPersistencePort auditLogPersistencePort;
    private final SystemRolePersistencePort systemRolePersistencePort;
    private final AuthorizationPersistencePort authorizationPersistencePort;

    // ===== 감사 로그 =====

    @Override
    public AuditLog logEvent(LogCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Log command is required.");
        }

        AuditLog log = new AuditLog();
        log.setEventType(command.eventType());
        log.setUserId(command.userId());
        log.setTargetEntity(command.targetEntity());
        log.setTargetId(command.targetId());
        log.setBeforeData(command.beforeData());
        log.setAfterData(command.afterData());
        log.setStatus(command.status());
        log.setRemarks(truncate(command.remarks(), REMARKS_MAX_LENGTH));
        log.setIpAddress(command.ipAddress());
        return auditLogPersistencePort.save(log);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByEventType(String eventType) {
        return auditLogPersistencePort.findByEventType(eventType);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByUser(String userId) {
        return auditLogPersistencePort.findByUserId(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByTarget(String targetEntity, String targetId) {
        return auditLogPersistencePort.findByTargetEntityAndTargetId(targetEntity, targetId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLog> getLogsByDateRange(LocalDateTime start, LocalDateTime end) {
        return auditLogPersistencePort.findByEventDateTimeBetween(start, end);
    }

    // ===== 역할 관리 =====

    @Override
    public SystemRole createRole(CreateRoleCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Create role command is required.");
        }
        if (systemRolePersistencePort.existsByRoleCode(command.roleCode())) {
            throw new IllegalArgumentException("이미 존재하는 역할 코드: " + command.roleCode());
        }

        SystemRole role = new SystemRole();
        role.setRoleCode(command.roleCode());
        role.setRoleName(command.roleName());
        role.setDescription(command.description());
        return systemRolePersistencePort.save(role);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SystemRole> getAllRoles() {
        return systemRolePersistencePort.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public SystemRole getRoleByCode(String roleCode) {
        return systemRolePersistencePort.findByRoleCode(roleCode)
                .orElseThrow(() -> new IllegalArgumentException("역할을 찾을 수 없습니다: " + roleCode));
    }

    // ===== 권한 관리 =====

    @Override
    public Authorization grantAuthorization(GrantAuthorizationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("Grant authorization command is required.");
        }
        SystemRole role = getRoleByCode(command.roleCode());

        if (authorizationPersistencePort.existsByRoleIdAndFunctionCodeAndAccessType(
                role.getId(), command.functionCode(), command.accessType())) {
            throw new IllegalArgumentException("이미 부여된 권한: " + command.roleCode()
                    + " / " + command.functionCode() + " / " + command.accessType());
        }

        Authorization auth = new Authorization();
        auth.setRole(role);
        auth.setFunctionCode(command.functionCode());
        auth.setAccessType(command.accessType());
        auth.setDataScope(command.dataScope());
        return authorizationPersistencePort.save(auth);
    }

    @Override
    public void revokeAuthorization(Long authorizationId) {
        authorizationPersistencePort.deleteById(authorizationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Authorization> getAuthorizationsByRole(String roleCode) {
        SystemRole role = getRoleByCode(roleCode);
        return authorizationPersistencePort.findByRoleId(role.getId());
    }

    /**
     * SOD(직무분리) 검사: 특정 역할이 특정 기능에 대해 특정 접근 유형의 권한을 가지고 있는지 확인
     */
    @Override
    @Transactional(readOnly = true)
    public boolean hasPermission(String roleCode, String functionCode, AccessType accessType) {
        SystemRole role = systemRolePersistencePort.findByRoleCode(roleCode).orElse(null);
        if (role == null)
            return false;
        return authorizationPersistencePort.existsByRoleIdAndFunctionCodeAndAccessType(
                role.getId(), functionCode, accessType);
    }
}
