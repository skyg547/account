package com.ho.account.auth.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RoleAssignmentJpaEntityTest {

    @Test
    void rejectsMissingScopeInsteadOfPersistingGlobalGrant() {
        for (String dataScope : new String[] {null, "", " "}) {
            assertThatThrownBy(() -> new RoleAssignmentJpaEntity(
                    "ROLE_ADMIN", dataScope, null, null, true, null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("dataScope");
        }
    }
}
