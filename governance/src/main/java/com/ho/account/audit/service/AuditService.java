package com.ho.account.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.audit.application.model.AuditActor;
import com.ho.account.audit.application.port.in.AuditLogUseCase;
import com.ho.account.audit.application.port.in.AuthorizationUseCase;
import com.ho.account.audit.application.port.in.MasterApprovalUseCase;
import com.ho.account.audit.application.port.out.AuditActorProviderPort;
import com.ho.account.audit.application.port.out.AuditLogPersistencePort;
import com.ho.account.audit.application.port.out.AuthorizationPersistencePort;
import com.ho.account.audit.application.port.out.SystemRolePersistencePort;
import com.ho.account.audit.domain.AccessType;
import com.ho.account.audit.domain.AuditLog;
import com.ho.account.audit.domain.Authorization;
import com.ho.account.audit.domain.AuthorizationDecision;
import com.ho.account.audit.domain.AuthorizationPolicy;
import com.ho.account.audit.domain.MasterApproval;
import com.ho.account.audit.domain.SystemRole;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [애플리케이션 서비스] AuditService
 * 감사 로그 기록 및 역할/권한 관리를 담당하는 핵심 서비스입니다.
 *
 * 🐣 [초보자를 위한 설명]
 * 이 서비스는 회사의 '감사실장' 역할을 합니다.
 * 1) 누군가 시스템에서 중요한 일을 하면 그 기록(로그)을 안전한 금고에 보관하고(logEvent),
 * 2) "이 직원은 재무제표를 볼 수 있지만 수정은 못해!"와 같이,
 *    직원들의 역할과 권한을 엄격하게 관리(createRole, grantAuthorization)합니다.
 * 특히 한 사람이 돈을 청구하고 스스로 승인하는 것(횡령 위험)을 막기 위한 직무분리(SOD) 검사도 수행합니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class AuditService implements AuditLogUseCase, AuthorizationUseCase {

    private static final int REMARKS_MAX_LENGTH = 1000;
    private static final String SYSTEM_ROLE_MASTER_TYPE = "SYSTEM_ROLE";
    private static final String AUTHORIZATION_MASTER_TYPE = "AUTHORIZATION";

    private final AuditLogPersistencePort auditLogPersistencePort;
    private final SystemRolePersistencePort systemRolePersistencePort;
    private final AuthorizationPersistencePort authorizationPersistencePort;
    private final MasterApprovalUseCase masterApprovalUseCase;
    private final AuditActorProviderPort auditActorProviderPort;
    private final ObjectMapper objectMapper;
    private final AuthorizationPolicy authorizationPolicy = new AuthorizationPolicy();

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

        AuditActor actor = auditActorProviderPort.currentActor();
        requestGovernanceApproval(
                SYSTEM_ROLE_MASTER_TYPE,
                command.roleCode(),
                MasterApproval.ChangeRequestType.CREATE,
                command,
                actor);

        // @todo 역할/권한 생성 API는 아직 저장되지 않은 preview 도메인 객체를 반환한다.
        // 외부 계약을 승인 접수증(approvalId/status) 응답으로 전환해 오해를 없애야 한다.
        return SystemRole.create(command.roleCode(), command.roleName(), command.description(), actor.userId());
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
        List<Authorization> existingAuthorizations = authorizationPersistencePort.findByRoleId(role.getId());
        authorizationPolicy.validateGrant(
                role,
                existingAuthorizations,
                command.functionCode(),
                command.accessType(),
                command.dataScope());

        if (authorizationPersistencePort.existsByRoleIdAndFunctionCodeAndAccessType(
                role.getId(), command.functionCode(), command.accessType())) {
            throw new IllegalArgumentException("이미 부여된 권한: " + command.roleCode()
                    + " / " + command.functionCode() + " / " + command.accessType());
        }

        AuditActor actor = auditActorProviderPort.currentActor();
        GrantAuthorizationCommand normalizedCommand = new GrantAuthorizationCommand(
                command.roleCode(),
                command.functionCode(),
                command.accessType(),
                authorizationPolicy.normalizeDataScope(command.dataScope()));
        requestGovernanceApproval(
                AUTHORIZATION_MASTER_TYPE,
                command.roleCode() + ":" + command.functionCode() + ":" + command.accessType(),
                MasterApproval.ChangeRequestType.CREATE,
                normalizedCommand,
                actor);

        return Authorization.grant(
                role,
                command.functionCode(),
                command.accessType(),
                normalizedCommand.dataScope(),
                actor.userId());
    }

    @Override
    public MasterApproval revokeAuthorization(Long authorizationId) {
        Authorization authorization = authorizationPersistencePort.findById(authorizationId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Authorization not found. ID: " + authorizationId));
        SystemRole role = authorization.getRole();
        if (role == null) {
            throw new IllegalStateException(
                    "Authorization has no role. ID: " + authorizationId);
        }

        AuditActor actor = auditActorProviderPort.currentActor();
        return requestGovernanceApproval(
                AUTHORIZATION_MASTER_TYPE,
                role.getRoleCode() + ":" + authorization.getFunctionCode() + ":" + authorization.getAccessType(),
                MasterApproval.ChangeRequestType.DELETE,
                authorizationId,
                actor);
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
        if (role == null) {
            return false;
        }
        AuthorizationDecision decision = authorizationPolicy.decide(
                authorizationPersistencePort.findByRoleId(role.getId()),
                functionCode,
                accessType);
        return decision.granted();
    }

    private MasterApproval requestGovernanceApproval(
            String masterType,
            String masterKey,
            MasterApproval.ChangeRequestType requestType,
            Object payload,
            AuditActor actor) {
        try {
            return masterApprovalUseCase.requestApproval(new MasterApprovalUseCase.RequestApprovalCommand(
                    masterType,
                    masterKey,
                    requestType,
                    objectMapper.writeValueAsString(payload),
                    actor.userId(),
                    LocalDate.now(),
                    1));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize governance approval payload.", e);
        }
    }
}
