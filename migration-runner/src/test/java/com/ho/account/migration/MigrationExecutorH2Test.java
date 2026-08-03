package com.ho.account.migration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
            assertThat(tableExists(statement, context.historyTable())).isTrue();
            assertThat(tableExists(statement, "flyway_schema_history_batch")).isTrue();
        }
    }

    @Test
    void assetLeaseBaselineContainsEveryJpaOwnedTable() throws Exception {
        String url = "jdbc:h2:mem:asset-lease-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", "asset_lease");

        new MigrationExecutor().execute(
                MigrationContext.require("asset-lease"),
                MigrationAction.MIGRATE,
                configuration);

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(Stream.of(
                            "fixed_assets",
                            "asset_histories",
                            "lease_contracts",
                            "right_of_use_assets",
                            "lease_liabilities",
                            "lease_payment_schedules")
                    .allMatch(table -> {
                        try {
                            return tableExists(statement, table);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    }))
                    .isTrue();
            assertNumericColumn(connection, "fixed_assets", "acquisition_cost", 19, 2);
            assertNumericColumn(connection, "lease_contracts", "monthly_payment", 19, 2);
            assertNumericColumn(connection, "lease_contracts", "discount_rate", 7, 4);
            assertNumericColumn(connection, "right_of_use_assets", "current_book_value", 19, 2);
            assertNumericColumn(connection, "lease_liabilities", "current_value", 19, 2);
            assertNumericColumn(connection, "lease_payment_schedules", "remaining_lease_liability", 19, 2);
            assertThat(isNullable(connection, "asset_histories", "asset_id")).isFalse();
            assertThat(isNullable(connection, "asset_histories", "history_type")).isFalse();
            assertThat(isNullable(connection, "asset_histories", "event_at")).isFalse();
            assertThat(isNullable(connection, "asset_histories", "audit_user")).isFalse();
            assertThat(isNullable(connection, "fixed_assets", "current_book_value")).isFalse();
            assertThat(isNullable(connection, "fixed_assets", "account_code")).isFalse();

            assertThat(hasIndex(connection, "fixed_assets", "status", false)).isTrue();
            assertThat(hasIndex(connection, "asset_histories", "asset_id", false)).isTrue();
            assertThat(hasIndex(connection, "lease_payment_schedules", "lease_contract_id", false)).isTrue();
            assertThat(hasIndex(connection, "right_of_use_assets", "lease_contract_id", true)).isTrue();
            assertThat(hasIndex(connection, "lease_liabilities", "lease_contract_id", true)).isTrue();

            assertThat(hasForeignKey(
                    connection, "asset_histories", "asset_id", "fixed_assets", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "right_of_use_assets", "lease_contract_id", "lease_contracts", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "lease_liabilities", "lease_contract_id", "lease_contracts", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "lease_payment_schedules", "lease_contract_id", "lease_contracts", "id")).isTrue();
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO lease_contracts (
                        contract_no, contract_name, start_date, end_date, monthly_payment,
                        payment_day, discount_rate, ifrs16_applicable, short_term_lease, low_value_lease)
                    VALUES ('INVALID-DAY', 'Invalid day', DATE '2026-01-01', DATE '2026-12-31',
                        100.00, 0, 10.0000, TRUE, FALSE, FALSE)
                    """))
                    .hasMessageContaining("ck_lease_contract_payment_day");
        }
    }

    @Test
    void journalLedgerBaselineContainsEveryJpaOwnedTableAndFinancialConstraint() throws Exception {
        String url = "jdbc:h2:mem:journal-ledger-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", "journal_ledger");

        new MigrationExecutor().execute(
                MigrationContext.require("journal-ledger"),
                MigrationAction.MIGRATE,
                configuration);

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(Stream.of(
                            "journal_entries", "journal_details", "journal_rules",
                            "journal_rule_conditions", "journal_rule_details",
                            "gl_entries", "sl_entries", "gl_balances", "sl_balances",
                            "unsettled_items")
                    .allMatch(table -> {
                        try {
                            return tableExists(statement, table);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    }))
                    .isTrue();
            assertNumericColumn(connection, "journal_entries", "exchange_rate", 19, 8);
            assertNumericColumn(connection, "journal_details", "amount", 19, 2);
            assertNumericColumn(connection, "journal_details", "base_amount", 19, 2);
            assertNumericColumn(connection, "gl_entries", "dr_amount", 19, 2);
            assertNumericColumn(connection, "sl_entries", "base_cr_amount", 19, 2);
            assertNumericColumn(connection, "gl_balances", "ending_balance", 19, 2);
            assertNumericColumn(connection, "sl_balances", "ending_balance", 19, 2);
            assertThat(hasForeignKey(
                    connection, "journal_details", "journal_entry_id", "journal_entries", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "gl_entries", "journal_detail_id", "journal_details", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "sl_entries", "journal_detail_id", "journal_details", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "unsettled_items", "journal_detail_id", "journal_details", "id")).isTrue();
            assertThat(indexColumns(connection, "journal_entries", "idx_journal_entry_lineage"))
                    .containsExactly("lineage_source_type", "lineage_source_id");
            assertThat(indexColumns(connection, "gl_balances", "idx_gl_balance_lookup"))
                    .containsExactly("account_code", "currency_code", "balance_date");
            assertThat(hasUniqueIndexOnColumns(connection, "journal_entries", List.of("slip_no"))).isTrue();
            assertThat(hasUniqueIndexOnColumns(connection, "unsettled_items", List.of("management_no"))).isTrue();
            assertThat(hasUniqueIndexOnColumns(
                    connection,
                    "gl_balances",
                    List.of("account_code", "currency_code", "balance_date", "period"))).isTrue();
            statement.executeUpdate("""
                    INSERT INTO sl_balances (
                        balance_date, beginning_balance, debit_amount, credit_amount,
                        ending_balance, currency_code, account_code, bp_code, dept_code, period)
                    VALUES (DATE '2026-08-04', 0.00, 100.00, 0.00,
                        100.00, 'KRW', '101000', NULL, NULL, '2026-08')
                    """);
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO sl_balances (
                        balance_date, beginning_balance, debit_amount, credit_amount,
                        ending_balance, currency_code, account_code, bp_code, dept_code, period)
                    VALUES (DATE '2026-08-04', 0.00, 100.00, 0.00,
                        100.00, 'KRW', '101000', NULL, NULL, '2026-08')
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
        }
    }

    @Test
    void journalLedgerPlaceholderHistoryUpgradesForwardToV11() throws Exception {
        String url = "jdbc:h2:mem:journal-ledger-v10-upgrade"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/journal-ledger")
                .target("10")
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE sl_balances (
                        id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
                        balance_date DATE NOT NULL,
                        beginning_balance NUMERIC(19, 2) NOT NULL,
                        debit_amount NUMERIC(19, 2) NOT NULL,
                        credit_amount NUMERIC(19, 2) NOT NULL,
                        ending_balance NUMERIC(19, 2) NOT NULL,
                        currency_code VARCHAR(3) NOT NULL,
                        account_code VARCHAR(50) NOT NULL,
                        bp_code VARCHAR(50),
                        dept_code VARCHAR(50),
                        period VARCHAR(255) NOT NULL,
                        created_at TIMESTAMP(6),
                        updated_at TIMESTAMP(6),
                        CONSTRAINT uk_sl_balance_key UNIQUE
                            (account_code, bp_code, dept_code, currency_code, balance_date, period)
                    )
                    """);
            statement.executeUpdate("""
                    INSERT INTO sl_balances (
                        balance_date, beginning_balance, debit_amount, credit_amount,
                        ending_balance, currency_code, account_code, bp_code, dept_code, period)
                    VALUES (DATE '2026-08-04', 0.00, 100.00, 0.00,
                        100.00, 'KRW', '101000', NULL, NULL, '2026-08')
                    """);
        }

        MigrationConfiguration configuration =
                new MigrationConfiguration(url, "sa", "", "journal_ledger");
        MigrationExecutor executor = new MigrationExecutor();
        assertThat(executor.execute(
                MigrationContext.require("journal-ledger"), MigrationAction.MIGRATE, configuration))
                .isGreaterThanOrEqualTo(1);
        assertThat(executor.execute(
                MigrationContext.require("journal-ledger"), MigrationAction.VALIDATE, configuration)).isZero();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(tableExists(statement, "journal_entries")).isTrue();
            assertThat(tableExists(statement, "batch_job_instance")).isTrue();
            assertThatThrownBy(() -> statement.executeUpdate("""
                    INSERT INTO sl_balances (
                        balance_date, beginning_balance, debit_amount, credit_amount,
                        ending_balance, currency_code, account_code, bp_code, dept_code, period)
                    VALUES (DATE '2026-08-04', 0.00, 100.00, 0.00,
                        100.00, 'KRW', '101000', NULL, NULL, '2026-08')
                    """))
                    .isInstanceOf(java.sql.SQLException.class);
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*) FROM flyway_schema_history
                    WHERE version = '11' AND success = TRUE
                    """)) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isOne();
            }
        }
    }

    @Test
    void masterDataBaselineContainsEveryJpaOwnedTableAndConstraint() throws Exception {
        String url = "jdbc:h2:mem:master-data-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", "master_data");

        new MigrationExecutor().execute(
                MigrationContext.require("master-data"),
                MigrationAction.MIGRATE,
                configuration);

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(Stream.of(
                            "account_subjects", "business_partners", "business_partner_accounts",
                            "currencies", "departments", "exchange_rates", "fiscal_periods",
                            "master_data_change_requests", "products", "tax_profiles")
                    .allMatch(table -> {
                        try {
                            return tableExists(statement, table);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    }))
                    .isTrue();
            assertNumericColumn(connection, "exchange_rates", "rate", 19, 8);
            assertNumericColumn(connection, "products", "price", 19, 4);
            assertNumericColumn(connection, "tax_profiles", "tax_rate", 7, 4);
            assertThat(hasForeignKey(
                    connection, "account_subjects", "parent_id", "account_subjects", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "departments", "parent_id", "departments", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection,
                    "business_partner_accounts",
                    "business_partner_id",
                    "business_partners",
                    "id")).isTrue();
            assertThat(indexColumns(connection, "account_subjects", "idx_account_code_valid"))
                    .containsExactly("code", "valid_from", "valid_to");
            assertThat(indexColumns(connection, "business_partners", "idx_bp_code_valid"))
                    .containsExactly("business_partner_code", "valid_from", "valid_to");
            assertThat(hasUniqueIndexOnColumns(
                    connection,
                    "exchange_rates",
                    List.of("from_currency_code", "to_currency_code", "effective_date"))).isTrue();
            assertThat(hasUniqueIndexOnColumns(
                    connection, "fiscal_periods", List.of("fiscal_year", "fiscal_period"))).isTrue();
            assertThat(hasUniqueIndexOnColumns(
                    connection, "master_data_change_requests", List.of("source_reference"))).isTrue();
        }
    }

    @Test
    void masterDataPublishedHistoryUpgradesForwardFromV5ToV6() throws Exception {
        String url = "jdbc:h2:mem:master-data-v5-upgrade"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/master-data")
                .target("5")
                .load()
                .migrate();

        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", "master_data");
        MigrationExecutor executor = new MigrationExecutor();
        assertThat(executor.execute(
                MigrationContext.require("master-data"), MigrationAction.MIGRATE, configuration))
                .isGreaterThanOrEqualTo(1);
        assertThat(executor.execute(
                MigrationContext.require("master-data"), MigrationAction.VALIDATE, configuration)).isZero();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(tableExists(statement, "account_subjects")).isTrue();
            assertThat(tableExists(statement, "master_data_change_requests")).isTrue();
            assertThat(tableExists(statement, "batch_job_instance")).isTrue();
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*) FROM flyway_schema_history
                    WHERE version = '6' AND success = TRUE
                    """)) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isOne();
            }
        }
    }

    @Test
    void closingBaselineContainsEveryJpaOwnedTableAndRelationship() throws Exception {
        String url = "jdbc:h2:mem:closing-parity"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        MigrationConfiguration configuration = new MigrationConfiguration(url, "sa", "", "closing");

        new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                configuration);

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            assertThat(Stream.of(
                            "closing_calendars",
                            "closing_tasks",
                            "closing_gates",
                            "closing_audit_logs",
                            "period_locks",
                            "reopen_approvals",
                            "valuation_batches",
                            "provision_batches",
                            "closing_adjustments",
                            "daily_closing_status")
                    .allMatch(table -> {
                        try {
                            return tableExists(statement, table);
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    }))
                    .isTrue();
            assertThat(isNullable(connection, "closing_calendars", "fiscal_year")).isFalse();
            assertThat(isNullable(connection, "closing_tasks", "calendar_id")).isFalse();
            assertThat(isNullable(connection, "closing_audit_logs", "action_user")).isFalse();
            assertThat(isNullable(connection, "valuation_batches", "run_date_time")).isFalse();
            assertThat(isNullable(connection, "daily_closing_status", "state")).isFalse();
            assertThat(hasIndex(connection, "closing_calendars", "fiscal_year", true)).isTrue();
            assertThat(hasIndex(connection, "closing_tasks", "calendar_id", false)).isTrue();
            assertThat(hasIndex(connection, "closing_gates", "calendar_id", false)).isTrue();
            assertThat(hasForeignKey(
                    connection, "closing_tasks", "calendar_id", "closing_calendars", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "closing_gates", "calendar_id", "closing_calendars", "id")).isTrue();
            assertThat(hasForeignKey(
                    connection, "closing_audit_logs", "calendar_id", "closing_calendars", "id")).isTrue();
        }
    }

    @Test
    void existingClosingV49ShapeIsBaselinedAndUpgradedWithoutReapplyingBaseline() throws Exception {
        String url = "jdbc:h2:mem:closing-legacy-upgrade"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/closing")
                .table("flyway_schema_history_closing")
                .target("49")
                .load()
                .migrate();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history_closing");
            statement.execute("DROP INDEX idx_closing_task_calendar_order");
            statement.executeUpdate("""
                    INSERT INTO daily_closing_status(date, is_closed, closed_by)
                    VALUES (DATE '2026-07-31', TRUE, 'legacy-operator')
                    """);
        }

        MigrationConfiguration configuration =
                new MigrationConfiguration(url, "sa", "", "closing");
        new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                configuration);
        new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.VALIDATE,
                configuration);

        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT state, created_by
                    FROM daily_closing_status
                    WHERE date = DATE '2026-07-31'
                    """)) {
                assertThat(resultSet.next()).isTrue();
                assertThat(resultSet.getString("state")).isEqualTo("CLOSED");
                assertThat(resultSet.getString("created_by")).isEqualTo("legacy-operator");
            }
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM flyway_schema_history_closing
                    WHERE version = '49' AND type = 'BASELINE'
                    """)) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isOne();
            }
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM flyway_schema_history_closing
                    WHERE version = '51' AND success = TRUE
                    """)) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isOne();
            }
            assertThat(indexColumns(
                    connection, "closing_tasks", "idx_closing_task_calendar_order"))
                    .containsExactly("calendar_id", "task_order");
            try (ResultSet resultSet = statement.executeQuery("""
                    SELECT COUNT(*)
                    FROM flyway_schema_history_closing
                    WHERE version = '50' AND success = TRUE
                    """)) {
                resultSet.next();
                assertThat(resultSet.getInt(1)).isOne();
            }
        }
    }

    @Test
    void incompleteClosingLegacyShapeIsRejectedBeforeAutomaticBaseline() throws Exception {
        String url = "jdbc:h2:mem:closing-incomplete-legacy"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE closing_calendars (
                        id BIGINT PRIMARY KEY,
                        fiscal_year VARCHAR(4) NOT NULL,
                        fiscal_period VARCHAR(20) NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        is_current_period BOOLEAN NOT NULL)
                    """);
        }

        assertThatThrownBy(() -> new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "closing")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("complete version 49 table set")
                .hasMessageContaining("refusing automatic baseline");
    }

    @Test
    void incompleteClosingShapeWithBaselineHistoryIsRejectedBeforeV50() throws Exception {
        String url = "jdbc:h2:mem:closing-incomplete-with-history"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    CREATE TABLE closing_calendars (
                        id BIGINT PRIMARY KEY,
                        fiscal_year VARCHAR(4) NOT NULL,
                        fiscal_period VARCHAR(20) NOT NULL,
                        status VARCHAR(30) NOT NULL,
                        is_current_period BOOLEAN NOT NULL)
                    """);
        }
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/closing")
                .table("flyway_schema_history_closing")
                .baselineVersion("49")
                .load()
                .baseline();

        assertThatThrownBy(() -> new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "closing")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("complete version 49 table set")
                .hasMessageContaining("refusing automatic baseline");
    }

    @Test
    void closingLegacyShapeMissingAnyJpaColumnIsRejected() throws Exception {
        String url = "jdbc:h2:mem:closing-missing-column"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/closing")
                .table("flyway_schema_history_closing")
                .target("49")
                .load()
                .migrate();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history_closing");
            statement.execute("ALTER TABLE closing_tasks DROP COLUMN description");
        }

        assertThatThrownBy(() -> new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "closing")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different column set for closing_tasks")
                .hasMessageContaining("description")
                .hasMessageContaining("refusing automatic baseline");
    }

    @Test
    void unrelatedNonEmptySchemaCannotBeMistakenForCleanClosingDatabase() throws Exception {
        String url = "jdbc:h2:mem:closing-unrelated-nonempty"
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE unrelated_context_table (id BIGINT PRIMARY KEY)");
        }

        assertThatThrownBy(() -> new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "closing")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("non-empty")
                .hasMessageContaining("no version 49 Closing tables")
                .hasMessageContaining("refusing automatic baseline");
    }

    @ParameterizedTest
    @MethodSource("invalidClosingLegacyContracts")
    void closingLegacyTypeNullabilityAndKeysMustMatchBeforeBaseline(
            String databaseName,
            String mutation,
            String expectedMessage) throws Exception {
        String url = "jdbc:h2:mem:" + databaseName
                + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        Flyway.configure()
                .dataSource(url, "sa", "")
                .locations("classpath:db/contexts/closing")
                .table("flyway_schema_history_closing")
                .target("49")
                .load()
                .migrate();
        try (Connection connection = DriverManager.getConnection(url, "sa", "");
             Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE flyway_schema_history_closing");
            for (String sql : mutation.split(";;")) {
                statement.execute(sql);
            }
        }

        assertThatThrownBy(() -> new MigrationExecutor().execute(
                MigrationContext.require("closing"),
                MigrationAction.MIGRATE,
                new MigrationConfiguration(url, "sa", "", "closing")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(expectedMessage)
                .hasMessageContaining("refusing automatic baseline");
    }

    private static Stream<Arguments> postgresqlCompatibleContexts() {
        return Stream.of(
                Arguments.of("asset-lease", "fixed_assets"),
                Arguments.of("auth", "auth_users"),
                Arguments.of("budget", "budget_plans"),
                Arguments.of("closing", "closing_calendars"),
                Arguments.of("deposit", "deposit_accounts"),
                Arguments.of("journal-ledger", "journal_entries"),
                Arguments.of("loan", "loans"),
                Arguments.of("master-data", "account_subjects"),
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

    private static Stream<Arguments> invalidClosingLegacyContracts() {
        return Stream.of(
                Arguments.of(
                        "closing-nullable-state",
                        "ALTER TABLE daily_closing_status ALTER COLUMN is_closed DROP NOT NULL",
                        "incompatible nullability for daily_closing_status.is_closed"),
                Arguments.of(
                        "closing-invalid-type",
                        "ALTER TABLE closing_tasks ALTER COLUMN description BIGINT",
                        "incompatible type for closing_tasks.description"),
                Arguments.of(
                        "closing-invalid-length",
                        "ALTER TABLE closing_tasks ALTER COLUMN name VARCHAR(1)",
                        "incompatible length for closing_tasks.name"),
                Arguments.of(
                        "closing-unexpected-column",
                        "ALTER TABLE closing_tasks ADD legacy_required VARCHAR(10) NOT NULL",
                        "different column set for closing_tasks"),
                Arguments.of(
                        "closing-non-identity-id",
                        "ALTER TABLE period_locks ALTER COLUMN id DROP IDENTITY",
                        "non-identity primary key for period_locks.id"),
                Arguments.of(
                        "closing-missing-primary-key",
                        "ALTER TABLE daily_closing_status DROP PRIMARY KEY",
                        "incompatible primary key for daily_closing_status"),
                Arguments.of(
                        "closing-missing-foreign-key",
                        "ALTER TABLE closing_tasks DROP CONSTRAINT fk_closing_task_calendar",
                        "missing the required foreign key closing_tasks.calendar_id"),
                Arguments.of(
                        "closing-missing-period-unique",
                        "ALTER TABLE closing_calendars DROP CONSTRAINT uk_closing_calendar_period",
                        "missing the required unique key on closing_calendars"),
                Arguments.of(
                        "closing-wrong-operational-index",
                        "DROP INDEX idx_closing_task_calendar_order;;"
                                + "CREATE INDEX idx_closing_task_calendar_order ON closing_tasks(name)",
                        "incompatible index idx_closing_task_calendar_order"));
    }

    private boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_name = '" + tableName + "'")) {
            resultSet.next();
            return resultSet.getInt(1) == 1;
        }
    }

    private void assertNumericColumn(
            Connection connection,
            String tableName,
            String columnName,
            int precision,
            int scale) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getColumns(
                null, null, tableName, columnName)) {
            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getInt("COLUMN_SIZE")).isEqualTo(precision);
            assertThat(resultSet.getInt("DECIMAL_DIGITS")).isEqualTo(scale);
        }
    }

    private boolean isNullable(
            Connection connection,
            String tableName,
            String columnName) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getColumns(
                null, null, tableName, columnName)) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getInt("NULLABLE") == DatabaseMetaData.columnNullable;
        }
    }

    private boolean hasIndex(
            Connection connection,
            String tableName,
            String columnName,
            boolean unique) throws Exception {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getIndexInfo(null, null, tableName, unique, false)) {
            while (resultSet.next()) {
                if (columnName.equals(resultSet.getString("COLUMN_NAME"))
                        && (!unique || !resultSet.getBoolean("NON_UNIQUE"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private boolean hasForeignKey(
            Connection connection,
            String tableName,
            String columnName,
            String referencedTable,
            String referencedColumn) throws Exception {
        try (ResultSet resultSet = connection.getMetaData().getImportedKeys(
                null, null, tableName)) {
            while (resultSet.next()) {
                if (columnName.equals(resultSet.getString("FKCOLUMN_NAME"))
                        && referencedTable.equals(resultSet.getString("PKTABLE_NAME"))
                        && referencedColumn.equals(resultSet.getString("PKCOLUMN_NAME"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private List<String> indexColumns(
            Connection connection,
            String tableName,
            String indexName) throws Exception {
        List<String> columns = new ArrayList<>();
        try (ResultSet resultSet = connection.getMetaData().getIndexInfo(
                null, null, tableName, false, false)) {
            while (resultSet.next()) {
                if (indexName.equals(resultSet.getString("INDEX_NAME"))
                        && resultSet.getString("COLUMN_NAME") != null) {
                    int position = resultSet.getInt("ORDINAL_POSITION");
                    while (columns.size() < position) {
                        columns.add(null);
                    }
                    columns.set(position - 1, resultSet.getString("COLUMN_NAME"));
                }
            }
        }
        return columns.stream().filter(java.util.Objects::nonNull).toList();
    }

    private boolean hasUniqueIndexOnColumns(
            Connection connection,
            String tableName,
            List<String> expectedColumns) throws Exception {
        Map<String, List<String>> indexes = new LinkedHashMap<>();
        try (ResultSet resultSet = connection.getMetaData().getIndexInfo(
                null, null, tableName, true, false)) {
            while (resultSet.next()) {
                String indexName = resultSet.getString("INDEX_NAME");
                String columnName = resultSet.getString("COLUMN_NAME");
                if (indexName == null || columnName == null || resultSet.getBoolean("NON_UNIQUE")) {
                    continue;
                }
                List<String> columns = indexes.computeIfAbsent(indexName, ignored -> new ArrayList<>());
                int position = resultSet.getInt("ORDINAL_POSITION");
                while (columns.size() < position) {
                    columns.add(null);
                }
                columns.set(position - 1, columnName);
            }
        }
        return indexes.values().stream()
                .map(columns -> columns.stream().filter(java.util.Objects::nonNull).toList())
                .anyMatch(expectedColumns::equals);
    }
}
