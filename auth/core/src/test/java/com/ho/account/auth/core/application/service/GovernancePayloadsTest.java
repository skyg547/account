package com.ho.account.auth.core.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase.ReplaceRoleAssignmentsCommand;
import com.ho.account.shared.infrastructure.security.domain.MasterApproval;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GovernancePayloadsTest {
    private final GovernancePayloads payloads = new GovernancePayloads(new ObjectMapper().findAndRegisterModules());

    @Test
    void omittedScopeBecomesGlobalAndLegacyDisplayUserIdNeverSelectsTarget() {
        var command = (ReplaceRoleAssignmentsCommand) payloads.validate(approval(
                "{\"username\":\"owner\",\"userId\":\"123\",\"role\":\"user\"}"));
        assertThat(command.username()).isEqualTo("owner");
        assertThat(command.dataScope()).isEqualTo("GLOBAL");
        assertThat(command.roleCodes()).containsExactly("ROLE_USER");
        assertThat(command.approvedBy()).isEqualTo("checker");
        assertThat(command.approvalTraceId()).isEqualTo("governance-approval-id=42");
        assertThat(command.validFrom()).hasToString("2026-11-19T00:00:00Z");
    }

    @Test
    void unsupportedScopeTargetMismatchAndMalformedRoleAreRejected() {
        for (String json : new String[] {"{\"username\":\"other\",\"role\":\"USER\"}",
                "{\"role\":\"USER\",\"dataScope\":\"\"}", "{\"role\":\"USER\",\"dataScope\":\"DEPARTMENT\"}",
                "{\"role\":\"ROLE_\"}", "{\"roles\":[null]}", "{\"roles\":[]}",
                "{\"role\":\"USER\",\"validTo\":\"2026-11-18T00:00:00Z\"}",
                "{\"role\":\"USER\",\"validTo\":\"invalid\"}", "null"})
            assertThatThrownBy(() -> payloads.validate(approval(json))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void duplicateAndTrailingJsonCannotHideAnotherCommand() {
        assertThatThrownBy(() -> payloads.validate(approval("{\"role\":\"USER\",\"role\":\"SYSTEM_ADMIN\"}")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> payloads.validate(approval("{\"role\":\"USER\"} {}")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void unsupportedMasterAndDeleteDoNotBecomeApprovedNoOps() {
        MasterApproval approval = approval("{\"role\":\"USER\"}");
        approval.setRequestType(MasterApproval.ChangeRequestType.DELETE);
        assertThatThrownBy(() -> payloads.validate(approval)).isInstanceOf(IllegalArgumentException.class);
        approval.setMasterType("DEPARTMENT");
        assertThatThrownBy(() -> payloads.validate(approval)).isInstanceOf(IllegalArgumentException.class);
    }

    private MasterApproval approval(String payload) {
        MasterApproval approval = new MasterApproval();
        approval.setId(42L); approval.setMasterType("AUTH_USER_ROLE"); approval.setMasterKey("owner");
        approval.setRequestType(MasterApproval.ChangeRequestType.UPDATE); approval.setPayload(payload);
        approval.setEffectiveDate(LocalDate.of(2026, 11, 19)); approval.setApproverUser("checker");
        return approval;
    }
}
