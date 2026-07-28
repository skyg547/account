package com.ho.account.audit.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.audit.application.port.out.AuthorizationPersistencePort;
import com.ho.account.audit.application.port.out.SystemRolePersistencePort;
import com.ho.account.audit.domain.MasterApproval;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SystemRoleApprovalApplyAdapterTest {

    private final SystemRolePersistencePort rolePort = Mockito.mock(SystemRolePersistencePort.class);
    private final AuthorizationPersistencePort authorizationPort =
            Mockito.mock(AuthorizationPersistencePort.class);
    private final SystemRoleApprovalApplyAdapter adapter = new SystemRoleApprovalApplyAdapter(
            rolePort,
            authorizationPort,
            new ObjectMapper());

    @Test
    void systemRoleUpdateFailsClosedInsteadOfBecomingApprovedWithoutAChange() {
        MasterApproval approval = approval(
                "SYSTEM_ROLE",
                MasterApproval.ChangeRequestType.UPDATE,
                """
                {"roleCode":"FIN_APPROVER","roleName":"Finance Approver"}
                """);

        assertThatThrownBy(() -> adapter.applyApprovedChange(approval))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported SYSTEM_ROLE requestType: UPDATE");

        verify(rolePort, never()).save(Mockito.any());
    }

    @Test
    void authorizationUpdateFailsClosedInsteadOfBecomingApprovedWithoutAChange() {
        MasterApproval approval = approval(
                "AUTHORIZATION",
                MasterApproval.ChangeRequestType.UPDATE,
                """
                {
                  "roleCode":"FIN_APPROVER",
                  "functionCode":"JOURNAL_APPROVE",
                  "accessType":"EXECUTE"
                }
                """);

        assertThatThrownBy(() -> adapter.applyApprovedChange(approval))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported AUTHORIZATION requestType: UPDATE");

        verify(authorizationPort, never()).save(Mockito.any());
    }

    @Test
    void authorizationDeleteAppliesOnlyTheApprovedIdentifier() {
        MasterApproval approval = approval(
                "AUTHORIZATION",
                MasterApproval.ChangeRequestType.DELETE,
                "42");

        adapter.applyApprovedChange(approval);

        verify(authorizationPort).deleteById(42L);
    }

    @Test
    void unsupportedMasterTypeFailsClosedWhenAdapterIsCalledDirectly() {
        MasterApproval approval = approval(
                "UNKNOWN",
                MasterApproval.ChangeRequestType.CREATE,
                "{}");

        assertThatThrownBy(() -> adapter.applyApprovedChange(approval))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unsupported governance masterType");
    }

    private MasterApproval approval(
            String masterType,
            MasterApproval.ChangeRequestType requestType,
            String payload) {
        MasterApproval approval = new MasterApproval();
        approval.setId(10L);
        approval.setMasterType(masterType);
        approval.setMasterKey("KEY-1");
        approval.setRequestType(requestType);
        approval.setPayload(payload);
        approval.setRequestUser("requester");
        approval.setApproverUser("approver");
        approval.setStatus(MasterApproval.ApprovalStatus.APPROVED);
        return approval;
    }
}