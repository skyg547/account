package com.ho.account.budget.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class BudgetProductionPostgresqlTlsGuardTest {

    @Test
    void acceptsExactlyOneVerifyFullMode() {
        assertThatCode(() -> BudgetProductionPostgresqlTlsGuard.requireVerifyFull(
                        "jdbc:postgresql://db.invalid:5432/budget?connectTimeout=5&sslmode=verify-full"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsNonPostgresqlMissingWeakOrAmbiguousTlsModes() {
        assertRejected("jdbc:h2:mem:budget");
        assertRejected("jdbc:postgresql://db.invalid:5432/budget");
        assertRejected("jdbc:postgresql://db.invalid:5432/budget?sslmode=require");
        assertRejected(
                "jdbc:postgresql://db.invalid:5432/budget?sslmode=verify-full&sslmode=disable");
    }

    private void assertRejected(String url) {
        assertThatThrownBy(() -> BudgetProductionPostgresqlTlsGuard.requireVerifyFull(url))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("sslmode=verify-full");
    }
}
