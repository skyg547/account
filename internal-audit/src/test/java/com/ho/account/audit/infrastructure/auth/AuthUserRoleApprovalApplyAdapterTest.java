package com.ho.account.audit.infrastructure.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.audit.application.model.AuthUserRoleAssignmentChange;
import com.ho.account.audit.domain.MasterApproval;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AuthUserRoleApprovalApplyAdapterTest {

    @Test
    void applyApprovedChange_mapsAuthUserRoleApprovalToAuthRoleAssignmentChange() {
        AtomicReference<AuthUserRoleAssignmentChange> applied = new AtomicReference<>();
        AuthUserRoleApprovalApplyAdapter adapter = new AuthUserRoleApprovalApplyAdapter(
                applied::set,
                new ObjectMapper());
        MasterApproval approval = new MasterApproval();
        approval.setId(42L);
        approval.setMasterType("AUTH_USER_ROLE");
        approval.setMasterKey("fallback-user");
        approval.setRequestType(MasterApproval.ChangeRequestType.UPDATE);
        approval.setPayload("""
                {"username":"admin","role":"accounting_admin","dataScope":"FIN"}
                """);
        approval.setApproverUser("approver01");
        approval.setEffectiveDate(LocalDate.of(2026, 6, 1));

        adapter.applyApprovedChange(approval);

        AuthUserRoleAssignmentChange change = applied.get();
        assertThat(change.username()).isEqualTo("admin");
        assertThat(change.roleCodes()).containsExactly("ROLE_ACCOUNTING_ADMIN");
        assertThat(change.dataScope()).isEqualTo("FIN");
        assertThat(change.validFrom()).isNotNull();
        assertThat(change.approvedBy()).isEqualTo("approver01");
        assertThat(change.approvalTraceId()).isEqualTo("governance-approval-id=42");
    }

    @Test
    void supports_onlyAuthUserRoleMasterType() {
        AuthUserRoleApprovalApplyAdapter adapter = new AuthUserRoleApprovalApplyAdapter(
                change -> {
                },
                new ObjectMapper());

        assertThat(adapter.supports("AUTH_USER_ROLE")).isTrue();
        assertThat(adapter.supports("SYSTEM_ROLE")).isFalse();
    }
}
