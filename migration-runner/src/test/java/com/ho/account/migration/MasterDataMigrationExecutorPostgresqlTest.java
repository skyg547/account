package com.ho.account.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Executes the packaged, uncapped Master Data history on a disposable local PostgreSQL. */
class MasterDataMigrationExecutorPostgresqlTest {

    private static final MigrationContext CONTEXT = MigrationContext.require("master-data");
    private static EmbeddedPostgres postgres;

    @BeforeAll
    static void startPostgres() throws Exception {
        // No external URL, environment credentials or conditional skip can substitute this engine.
        postgres = EmbeddedPostgres.builder().start();
    }

    @AfterAll
    static void stopPostgres() throws Exception {
        if (postgres != null) {
            postgres.close();
        }
    }

    @Test
    void currentPackagedHistoryAndBatchMetadataMigrateValidateAndRepeat() throws Exception {
        MigrationConfiguration configuration = freshDatabase("master_data_current");
        MigrationExecutor executor = new MigrationExecutor();

        assertThat(executor.execute(CONTEXT, MigrationAction.MIGRATE, configuration))
                .isGreaterThanOrEqualTo(2);
        assertThat(executor.execute(CONTEXT, MigrationAction.VALIDATE, configuration)).isZero();
        assertThat(executor.execute(CONTEXT, MigrationAction.MIGRATE, configuration)).isZero();
        assertLatestHistory(configuration);
        try (Connection connection = connect(configuration)) {
            assertTables(connection, "account_subjects", "batch_job_instance",
                    CONTEXT.historyTable(), "flyway_schema_history_batch");
        }
    }

    @Test
    void masterDataBaselineContainsEveryJpaOwnedTableAndConstraint() throws Exception {
        MigrationConfiguration configuration = freshDatabase("master_data_parity");
        new MigrationExecutor().execute(CONTEXT, MigrationAction.MIGRATE, configuration);

        try (Connection connection = connect(configuration)) {
            assertTables(connection, "account_subjects", "business_partners", "business_partner_accounts",
                    "currencies", "departments", "exchange_rates", "fiscal_periods",
                    "master_data_change_requests", "products", "tax_profiles");
            assertNumericColumn(connection, "exchange_rates", "rate", 19, 8);
            assertNumericColumn(connection, "products", "price", 19, 4);
            assertNumericColumn(connection, "tax_profiles", "tax_rate", 7, 4);
            assertThat(hasForeignKey(connection, "account_subjects", "parent_id", "account_subjects", "id"))
                    .isTrue();
            assertThat(hasForeignKey(connection, "departments", "parent_id", "departments", "id"))
                    .isTrue();
            assertThat(hasForeignKey(connection, "business_partner_accounts", "business_partner_id",
                    "business_partners", "id")).isTrue();
            assertThat(indexColumns(connection, "account_subjects", "idx_account_code_valid", false))
                    .containsExactly("code", "valid_from", "valid_to");
            assertThat(indexColumns(connection, "business_partners", "idx_bp_code_valid", false))
                    .containsExactly("business_partner_code", "valid_from", "valid_to");
            assertThat(hasUniqueIndex(connection, "exchange_rates",
                    List.of("from_currency_code", "to_currency_code", "effective_date"))).isTrue();
            assertThat(hasUniqueIndex(connection, "fiscal_periods", List.of("fiscal_year", "fiscal_period")))
                    .isTrue();
            assertThat(hasUniqueIndex(connection, "master_data_change_requests", List.of("source_reference")))
                    .isTrue();
        }
    }

    @Test
    void publishedV5HistoryUpgradesThroughV6ToTheLatestPackagedVersion() throws Exception {
        MigrationConfiguration configuration = freshDatabase("master_data_v5_upgrade");
        contextFlyway(configuration).target("5").load().migrate();

        MigrationExecutor executor = new MigrationExecutor();
        assertThat(executor.execute(CONTEXT, MigrationAction.MIGRATE, configuration))
                .isGreaterThanOrEqualTo(1);
        assertThat(executor.execute(CONTEXT, MigrationAction.VALIDATE, configuration)).isZero();
        assertThat(executor.execute(CONTEXT, MigrationAction.MIGRATE, configuration)).isZero();
        assertLatestHistory(configuration);
        try (Connection connection = connect(configuration);
             Statement statement = connection.createStatement()) {
            assertTables(connection, "account_subjects", "master_data_change_requests", "batch_job_instance");
            try (ResultSet rows = statement.executeQuery("""
                    SELECT COUNT(*) FROM flyway_schema_history WHERE version = '6' AND success = TRUE
                    """)) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isOne();
            }
        }
    }

    @Test
    void fullHistoryEnforcesPostgresqlRangeAndFinancialConstraints() throws Exception {
        MigrationConfiguration configuration = freshDatabase("master_data_constraints");
        new MigrationExecutor().execute(CONTEXT, MigrationAction.MIGRATE, configuration);
        try (Connection connection = connect(configuration);
             Statement statement = connection.createStatement()) {
            statement.execute(productInsert("P-946", "2026-01-01", "2026-01-31", "1.0000"));
            // Inclusive SCD2 boundaries may touch neither the same day nor a nested range.
            assertThatThrownBy(() -> statement.execute(
                    productInsert("P-946", "2026-01-31", "2026-02-28", "1.0000")))
                    .isInstanceOf(SQLException.class).extracting("SQLState").isEqualTo("23P01");
            statement.execute(productInsert("P-946", "2026-02-01", "2026-02-28", "0.0000"));
            assertThatThrownBy(() -> statement.execute(
                    productInsert("NEGATIVE-946", "2026-01-01", "2026-01-31", "-0.0001")))
                    .isInstanceOf(SQLException.class).extracting("SQLState").isEqualTo("23514");
            try (ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM products")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(2);
            }
        }
    }

    private MigrationConfiguration freshDatabase(String name) throws Exception {
        // Fixed test-owned identifiers; each test gets an empty database and independent histories.
        try (Connection connection = postgres.getPostgresDatabase().getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE DATABASE " + name);
        }
        return new MigrationConfiguration(postgres.getJdbcUrl("postgres", name), "postgres", "", name);
    }

    private Connection connect(MigrationConfiguration configuration) throws SQLException {
        return DriverManager.getConnection(configuration.url(), configuration.user(), configuration.password());
    }

    private org.flywaydb.core.api.configuration.FluentConfiguration contextFlyway(
            MigrationConfiguration configuration) {
        return Flyway.configure().dataSource(configuration.url(), configuration.user(), configuration.password())
                .locations(CONTEXT.location()).table(CONTEXT.historyTable()).cleanDisabled(true)
                .validateMigrationNaming(true);
    }

    private void assertLatestHistory(MigrationConfiguration configuration) {
        Flyway flyway = contextFlyway(configuration).load();
        flyway.validate();
        assertThat(flyway.info().pending()).isEmpty();
        // Do not cap the target: newly packaged versions must also execute in this regression.
        assertThat(flyway.info().current().getVersion().isAtLeast("13")).isTrue();
    }

    private void assertTables(Connection connection, String... names) throws Exception {
        for (String name : names) {
            try (ResultSet rows = connection.getMetaData().getTables(null, "public", name, null)) {
                assertThat(rows.next()).as("table %s", name).isTrue();
            }
        }
    }

    private void assertNumericColumn(Connection connection, String table, String column,
                                     int precision, int scale) throws Exception {
        try (ResultSet rows = connection.getMetaData().getColumns(null, "public", table, column)) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt("COLUMN_SIZE")).isEqualTo(precision);
            assertThat(rows.getInt("DECIMAL_DIGITS")).isEqualTo(scale);
        }
    }

    private boolean hasForeignKey(Connection connection, String table, String column,
                                  String targetTable, String targetColumn) throws Exception {
        try (ResultSet rows = connection.getMetaData().getImportedKeys(null, "public", table)) {
            while (rows.next()) {
                if (column.equals(rows.getString("FKCOLUMN_NAME"))
                        && targetTable.equals(rows.getString("PKTABLE_NAME"))
                        && targetColumn.equals(rows.getString("PKCOLUMN_NAME"))) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> indexColumns(Connection connection, String table, String index,
                                      boolean unique) throws Exception {
        return indexes(connection, table, unique).getOrDefault(index, List.of());
    }

    private boolean hasUniqueIndex(Connection connection, String table, List<String> expected) throws Exception {
        return indexes(connection, table, true).containsValue(expected);
    }

    private Map<String, List<String>> indexes(Connection connection, String table, boolean unique) throws Exception {
        Map<String, List<String>> indexes = new LinkedHashMap<>();
        try (ResultSet rows = connection.getMetaData().getIndexInfo(null, "public", table, unique, false)) {
            while (rows.next()) {
                String name = rows.getString("INDEX_NAME");
                String column = rows.getString("COLUMN_NAME");
                if (name == null || column == null || (unique && rows.getBoolean("NON_UNIQUE"))) {
                    continue;
                }
                List<String> columns = indexes.computeIfAbsent(name, ignored -> new ArrayList<>());
                int position = rows.getInt("ORDINAL_POSITION");
                while (columns.size() < position) {
                    columns.add(null);
                }
                columns.set(position - 1, column);
            }
        }
        return indexes;
    }

    private String productInsert(String code, String from, String to, String price) {
        // All literals are fixed synthetic test inputs, never caller or environment values.
        return "INSERT INTO products (price, valid_from, valid_to, created_at, updated_at, product_code, "
                + "product_type, name) VALUES (" + price + ", DATE '" + from + "', DATE '" + to
                + "', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, '" + code + "', 'PHYSICAL', 'Synthetic 946')";
    }
}
