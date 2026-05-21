package com.ho.account.auth.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase.ReplaceRoleAssignmentsCommand;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AuthUserRoleAssignmentServiceTest {

    @Test
    void replaceRoleAssignments_normalizesRolesAndReturnsNewRoleVersion() {
        AtomicReference<List<RoleAssignment>> savedAssignments = new AtomicReference<>();
        AuthUserRoleAssignmentService service = new AuthUserRoleAssignmentService((username, assignments, approvedBy) -> {
            savedAssignments.set(assignments);
            return new AuthUser(username, "{noop}pw", "FIN", true, false, assignments, 7L);
        });

        var result = service.replaceRoleAssignments(new ReplaceRoleAssignmentsCommand(
                " admin ",
                List.of("accounting_admin", "ROLE_AUDITOR", "accounting_admin"),
                "FIN",
                null,
                null,
                "approver01",
                "governance-approval-id=10"));

        assertThat(savedAssignments.get())
                .extracting(RoleAssignment::roleCode)
                .containsExactly("ROLE_ACCOUNTING_ADMIN", "ROLE_AUDITOR");
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.roleVersion()).isEqualTo(7L);
        assertThat(result.roles()).containsExactly("ROLE_ACCOUNTING_ADMIN", "ROLE_AUDITOR");
    }

    @Test
    void replaceRoleAssignments_rejectsEmptyRoles() {
        AuthUserRoleAssignmentService service = new AuthUserRoleAssignmentService((username, assignments, approvedBy) ->
                new AuthUser(username, "{noop}pw", true, false, List.of()));

        assertThatThrownBy(() -> service.replaceRoleAssignments(new ReplaceRoleAssignmentsCommand(
                "admin",
                List.of(),
                null,
                null,
                null,
                "approver01",
                null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("roleCode");
    }
}
