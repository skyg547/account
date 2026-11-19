package com.ho.account.auth.core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.auth.core.application.exception.GovernanceException;
import com.ho.account.auth.core.application.model.GovernanceActor;
import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.application.port.out.GovernanceCredentialVerifierPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.shared.infrastructure.security.application.port.in.MasterApprovalUseCase.RequestApprovalCommand;
import com.ho.account.shared.infrastructure.security.application.port.out.*;
import com.ho.account.shared.infrastructure.security.domain.*;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Application orchestration tests; actual commit/rollback evidence is in GovernanceHttpIntegrationTest. */
class GovernanceServiceTest {
    final GovernanceCredentialVerifierPort credentials = mock(GovernanceCredentialVerifierPort.class);
    final MasterApprovalPersistencePort approvals = mock(MasterApprovalPersistencePort.class);
    final SystemRolePersistencePort roles = mock(SystemRolePersistencePort.class);
    final AuthorizationPersistencePort grants = mock(AuthorizationPersistencePort.class);
    final AuditLogPersistencePort logs = mock(AuditLogPersistencePort.class);
    final AuthUserRoleAssignmentUseCase assignments = mock(AuthUserRoleAssignmentUseCase.class);
    final AuthUserQueryPort users = mock(AuthUserQueryPort.class);
    final GovernanceService service = new GovernanceService(credentials, approvals, roles, grants, logs, assignments,
            users, Clock.fixed(Instant.parse("2026-11-19T12:00:00Z"), ZoneOffset.UTC), new ObjectMapper());

    @BeforeEach
    void stubPersistence() {
        when(users.findByUsername("owner")).thenReturn(Optional.of(new AuthUser("owner", UUID.randomUUID().toString(), true, false, List.of("ROLE_USER"))));
        when(approvals.save(any())).thenAnswer(invocation -> {
            MasterApproval approval = invocation.getArgument(0);
            if (approval.getId() == null) { approval.setId(42L); approval.setStatus(MasterApproval.ApprovalStatus.PENDING); }
            return approval;
        });
    }

    @Test
    void requestUsesVerifiedActorAndDomainDefaultsInsteadOfCallerIdentity() {
        when(credentials.verify("credential")).thenReturn(new GovernanceActor("owner", Set.of("ROLE_USER")));
        MasterApproval approval = service.request("credential", command("owner"));
        assertThat(approval.getRequestUser()).isEqualTo("owner");
        assertThat(approval.getAuditUser()).isEqualTo("owner");
        assertThat(approval.getEffectiveDate()).hasToString("2026-11-19");
        assertThat(approval.getRequestedVersion()).isEqualTo(1);
        ArgumentCaptor<AuditLog> audit = ArgumentCaptor.forClass(AuditLog.class);
        verify(logs).save(audit.capture());
        assertThat(audit.getValue().getUserId()).isEqualTo("owner");
        verifyNoInteractions(assignments);
    }

    @Test
    void otherIdentityIsDeniedBeforePersistence() {
        when(credentials.verify("credential")).thenReturn(new GovernanceActor("owner", Set.of("ROLE_USER")));
        assertThatThrownBy(() -> service.request("credential", command("other"))).isInstanceOf(GovernanceException.class);
        verify(approvals, never()).save(any());
        verifyNoInteractions(assignments, logs);
    }

    @Test
    void decisionsBindCheckerAndUseLockedReceiptBeforeLocalApplicationAndAudit() {
        MasterApproval approval = pending();
        when(credentials.verify("credential")).thenReturn(new GovernanceActor("checker", Set.of("ROLE_SYSTEM_ADMIN")));
        when(approvals.findByIdForDecision(42L)).thenReturn(Optional.of(approval));
        service.decide("credential", 42L, true, "reviewed");
        ArgumentCaptor<AuthUserRoleAssignmentUseCase.ReplaceRoleAssignmentsCommand> change = ArgumentCaptor.forClass(AuthUserRoleAssignmentUseCase.ReplaceRoleAssignmentsCommand.class);
        verify(assignments).replaceRoleAssignments(change.capture());
        assertThat(change.getValue().approvedBy()).isEqualTo("checker");
        assertThat(change.getValue().approvalTraceId()).isEqualTo("governance-approval-id=42");
        assertThat(change.getValue().username()).isEqualTo("owner");
        assertThat(approval.getStatus()).isEqualTo(MasterApproval.ApprovalStatus.APPROVED);
        var order = inOrder(approvals, assignments, logs);
        order.verify(approvals).findByIdForDecision(42L);
        order.verify(assignments).replaceRoleAssignments(any());
        order.verify(approvals).save(approval);
        order.verify(logs).save(any());
    }

    @Test
    void selfDecisionIsDeniedForApproveAndRejectWithoutApplyingAnything() {
        when(credentials.verify("credential")).thenReturn(new GovernanceActor("owner", Set.of("ROLE_SYSTEM_ADMIN")));
        when(approvals.findByIdForDecision(42L)).thenReturn(Optional.of(pending()));
        for (boolean approve : List.of(true, false))
            assertThatThrownBy(() -> service.decide("credential", 42L, approve, null)).isInstanceOf(GovernanceException.class);
        verifyNoInteractions(assignments, logs);
        verify(approvals, never()).save(any());
    }

    private MasterApproval pending() {
        MasterApproval approval = new MasterApproval();
        approval.setId(42L); approval.setMasterType("AUTH_USER_ROLE"); approval.setMasterKey("owner");
        approval.setRequestType(MasterApproval.ChangeRequestType.UPDATE); approval.setRequestUser("owner");
        approval.setStatus(MasterApproval.ApprovalStatus.PENDING); approval.setPayload("{\"role\":\"USER\"}");
        approval.setEffectiveDate(java.time.LocalDate.of(2026, 11, 19));
        return approval;
    }
    private RequestApprovalCommand command(String target) {
        return new RequestApprovalCommand("AUTH_USER_ROLE", target, MasterApproval.ChangeRequestType.UPDATE,
                "{\"role\":\"USER\"}", "forged", null, null);
    }
}
