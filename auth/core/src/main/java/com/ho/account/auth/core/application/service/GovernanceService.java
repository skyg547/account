package com.ho.account.auth.core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.auth.core.application.exception.GovernanceException;
import com.ho.account.auth.core.application.model.GovernanceActor;
import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase;
import com.ho.account.auth.core.application.port.in.GovernanceUseCase;
import com.ho.account.auth.core.domain.model.GovernanceChangePolicy;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.application.port.out.GovernanceCredentialVerifierPort;
import com.ho.account.shared.infrastructure.security.application.port.in.AuthorizationUseCase.*;
import com.ho.account.shared.infrastructure.security.application.port.in.MasterApprovalUseCase.RequestApprovalCommand;
import com.ho.account.shared.infrastructure.security.application.port.out.*;
import com.ho.account.shared.infrastructure.security.domain.*;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.ho.account.auth.core.application.exception.GovernanceException.Reason.*;

/** Auth owns the transaction: decision, local role replacement, version, apply log and audit commit together. */
@Service
@Transactional
@ConditionalOnProperty(prefix = "auth.persistence", name = "mode", havingValue = "jpa", matchIfMissing = true)
public class GovernanceService implements GovernanceUseCase {
    private final GovernanceCredentialVerifierPort credentials;
    private final MasterApprovalPersistencePort approvals;
    private final SystemRolePersistencePort roles;
    private final AuthorizationPersistencePort authorizations;
    private final AuditLogPersistencePort logs;
    private final AuthUserRoleAssignmentUseCase assignments;
    private final AuthUserQueryPort users;
    private final Clock clock;
    private final GovernancePayloads payloads;
    private final AuthorizationPolicy policy = new AuthorizationPolicy();

    public GovernanceService(GovernanceCredentialVerifierPort credentials, MasterApprovalPersistencePort approvals,
            SystemRolePersistencePort roles, AuthorizationPersistencePort authorizations, AuditLogPersistencePort logs,
            AuthUserRoleAssignmentUseCase assignments, AuthUserQueryPort users, Clock clock, ObjectMapper json) {
        this.credentials = credentials; this.approvals = approvals; this.roles = roles;
        this.authorizations = authorizations; this.logs = logs; this.assignments = assignments;
        this.users = users; this.clock = clock; this.payloads = new GovernancePayloads(json);
    }

    public MasterApproval request(String credential, RequestApprovalCommand command) {
        GovernanceActor actor = credentials.verify(credential);
        GovernanceChangePolicy.require(command != null && command.requestType() != null);
        GovernanceChangePolicy.text(command.masterType(), 50); GovernanceChangePolicy.text(command.masterKey(), 100);
        // Ordinary users can request a change only for their exact verified identity; admins may manage others.
        if (!actor.systemAdmin() && !("AUTH_USER_ROLE".equals(command.masterType())
                && actor.username().equals(command.masterKey()))) throw new GovernanceException(FORBIDDEN);
        MasterApproval approval = new MasterApproval();
        approval.setMasterType(command.masterType()); approval.setMasterKey(command.masterKey());
        approval.setRequestType(command.requestType()); approval.setPayload(command.payload());
        approval.setRequestUser(actor.username()); approval.setAuditUser(actor.username());
        approval.setEffectiveDate(GovernanceChangePolicy.effectiveDate(command.effectiveDate(), LocalDate.now(clock)));
        approval.setRequestedVersion(GovernanceChangePolicy.requestedVersion(command.requestedVersion()));
        validateTarget(approval, payloads.validate(approval));
        MasterApproval saved = approvals.save(approval);
        audit(saved, actor, "APPROVAL_REQUEST", null);
        return saved;
    }

    public MasterApproval createRole(String credential, CreateRoleCommand command) {
        admin(credential);
        return request(credential, new RequestApprovalCommand("SYSTEM_ROLE", command.roleCode(),
                MasterApproval.ChangeRequestType.CREATE, payloads.write(command), null, null, null));
    }

    public MasterApproval grant(String credential, GrantAuthorizationCommand command) {
        admin(credential);
        return request(credential, new RequestApprovalCommand("AUTHORIZATION",
                command.roleCode() + ":" + command.functionCode() + ":" + command.accessType(),
                MasterApproval.ChangeRequestType.CREATE, payloads.write(command), null, null, null));
    }

    public MasterApproval revoke(String credential, Long id) {
        admin(credential);
        Authorization grant = authorization(id);
        return request(credential, new RequestApprovalCommand("AUTHORIZATION",
                grant.getRole().getRoleCode() + ":" + grant.getFunctionCode() + ":" + grant.getAccessType(),
                MasterApproval.ChangeRequestType.DELETE, payloads.write(id), null, null, null));
    }

    public MasterApproval decide(String credential, Long id, boolean approve, String remarks) {
        GovernanceActor actor = admin(credential);
        GovernanceChangePolicy.optional(remarks, 500);
        // The lock is held across application and audit, so competing approve/reject cannot both succeed.
        MasterApproval approval = approvals.findByIdForDecision(id).orElseThrow(() -> new GovernanceException(NOT_FOUND));
        if (approval.getStatus() != MasterApproval.ApprovalStatus.PENDING) throw new GovernanceException(CONFLICT);
        if (actor.username().equals(approval.getRequestUser())) throw new GovernanceException(FORBIDDEN);
        if (approve) {
            approval.approve(actor.username(), remarks, LocalDateTime.now(clock));
            Object command = payloads.validate(approval);
            // Separate approvals for one role must also serialize their WRITE/EXECUTE SOD checks.
            if (command instanceof GrantAuthorizationCommand grant)
                roles.findByRoleCodeForDecision(grant.roleCode()).orElseThrow(() -> new GovernanceException(NOT_FOUND));
            validateTarget(approval, command);
            apply(approval, command);
        } else approval.reject(actor.username(), remarks, LocalDateTime.now(clock));
        MasterApproval saved = approvals.save(approval);
        audit(saved, actor, approve ? "APPROVAL_APPROVE" : "APPROVAL_REJECT", "PENDING");
        return saved;
    }

    private void validateTarget(MasterApproval approval, Object command) {
        if (command instanceof AuthUserRoleAssignmentUseCase.ReplaceRoleAssignmentsCommand c) {
            users.findByUsername(c.username()).filter(user -> user.hasUsername(c.username()))
                    .orElseThrow(() -> new GovernanceException(NOT_FOUND));
        } else if (command instanceof CreateRoleCommand c) {
            if (roles.existsByRoleCode(c.roleCode())) throw new GovernanceException(CONFLICT);
        } else if (command instanceof GrantAuthorizationCommand c) {
            SystemRole role = role(c.roleCode());
            policy.validateGrant(role, authorizations.findByRoleId(role.getId()), c.functionCode(), c.accessType(), c.dataScope());
            if (authorizations.existsByRoleIdAndFunctionCodeAndAccessType(role.getId(), c.functionCode(), c.accessType()))
                throw new GovernanceException(CONFLICT);
        } else if (command instanceof Long id) {
            Authorization grant = authorization(id);
            GovernanceChangePolicy.require(approval.getMasterKey().equals(grant.getRole().getRoleCode()
                    + ":" + grant.getFunctionCode() + ":" + grant.getAccessType()));
        }
    }

    private void apply(MasterApproval approval, Object command) {
        if (command instanceof AuthUserRoleAssignmentUseCase.ReplaceRoleAssignmentsCommand c) assignments.replaceRoleAssignments(c);
        else if (command instanceof CreateRoleCommand c)
            roles.save(SystemRole.create(c.roleCode(), c.roleName(), c.description(), approval.getApproverUser()));
        else if (command instanceof GrantAuthorizationCommand c)
            authorizations.save(Authorization.grant(role(c.roleCode()), c.functionCode(), c.accessType(), c.dataScope(), approval.getApproverUser()));
        else if (command instanceof Long id) authorizations.deleteById(id);
        else throw new IllegalArgumentException("Unsupported governance command");
    }

    private void audit(MasterApproval approval, GovernanceActor actor, String event, String before) {
        AuditLog log = new AuditLog();
        log.setEventType(event); log.setUserId(actor.username()); log.setAuditUser(actor.username());
        log.setTargetEntity("MASTER_APPROVAL"); log.setTargetId(approval.getId().toString());
        log.setBeforeData(before); log.setAfterData(approval.getPayload());
        log.setStatus(approval.getStatus().name()); log.setRemarks(approval.getRemarks());
        logs.save(log);
    }

    @Transactional(readOnly = true)
    public List<MasterApproval> pending(String credential) { admin(credential); return approvals.findByStatus(MasterApproval.ApprovalStatus.PENDING); }

    @Transactional(readOnly = true)
    public List<SystemRole> roles(String credential) {
        admin(credential);
        List<SystemRole> result = roles.findAll();
        result.forEach(role -> role.getAuthorizations().size());
        return result;
    }

    @Transactional(readOnly = true)
    public SystemRole role(String credential, String code) {
        admin(credential); SystemRole role = role(code); role.getAuthorizations().size(); return role;
    }

    @Transactional(readOnly = true)
    public List<Authorization> authorizations(String credential, String code) {
        GovernanceActor actor = credentials.verify(credential);
        if (!actor.systemAdmin() && !actor.roles().contains(code)) throw new GovernanceException(FORBIDDEN);
        // No catalog row means no grant; never synthesize menu permissions for an assigned role.
        List<Authorization> result = roles.findByRoleCode(code).map(role -> authorizations.findByRoleId(role.getId()).stream()
                .filter(grant -> "GLOBAL".equals(grant.getDataScope())).toList()).orElseGet(List::of);
        result.forEach(grant -> grant.getRole().getRoleCode());
        return result;
    }

    @Transactional(readOnly = true)
    public boolean permission(String credential, String roleCode, String function, AccessType access) {
        GovernanceActor actor = credentials.verify(credential);
        // A caller cannot enumerate another role's permissions, including by asking as an admin.
        if (!actor.roles().contains(roleCode)) throw new GovernanceException(FORBIDDEN);
        GovernanceChangePolicy.text(function, 100); GovernanceChangePolicy.require(access != null);
        return roles.findByRoleCode(roleCode).map(role -> policy.decide(authorizations.findByRoleId(role.getId()).stream()
                .filter(grant -> "GLOBAL".equals(grant.getDataScope())).toList(), function, access).granted()).orElse(false);
    }

    @Transactional(readOnly = true)
    public List<AuditLog> logs(String credential, String kind, String key, String target, LocalDateTime start, LocalDateTime end) {
        admin(credential);
        return switch (kind) {
            case "user" -> logs.findByUserId(GovernanceChangePolicy.text(key, 80));
            case "type" -> logs.findByEventType(GovernanceChangePolicy.text(key, 50));
            case "target" -> logs.findByTargetEntityAndTargetId(GovernanceChangePolicy.text(key, 100), GovernanceChangePolicy.text(target, 100));
            default -> {
                GovernanceChangePolicy.require(start != null && end != null && !end.isBefore(start));
                yield logs.findByEventDateTimeBetween(start, end);
            }
        };
    }

    private GovernanceActor admin(String credential) {
        GovernanceActor actor = credentials.verify(credential);
        if (!actor.systemAdmin()) throw new GovernanceException(FORBIDDEN);
        return actor;
    }
    private SystemRole role(String code) { return roles.findByRoleCode(code).orElseThrow(() -> new GovernanceException(NOT_FOUND)); }
    private Authorization authorization(Long id) { return authorizations.findById(id).orElseThrow(() -> new GovernanceException(NOT_FOUND)); }
}
