package com.ho.account.auth.core.domain.model;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class GovernanceChangePolicyTest {
    @Test
    void supportedCombinationsAreExplicit() {
        GovernanceChangePolicy.supportedChange("AUTH_USER_ROLE", "UPDATE");
        GovernanceChangePolicy.supportedChange("SYSTEM_ROLE", "CREATE");
        GovernanceChangePolicy.supportedChange("AUTHORIZATION", "DELETE");
        for (String[] pair : List.of(new String[] {"AUTH_USER_ROLE", "DELETE"}, new String[] {"SYSTEM_ROLE", "UPDATE"},
                new String[] {"AUTHORIZATION", "UPDATE"}, new String[] {"DEPARTMENT", "CREATE"}))
            assertThatThrownBy(() -> GovernanceChangePolicy.supportedChange(pair[0], pair[1])).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void domainNormalizesRolesAndOmittedScopeWithUtcValidity() {
        var change = GovernanceRoleChange.forTarget("owner", "owner", List.of("user", "ROLE_USER"), null,
                LocalDate.of(2026, 11, 19), null);
        assertThat(change.roleCodes()).containsExactly("ROLE_USER");
        assertThat(change.dataScope()).isEqualTo("GLOBAL");
        assertThat(change.validFrom()).hasToString("2026-11-19T00:00:00Z");
        assertThatThrownBy(() -> change.roleCodes().add("ROLE_ADMIN")).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void targetScopeRoleAndPeriodFailuresAreDomainRules() {
        LocalDate day = LocalDate.of(2026, 11, 19);
        assertThatThrownBy(() -> GovernanceRoleChange.forTarget("owner", "Owner", List.of("USER"), null, day, null))
                .isInstanceOf(IllegalArgumentException.class);
        for (String scope : List.of("", "DEPARTMENT", "GLOBAL:branch"))
            assertThatThrownBy(() -> GovernanceRoleChange.forTarget("owner", "owner", List.of("USER"), scope, day, null))
                    .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GovernanceRoleChange.forTarget("owner", "owner", List.of("ROLE_"), null, day, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GovernanceRoleChange.forTarget("owner", "owner", List.of(), null, day, null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GovernanceRoleChange.forTarget("owner", "owner", List.of("USER"), null, day,
                day.atStartOfDay(java.time.ZoneOffset.UTC).toInstant())).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void receiptDefaultsAndCatalogKeysAreValidated() {
        LocalDate today = LocalDate.of(2026, 11, 19);
        assertThat(GovernanceChangePolicy.effectiveDate(null, today)).isEqualTo(today);
        assertThat(GovernanceChangePolicy.requestedVersion(null)).isEqualTo(1);
        assertThatThrownBy(() -> GovernanceChangePolicy.requestedVersion(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GovernanceChangePolicy.roleDefinition("USER", "OTHER", "User", null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> GovernanceChangePolicy.authorization("USER:f:READ", "USER", "g", "READ"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
