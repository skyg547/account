package com.ho.account.auth.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.application.port.in.AuthUserRoleAssignmentUseCase.ReplaceRoleAssignmentsCommand;
import com.ho.account.auth.core.application.port.out.AuthUserRoleAssignmentPersistencePort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AuthUserRoleAssignmentServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-14T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    @Test
    void replaceRoleAssignmentsNormalizesRolesAndBuildsIdempotentReplacement() {
        AtomicReference<AuthUserRoleAssignmentPersistencePort.RoleAssignmentReplacement> saved =
                new AtomicReference<>();
        AuthUserRoleAssignmentService service = new AuthUserRoleAssignmentService(replacement -> {
            saved.set(replacement);
            return new AuthUser(
                    replacement.username(),
                    "{noop}pw",
                    "FIN",
                    true,
                    false,
                    replacement.roleAssignments(),
                    7L);
        }, CLOCK);

        var result = service.replaceRoleAssignments(new ReplaceRoleAssignmentsCommand(
                " admin ",
                List.of("ROLE_AUDITOR", "accounting_admin", "accounting_admin"),
                "GLOBAL",
                null,
                null,
                "approver01",
                "governance-approval-id=10"));

        assertThat(saved.get().roleAssignments())
                .extracting(RoleAssignment::roleCode)
                .containsExactly("ROLE_ACCOUNTING_ADMIN", "ROLE_AUDITOR");
        assertThat(saved.get().approvalTraceId()).isEqualTo("governance-approval-id=10");
        assertThat(saved.get().requestFingerprint()).hasSize(64);
        assertThat(saved.get().roleAssignments()).allSatisfy(assignment ->
                assertThat(assignment.dataScope()).isEqualTo("GLOBAL"));
        assertThat(result.username()).isEqualTo("admin");
        assertThat(result.roleVersion()).isEqualTo(7L);
        assertThat(result.roles()).containsExactly("ROLE_ACCOUNTING_ADMIN", "ROLE_AUDITOR");
    }

    @Test
    void replaceRoleAssignmentsReturnsOnlyRolesEffectiveAtServiceClock() {
        AuthUserRoleAssignmentService service = new AuthUserRoleAssignmentService(replacement ->
                new AuthUser(
                        replacement.username(),
                        "{noop}pw",
                        "FIN",
                        true,
                        false,
                        replacement.roleAssignments(),
                        2L), CLOCK);

        var result = service.replaceRoleAssignments(new ReplaceRoleAssignmentsCommand(
                "admin",
                List.of("ROLE_FUTURE"),
                "GLOBAL",
                NOW.plusSeconds(60),
                null,
                "approver01",
                "governance-approval-id=11"));

        assertThat(result.roles()).isEmpty();
    }

    @Test
    void replaceRoleAssignmentsRejectsEmptyRoles() {
        AuthUserRoleAssignmentService service = service();

        assertThatThrownBy(() -> service.replaceRoleAssignments(new ReplaceRoleAssignmentsCommand(
                "admin",
                List.of(),
                null,
                null,
                null,
                "approver01",
                "governance-approval-id=12")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("roleCode");
    }

    @Test
    void replaceRoleAssignmentsRequiresApprovalTraceId() {
        AuthUserRoleAssignmentService service = service();

        assertThatThrownBy(() -> service.replaceRoleAssignments(new ReplaceRoleAssignmentsCommand(
                "admin",
                List.of("ROLE_ADMIN"),
                null,
                null,
                null,
                "approver01",
                null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("approvalTraceId");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", " ", "FIN", "GLOBAL:FIN", "GLOBAL,FIN", "global", " GLOBAL "})
    void replaceRoleAssignmentsRejectsUnapprovedScopeBeforePersistence(String scope) {
        AtomicReference<AuthUserRoleAssignmentPersistencePort.RoleAssignmentReplacement> saved =
                new AtomicReference<>();
        AuthUserRoleAssignmentService service = new AuthUserRoleAssignmentService(replacement -> {
            saved.set(replacement);
            throw new AssertionError("Invalid scope reached persistence");
        }, CLOCK);

        assertThatThrownBy(() -> service.replaceRoleAssignments(new ReplaceRoleAssignmentsCommand(
                "admin", List.of("ROLE_SYSTEM_ADMIN"), scope, null, null,
                "approver01", "governance-approval-id=invalid-scope")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("dataScope");
        assertThat(saved.get()).isNull();
    }

    private AuthUserRoleAssignmentService service() {
        return new AuthUserRoleAssignmentService(replacement -> new AuthUser(
                replacement.username(),
                "{noop}pw",
                true,
                false,
                List.of("ROLE_ADMIN")), CLOCK);
    }
}
