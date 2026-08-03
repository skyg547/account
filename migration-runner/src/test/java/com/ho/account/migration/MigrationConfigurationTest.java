package com.ho.account.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MigrationConfigurationTest {

    @Test
    void acceptsAContextBoundProductionTargetWithFullTlsVerification() {
        MigrationConfiguration configuration = MigrationConfiguration.from(Map.of(
                "MIGRATION_DB_URL", "jdbc:postgresql://db.example/auth_prod?sslmode=verify-full",
                "MIGRATION_DB_USER", "auth_migrator",
                "MIGRATION_DB_PASSWORD", "secret",
                "MIGRATION_TARGET_ENV", "production",
                "MIGRATION_EXPECTED_DATABASE", "auth_prod"), MigrationContext.require("auth"));

        assertThat(configuration.expectedDatabase()).isEqualTo("auth_prod");
    }

    @Test
    void rejectsNonPostgresqlUrls() {
        assertThatThrownBy(() -> MigrationConfiguration.from(Map.of(
                "MIGRATION_DB_URL", "jdbc:h2:mem:unsafe",
                "MIGRATION_DB_USER", "migrator",
                "MIGRATION_DB_PASSWORD", "secret"), MigrationContext.require("auth")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MIGRATION_DB_URL must use jdbc:postgresql://");
    }

    @Test
    void migrateRequiresBothExplicitSwitchAndChangeTicket() {
        assertThatThrownBy(() -> MigrationConfiguration.requireMigrateApproval(Map.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MIGRATION_ALLOW_MIGRATE must be exactly true");

        assertThatThrownBy(() -> MigrationConfiguration.requireMigrateApproval(Map.of(
                "MIGRATION_ALLOW_MIGRATE", "true")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MIGRATION_CHANGE_TICKET is required");
    }

    @Test
    void rejectsCredentialsEmbeddedInJdbcUrl() {
        assertThatThrownBy(() -> MigrationConfiguration.from(Map.of(
                "MIGRATION_DB_URL", "jdbc:postgresql://db.example/context?user=leaked",
                "MIGRATION_DB_USER", "migrator",
                "MIGRATION_DB_PASSWORD", "secret"), MigrationContext.require("auth")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MIGRATION_DB_URL must not contain user or password parameters");

        assertThatThrownBy(() -> MigrationConfiguration.from(Map.of(
                "MIGRATION_DB_URL", "jdbc:postgresql://user:secret@db.example/context",
                "MIGRATION_DB_USER", "migrator",
                "MIGRATION_DB_PASSWORD", "secret"), MigrationContext.require("auth")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MIGRATION_DB_URL must not contain user information");
    }

    @Test
    void bindsTheSelectedContextToTheExpectedDatabaseAndUrl() {
        assertThatThrownBy(() -> MigrationConfiguration.from(Map.of(
                "MIGRATION_DB_URL", "jdbc:postgresql://db.example/loan_dev",
                "MIGRATION_DB_USER", "migrator",
                "MIGRATION_DB_PASSWORD", "secret",
                "MIGRATION_TARGET_ENV", "development",
                "MIGRATION_EXPECTED_DATABASE", "loan_dev"), MigrationContext.require("auth")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MIGRATION_EXPECTED_DATABASE does not match the selected context");

        assertThatThrownBy(() -> MigrationConfiguration.from(Map.of(
                "MIGRATION_DB_URL", "jdbc:postgresql://db.example/auth_dev",
                "MIGRATION_DB_USER", "migrator",
                "MIGRATION_DB_PASSWORD", "secret",
                "MIGRATION_TARGET_ENV", "development",
                "MIGRATION_EXPECTED_DATABASE", "auth_prod"), MigrationContext.require("auth")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("MIGRATION_DB_URL database does not match MIGRATION_EXPECTED_DATABASE");
    }

    @Test
    void productionRequiresFullTlsHostnameVerification() {
        assertThatThrownBy(() -> MigrationConfiguration.from(Map.of(
                "MIGRATION_DB_URL", "jdbc:postgresql://db.example/auth_prod?sslmode=require",
                "MIGRATION_DB_USER", "migrator",
                "MIGRATION_DB_PASSWORD", "secret",
                "MIGRATION_TARGET_ENV", "production",
                "MIGRATION_EXPECTED_DATABASE", "auth_prod"), MigrationContext.require("auth")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Production MIGRATION_DB_URL must use sslmode=verify-full");
    }
}
