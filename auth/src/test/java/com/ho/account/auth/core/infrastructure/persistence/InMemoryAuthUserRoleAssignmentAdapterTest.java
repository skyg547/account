package com.ho.account.auth.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.application.port.out.AuthUserRoleAssignmentPersistencePort.RoleAssignmentReplacement;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class InMemoryAuthUserRoleAssignmentAdapterTest {

    @Test
    void preservesRoleMetadataAndMakesApprovalRetryIdempotent() {
        InMemoryAuthUserQueryAdapter queryAdapter = queryAdapter();
        InMemoryAuthUserRoleAssignmentAdapter adapter =
                new InMemoryAuthUserRoleAssignmentAdapter(queryAdapter);
        RoleAssignment assignment = new RoleAssignment(
                "ROLE_AUDITOR",
                "FIN",
                Instant.parse("2026-07-14T00:00:00Z"),
                Instant.parse("2026-12-31T00:00:00Z"),
                true);
        RoleAssignmentReplacement replacement = new RoleAssignmentReplacement(
                "admin",
                List.of(assignment),
                "approver01",
                "governance-approval-id=100",
                "a".repeat(64));

        AuthUser first = adapter.replaceRoleAssignments(replacement);
        AuthUser retried = adapter.replaceRoleAssignments(replacement);

        assertThat(first.getRoleVersion()).isEqualTo(2L);
        assertThat(retried.getRoleVersion()).isEqualTo(2L);
        assertThat(retried.getRoleAssignments()).containsExactly(assignment);
    }

    @Test
    void reusedApprovalTraceWithDifferentPayloadFailsClosed() {
        InMemoryAuthUserRoleAssignmentAdapter adapter =
                new InMemoryAuthUserRoleAssignmentAdapter(queryAdapter());
        adapter.replaceRoleAssignments(replacement(
                "governance-approval-id=101",
                "a".repeat(64),
                "ROLE_AUDITOR"));

        assertThatThrownBy(() -> adapter.replaceRoleAssignments(replacement(
                "governance-approval-id=101",
                "b".repeat(64),
                "ROLE_ADMIN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different role assignment request");
    }

    private RoleAssignmentReplacement replacement(String traceId, String fingerprint, String roleCode) {
        return new RoleAssignmentReplacement(
                "admin",
                List.of(RoleAssignment.approved(roleCode)),
                "approver01",
                traceId,
                fingerprint);
    }

    private InMemoryAuthUserQueryAdapter queryAdapter() {
        AuthModuleProperties properties = new AuthModuleProperties();
        AuthModuleProperties.User user = new AuthModuleProperties.User();
        user.setUsername("admin");
        user.setPassword("{noop}1234");
        user.setDepartmentCode("FIN");
        user.setRoles(List.of("ROLE_ADMIN"));
        properties.setUsers(List.of(user));
        return new InMemoryAuthUserQueryAdapter(properties);
    }
}
