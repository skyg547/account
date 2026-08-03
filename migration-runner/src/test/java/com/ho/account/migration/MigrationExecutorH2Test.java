package com.ho.account.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.stream.Stream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.api.Test;

class MigrationExecutorH2Test {

    @Test
    void rejectsAProvisionedButIncompatibleLegacyBatchSchema() throws Exception {
        String url = "jdbc:h2:mem:legacy-batch;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/auth")
                .load()
                .migrate();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE batch_job_instance (job_instance_id BIGINT PRIMARY KEY)");
        }
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", "legacy_batch");

        assertThatThrownBy(() -> new MigrationExecutor().execute(
                MigrationContext.require("auth"),
                MigrationAction.MIGRATE,
                configuration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("incompatible shape");
    }

    @ParameterizedTest
    @MethodSource("malformedBatchMetadataMutations")
    void validateRejectsMalformedBatchMetadataDefinitions(
            String databaseName,
            String mutation,
            String expectedMessage) throws Exception {
        String url = "jdbc:h2:mem:" + databaseName
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", databaseName);
        MigrationExecutor executor = new MigrationExecutor();
        MigrationContext context = MigrationContext.require("auth");
        executor.execute(context, MigrationAction.MIGRATE, configuration);
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            for (String sql : mutation.split(";;")) {
                statement.execute(sql);
            }
        }

        assertThatThrownBy(() -> executor.execute(context, MigrationAction.VALIDATE, configuration))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedMessage);
    }

    @ParameterizedTest
    @MethodSource("postgresqlCompatibleContexts")
    void correctedMigrationsAndBatchMetadataMigrateAndValidateOnH2(
            String contextSlug,
            String expectedBusinessTable) throws Exception {
        String url = "jdbc:h2:mem:" + contextSlug
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", contextSlug);
        MigrationExecutor executor = new MigrationExecutor();
        MigrationContext context = MigrationContext.require(contextSlug);

        int migrationCount = executor.execute(context, MigrationAction.MIGRATE, configuration);
        int validationCount = executor.execute(context, MigrationAction.VALIDATE, configuration);

        assertThat(migrationCount).isGreaterThanOrEqualTo(2);
        assertThat(validationCount).isZero();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(tableExists(statement, expectedBusinessTable)).isTrue();
            assertThat(tableExists(statement, "batch_job_instance")).isTrue();
            assertThat(tableExists(statement, "flyway_schema_history")).isTrue();
            assertThat(tableExists(statement, "flyway_schema_history_batch")).isTrue();
        }
    }

    private static Stream<Arguments> postgresqlCompatibleContexts() {
        return Stream.of(
                Arguments.of("asset-lease", "fixed_assets"),
                Arguments.of("auth", "auth_users"),
                Arguments.of("budget", "budget_plans"),
                Arguments.of("deposit", "deposit_accounts"),
                Arguments.of("loan", "loans"),
                Arguments.of("reporting", "rpt_snapshot_header"));
    }

    private static Stream<Arguments> malformedBatchMetadataMutations() {
        return Stream.of(
                Arguments.of(
                        "bad-batch-column",
                        "ALTER TABLE batch_job_instance ALTER COLUMN job_key VARCHAR(64)",
                        "incompatible definition"),
                Arguments.of(
                        "bad-batch-unique",
                        "ALTER TABLE batch_job_instance DROP CONSTRAINT job_inst_un;;"
                                + "ALTER TABLE batch_job_instance ADD UNIQUE (job_name);;"
                                + "ALTER TABLE batch_job_instance ADD UNIQUE (job_key)",
                        "unique key"),
                Arguments.of(
                        "bad-batch-sequence",
                        "ALTER SEQUENCE batch_job_seq INCREMENT BY 2",
                        "incompatible definition"));
    }

    private boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '" + tableName + "'")) {
            resultSet.next();
            return resultSet.getInt(1) == 1;
        }
    }
}
