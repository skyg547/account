package com.ho.account.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @ValueSource(strings = {
            "sslmode=verify-full",
            "ApplicationName=migration&sslmode=verify-full",
            "sslmode=verify-full&connectTimeout=10",
            "ApplicationName=migration&sslmode=verify-full&connectTimeout=10",
            "connectTimeout=10&sslmode=verify-full&ApplicationName=migration%20runner"
    })
    void acceptedProductionUrlsRemainUnchangedAndMatchTheRuntimeDriver(String query)
            throws ReflectiveOperationException {
        String url = "jdbc:postgresql://db.example/auth_prod?" + query;

        MigrationConfiguration configuration = configurationFor(url, "production");

        assertThat(configuration.url()).isEqualTo(url);
        assertThat(parseWithRuntimeDriver(configuration.url()).getProperty("sslmode"))
                .isEqualTo("verify-full");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "sslmode=verify-full&sslmode=disable",
            "sslmode=disable&sslmode=verify-full",
            "sslmode=verify-full&sslmode=verify-full",
            "sslmode=verify-full&sslmode=",
            "sslmode=&sslmode=verify-full",
            "sslmode=verify-full&sslmode",
            "sslmode&sslmode=verify-full",
            "sslmode=verify-full&SSLMODE=verify-full",
            "SSLMODE=verify-full&sslmode=verify-full",
            "sslmode=verify-full&ApplicationName=migration&sslmode=disable",
            "sslmode=verify-full&sslmode=verify-full&sslmode=verify-full"
    })
    void productionRejectsEveryDuplicateSslmodeKey(String query) {
        assertThatThrownBy(() -> configurationFor(
                "jdbc:postgresql://db.example/auth_prod?" + query, "production"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Production MIGRATION_DB_URL must use sslmode=verify-full");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "?", "?ApplicationName=migration", "?sslmode", "?sslmode=",
            "?SSLMODE=verify-full", "?SslMode=verify-full", "?sslmode=VERIFY-FULL",
            "?sslmode=Verify-Full", "?sslmode=verify-full=disable",
            "?sslmode=verify%2Dfull", "?sslmode=%76erify-full", "?%73slmode=verify-full",
            "?ssl%6dode=verify-full", "?sslmode=verify-full%20"
    })
    void productionRejectsMissingOrNoncanonicalSslmode(String suffix) {
        assertThatThrownBy(() -> configurationFor(
                "jdbc:postgresql://db.example/auth_prod" + suffix, "production"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Production MIGRATION_DB_URL must use sslmode=verify-full");
    }

    @Test
    void rejectsTheUrlWhoseLastSslmodeDisablesTlsInTheRuntimeDriver()
            throws ReflectiveOperationException {
        String url = "jdbc:postgresql://db.example/auth_prod?sslmode=verify-full&sslmode=disable";

        assertThat(parseWithRuntimeDriver(url).getProperty("sslmode")).isEqualTo("disable");
        assertThatThrownBy(() -> configurationFor(url, "production"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Production MIGRATION_DB_URL must use sslmode=verify-full");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "?sslmode=disable", "?sslmode", "?sslmode=",
            "?sslmode=verify-full&sslmode=disable", "?sslmode=disable&sslmode=verify-full",
            "?SSLMODE=VERIFY-FULL", "?sslmode=verify%2Dfull"
    })
    void developmentRetainsItsExistingTlsInputContract(String suffix) {
        String url = "jdbc:postgresql://db.example/auth_dev" + suffix;

        assertThat(configurationFor(url, "development").url()).isEqualTo(url);
    }

    private static MigrationConfiguration configurationFor(String url, String targetEnvironment) {
        return MigrationConfiguration.from(Map.of(
                "MIGRATION_DB_URL", url,
                "MIGRATION_DB_USER", "auth_migrator",
                "MIGRATION_DB_PASSWORD", "secret",
                "MIGRATION_TARGET_ENV", targetEnvironment,
                "MIGRATION_EXPECTED_DATABASE",
                "production".equals(targetEnvironment) ? "auth_prod" : "auth_dev"),
                MigrationContext.require("auth"));
    }

    private static Properties parseWithRuntimeDriver(String url) throws ReflectiveOperationException {
        // Inspect the existing runtime driver's interpretation without a compile dependency or connection.
        Properties properties = (Properties) Class.forName("org.postgresql.Driver")
                .getMethod("parseURL", String.class, Properties.class)
                .invoke(null, url, new Properties());
        assertThat(properties).isNotNull();
        return properties;
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
