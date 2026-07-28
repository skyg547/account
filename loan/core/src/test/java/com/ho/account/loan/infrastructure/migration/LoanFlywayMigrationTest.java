package com.ho.account.loan.infrastructure.migration;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

class LoanFlywayMigrationTest {

    @Test
    void loanMigrationCreatesConsolidatedLoanSchema() throws Exception {
        String jdbcUrl = "jdbc:h2:mem:loan_flyway;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";

        Flyway.configure()
                .dataSource(jdbcUrl, "sa", "")
                .locations("filesystem:" + migrationDirectory().toAbsolutePath())
                .load()
                .migrate();

        try (Connection connection = DriverManager.getConnection(jdbcUrl, "sa", "");
                Statement statement = connection.createStatement()) {
            assertThat(tableExists(statement, "loans")).isTrue();
            assertThat(tableExists(statement, "loan_amortization_schedule_entries")).isTrue();
            assertThat(tableExists(statement, "loan_accrual_log")).isTrue();
            assertThat(columnExists(statement, "loans", "loan_number")).isTrue();
            assertThat(columnExists(statement, "loans", "current_eir")).isTrue();
            assertThat(columnExists(statement, "loans", "lock_version")).isTrue();
            assertThat(columnExists(statement, "deferred_item_types", "deferred_asset_account_ref")).isTrue();
            assertThat(columnExists(statement, "deferred_item_types", "recognized_income_account_ref")).isTrue();
            assertThat(columnExists(statement, "loan_amortization_schedule_entries", "loan_id")).isTrue();
            assertThat(columnExists(statement, "loan_accrual_log", "loan_id")).isTrue();
            assertThat(columnExists(statement, "loan_accrual_log", "journal_entry_id")).isTrue();
            assertThat(columnExists(statement, "eir_amortization_schedules", "amortization_journal_entry_slip_no")).isTrue();
            assertThat(columnExists(statement, "loan_events", "journal_entry_slip_no")).isTrue();
            assertThat(indexExists(statement, "uq_loan_disbursal_loan")).isTrue();
            assertThat(indexExists(statement, "uq_eir_schedule_loan_date")).isTrue();
            assertThat(indexExists(statement, "uq_loan_accrual_log_loan_date")).isTrue();
            assertThat(indexExists(statement, "ix_loans_status_id")).isTrue();
            assertThat(tableExists(statement, "loan_contracts")).isFalse();
        }
    }

    private Path migrationDirectory() {
        Path moduleRelative = Paths.get("src/main/resources/db/migration");
        if (Files.isDirectory(moduleRelative)) {
            return moduleRelative;
        }
        return Paths.get("loan/core/src/main/resources/db/migration");
    }

    private boolean tableExists(Statement statement, String tableName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "select count(*) from information_schema.tables where table_name = '" + tableName + "'")) {
            resultSet.next();
            return resultSet.getInt(1) > 0;
        }
    }

    private boolean columnExists(Statement statement, String tableName, String columnName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "select count(*) from information_schema.columns where table_name = '" + tableName
                        + "' and column_name = '" + columnName + "'")) {
            resultSet.next();
            return resultSet.getInt(1) > 0;
        }
    }

    private boolean indexExists(Statement statement, String indexName) throws Exception {
        try (ResultSet resultSet = statement.executeQuery(
                "select count(*) from information_schema.indexes where index_name = '" + indexName + "'")) {
            resultSet.next();
            return resultSet.getInt(1) > 0;
        }
    }
}
