package com.ho.account.auth.core.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.auth.core.application.model.AdminUserView;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.domain.model.RoleAssignment;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class AdminUserQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-09-24T00:00:00Z");
    private static final long JAVASCRIPT_MAX_SAFE_INTEGER = 9_007_199_254_740_991L;

    @ParameterizedTest
    @MethodSource("roleMappings")
    void mapsEffectiveRoleCodesToFrontendRoles(String storedRole, String expectedRole) {
        AdminUserQueryService service = service(List.of(user(
                "user@example.com", true, false, "FIN", List.of(RoleAssignment.approved(storedRole)))));

        assertThat(service.findAllUsers()).singleElement()
                .extracting(AdminUserView::role)
                .isEqualTo(expectedRole);
    }

    @Test
    void projectsSortedUsersWithStableJavascriptSafeIdsAndAccountStatus() {
        AuthUser zeta = user(
                "zeta@example.com", true, true, null, List.of(RoleAssignment.approved("ROLE_AUDITOR")));
        AuthUser alpha = user(
                "alpha@example.com",
                true,
                false,
                "FIN",
                List.of(new RoleAssignment("ROLE_FUTURE", "GLOBAL", NOW.plusSeconds(1), null, true)));

        List<AdminUserView> first = service(List.of(zeta, alpha)).findAllUsers();
        List<AdminUserView> reordered = service(List.of(alpha, zeta)).findAllUsers();
        List<AdminUserView> alphaOnly = service(List.of(alpha)).findAllUsers();

        assertThat(first).extracting(AdminUserView::email)
                .containsExactly("alpha@example.com", "zeta@example.com");
        assertThat(first.get(0).id()).isEqualTo(reordered.get(0).id());
        assertThat(first.get(0).id()).isEqualTo(alphaOnly.get(0).id());
        assertThat(first).allSatisfy(view -> assertThat(view.id())
                .isPositive()
                .isLessThanOrEqualTo(JAVASCRIPT_MAX_SAFE_INTEGER));
        assertThat(first.get(0))
                .extracting(
                        AdminUserView::name,
                        AdminUserView::role,
                        AdminUserView::status,
                        AdminUserView::lastLogin,
                        AdminUserView::department)
                .containsExactly("alpha@example.com", "USER", "ACTIVE", "", "FIN");
        assertThat(first.get(1).status()).isEqualTo("INACTIVE");
        assertThat(first.get(1).department()).isEmpty();
    }

    private AdminUserQueryService service(List<AuthUser> users) {
        AuthUserQueryPort queryPort = new AuthUserQueryPort() {
            @Override
            public Optional<AuthUser> findByUsername(String username) {
                return users.stream().filter(user -> user.hasUsername(username)).findFirst();
            }

            @Override
            public List<AuthUser> findAllUsers() {
                return users;
            }
        };
        return new AdminUserQueryService(queryPort, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuthUser user(
            String username,
            boolean active,
            boolean locked,
            String department,
            List<RoleAssignment> assignments) {
        return new AuthUser(username, "stored", department, active, locked, assignments, 1L);
    }

    private static Stream<Arguments> roleMappings() {
        return Stream.of(
                Arguments.of("ROLE_SYSTEM_ADMIN", "SYSTEM_ADMIN"),
                Arguments.of("ROLE_ADMIN", "SYSTEM_ADMIN"),
                Arguments.of("ROLE_ACCOUNTING_ADMIN", "ACCOUNTING_ADMIN"),
                Arguments.of("ROLE_RISK_MANAGER", "RISK_MANAGER"),
                Arguments.of("ROLE_RISK_ANALYST", "RISK_ANALYST"),
                Arguments.of("ROLE_MASTER_MANAGER", "MASTER_MANAGER"),
                Arguments.of("ROLE_AUDITOR", "AUDITOR"),
                Arguments.of("ROLE_USER", "USER"),
                Arguments.of("ROLE_UNKNOWN", "USER"));
    }
}
