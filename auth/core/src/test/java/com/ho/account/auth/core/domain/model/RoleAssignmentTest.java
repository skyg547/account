package com.ho.account.auth.core.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class RoleAssignmentTest {

    private static final Instant START = Instant.parse("2026-07-14T00:00:00Z");
    private static final Instant END = Instant.parse("2026-07-15T00:00:00Z");

    @Test
    void effectivePeriodIncludesStartAndExcludesEnd() {
        RoleAssignment assignment = new RoleAssignment(
                "ROLE_ADMIN",
                "GLOBAL",
                START,
                END,
                true);

        assertThat(assignment.isEffectiveAt(START)).isTrue();
        assertThat(assignment.isEffectiveAt(END.minusNanos(1))).isTrue();
        assertThat(assignment.isEffectiveAt(END)).isFalse();
    }

    @Test
    void rejectsEmptyOrReversedEffectivePeriod() {
        assertThatThrownBy(() -> new RoleAssignment(
                "ROLE_ADMIN", "GLOBAL", START, START, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("validFrom");
        assertThatThrownBy(() -> new RoleAssignment(
                "ROLE_ADMIN", "GLOBAL", END, START, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("validFrom");
    }

    @Test
    void rejectsValuesLongerThanPersistenceContract() {
        assertThatThrownBy(() -> RoleAssignment.approved("R".repeat(81)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("80");
        assertThatThrownBy(() -> new RoleAssignment(
                "ROLE_ADMIN", "S".repeat(81), null, null, true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("80");
    }

    @Test
    void retainsLegacyScopeTextForExplicitAuthorizationCheck() {
        // Malformed stored rows must hydrate without silently gaining GLOBAL authorization.
        for (String dataScope : new String[] {null, "", " ", "FIN", " GLOBAL "}) {
            RoleAssignment assignment = new RoleAssignment("ROLE_ADMIN", dataScope, null, null, true);
            assertThat(assignment.dataScope()).isEqualTo(dataScope);
            assertThat(assignment.hasSupportedAuthorizationScope()).isFalse();
        }
        assertThat(RoleAssignment.approved("ROLE_ADMIN").hasSupportedAuthorizationScope()).isTrue();
    }
}
