package com.ho.account.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

final class LegacyBaselineVerifier {

    private static final Map<String, Set<String>> CLOSING_V49_SHAPE = closingV49Shape();
    private static final Set<String> BIGINT_COLUMNS = Set.of(
            "closing_calendars.id",
            "closing_tasks.id", "closing_tasks.calendar_id",
            "closing_gates.id", "closing_gates.calendar_id",
            "closing_audit_logs.id", "closing_audit_logs.calendar_id",
            "period_locks.id", "period_locks.fiscal_period_id",
            "reopen_approvals.id", "reopen_approvals.fiscal_period_id",
            "valuation_batches.id", "valuation_batches.fiscal_period_id",
            "valuation_batches.generated_journal_entry_id",
            "provision_batches.id", "provision_batches.fiscal_period_id",
            "provision_batches.generated_journal_entry_id",
            "closing_adjustments.id", "closing_adjustments.fiscal_period_id",
            "closing_adjustments.journal_entry_id");
    private static final Set<String> INTEGER_COLUMNS = Set.of("closing_tasks.task_order");
    private static final Set<String> BOOLEAN_COLUMNS = Set.of(
            "closing_calendars.is_current_period",
            "closing_tasks.is_mandatory",
            "daily_closing_status.is_closed");
    private static final Set<String> DATE_COLUMNS = Set.of("daily_closing_status.date");
    private static final Set<String> TIMESTAMP_COLUMNS = Set.of(
            "closing_calendars.close_initiated_at", "closing_calendars.closed_at",
            "closing_calendars.reopened_at", "closing_calendars.created_at",
            "closing_calendars.updated_at",
            "closing_tasks.due_date", "closing_tasks.created_at", "closing_tasks.updated_at",
            "closing_gates.passed_at", "closing_gates.created_at", "closing_gates.updated_at",
            "closing_audit_logs.action_at",
            "period_locks.locked_at", "period_locks.created_at", "period_locks.updated_at",
            "reopen_approvals.requested_at", "reopen_approvals.approved_at",
            "reopen_approvals.created_at", "reopen_approvals.updated_at",
            "valuation_batches.run_date_time", "valuation_batches.created_at",
            "valuation_batches.updated_at",
            "provision_batches.run_date_time", "provision_batches.created_at",
            "provision_batches.updated_at",
            "closing_adjustments.approved_at", "closing_adjustments.created_at",
            "closing_adjustments.updated_at",
            "daily_closing_status.closed_at");
    private static final Set<String> NON_NULL_COLUMNS = Set.of(
            "closing_calendars.id", "closing_calendars.fiscal_year",
            "closing_calendars.fiscal_period", "closing_calendars.status",
            "closing_calendars.is_current_period",
            "closing_tasks.id", "closing_tasks.calendar_id", "closing_tasks.name",
            "closing_tasks.status", "closing_tasks.is_mandatory", "closing_tasks.task_order",
            "closing_gates.id", "closing_gates.calendar_id", "closing_gates.name",
            "closing_gates.status",
            "closing_audit_logs.id", "closing_audit_logs.calendar_id",
            "closing_audit_logs.action_type", "closing_audit_logs.current_status",
            "closing_audit_logs.action_user", "closing_audit_logs.action_at",
            "period_locks.id", "period_locks.fiscal_period_id", "period_locks.lock_type",
            "reopen_approvals.id", "reopen_approvals.fiscal_period_id",
            "reopen_approvals.status",
            "valuation_batches.id", "valuation_batches.fiscal_period_id",
            "valuation_batches.valuation_type", "valuation_batches.run_date_time",
            "valuation_batches.status",
            "provision_batches.id", "provision_batches.fiscal_period_id",
            "provision_batches.provision_type", "provision_batches.run_date_time",
            "provision_batches.status",
            "closing_adjustments.id", "closing_adjustments.fiscal_period_id",
            "closing_adjustments.journal_entry_id", "closing_adjustments.adjustment_type",
            "daily_closing_status.date", "daily_closing_status.is_closed");
    private static final Map<String, Integer> STRING_LENGTHS = stringLengths();
    private static final Map<String, List<String>> OPERATIONAL_INDEXES = operationalIndexes();

    void verify(MigrationContext context, MigrationConfiguration configuration) {
        if (context.legacyBaselineVersion() == null) {
            return;
        }
        try (Connection connection = DriverManager.getConnection(
                configuration.url(), configuration.user(), configuration.password())) {
            boolean historyExists = tableExists(connection, context.historyTable());
            if (historyExists && migrationApplied(connection, context.historyTable(), "50")) {
                return;
            }
            long presentTables = CLOSING_V49_SHAPE.keySet().stream()
                    .filter(table -> tableExistsUnchecked(connection, table))
                    .count();
            if (presentTables == 0) {
                if (historyExists) {
                    throw incompatible(
                            "has a history table but no version 49 business tables");
                }
                if (hasAnyUserTable(connection)) {
                    throw incompatible(
                            "is non-empty but contains no version 49 Closing tables");
                }
                return;
            }
            if (presentTables != CLOSING_V49_SHAPE.size()) {
                throw incompatible("does not contain the complete version 49 table set");
            }
            for (Map.Entry<String, Set<String>> table : CLOSING_V49_SHAPE.entrySet()) {
                verifyExactColumns(connection, table.getKey(), table.getValue());
                for (String column : table.getValue()) {
                    verifyColumn(connection, table.getKey(), column);
                }
                String primaryKey = "daily_closing_status".equals(table.getKey()) ? "date" : "id";
                verifyPrimaryKey(connection, table.getKey(), primaryKey);
            }
            verifyForeignKey(connection, "closing_tasks", "calendar_id", "closing_calendars", "id");
            verifyForeignKey(connection, "closing_gates", "calendar_id", "closing_calendars", "id");
            verifyForeignKey(
                    connection, "closing_audit_logs", "calendar_id", "closing_calendars", "id");
            verifyUniqueColumns(
                    connection, "closing_calendars", Set.of("fiscal_year", "fiscal_period"));
            verifyExistingOperationalIndexes(connection);
            if (columnExists(connection, "daily_closing_status", "state")) {
                throw incompatible("already contains the version 50 state column without history");
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not verify the legacy baseline shape", exception);
        }
    }

    private boolean tableExistsUnchecked(Connection connection, String table) {
        try {
            return tableExists(connection, table);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not inspect legacy table " + table, exception);
        }
    }

    private boolean tableExists(Connection connection, String table) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet resultSet = metadata.getTables(
                null, connection.getSchema(), table, new String[] {"TABLE"})) {
            return resultSet.next();
        }
    }

    private boolean hasAnyUserTable(Connection connection) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getTables(
                null, connection.getSchema(), "%", new String[] {"TABLE"})) {
            return resultSet.next();
        }
    }

    private boolean columnExists(Connection connection, String table, String column)
            throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getColumns(
                null, connection.getSchema(), table, column)) {
            return resultSet.next();
        }
    }

    private void verifyColumn(Connection connection, String table, String column)
            throws SQLException {
        String qualifiedColumn = table + "." + column;
        try (ResultSet resultSet = connection.getMetaData().getColumns(
                null, connection.getSchema(), table, column)) {
            if (!resultSet.next()) {
                throw incompatible("is missing required column " + qualifiedColumn);
            }
            int jdbcType = resultSet.getInt("DATA_TYPE");
            if (!expectedKind(qualifiedColumn).matches(jdbcType)) {
                throw incompatible("has an incompatible type for " + qualifiedColumn);
            }
            Integer expectedLength = STRING_LENGTHS.get(qualifiedColumn);
            if (expectedLength != null && resultSet.getInt("COLUMN_SIZE") != expectedLength) {
                throw incompatible("has an incompatible length for " + qualifiedColumn);
            }
            boolean nullable = resultSet.getInt("NULLABLE") != DatabaseMetaData.columnNoNulls;
            boolean expectedNullable = !NON_NULL_COLUMNS.contains(qualifiedColumn);
            if (nullable != expectedNullable) {
                throw incompatible("has incompatible nullability for " + qualifiedColumn);
            }
            if ("id".equals(column)
                    && !"YES".equalsIgnoreCase(resultSet.getString("IS_AUTOINCREMENT"))) {
                throw incompatible("has a non-identity primary key for " + qualifiedColumn);
            }
        }
    }

    private void verifyExactColumns(
            Connection connection,
            String table,
            Set<String> expectedColumns) throws SQLException {
        Set<String> actualColumns = new HashSet<>();
        try (ResultSet resultSet = connection.getMetaData().getColumns(
                null, connection.getSchema(), table, "%")) {
            while (resultSet.next()) {
                actualColumns.add(resultSet.getString("COLUMN_NAME"));
            }
        }
        if (!actualColumns.equals(expectedColumns)) {
            Set<String> unexpected = new HashSet<>(actualColumns);
            unexpected.removeAll(expectedColumns);
            Set<String> missing = new HashSet<>(expectedColumns);
            missing.removeAll(actualColumns);
            throw incompatible(
                    "has a different column set for " + table
                            + " (unexpected=" + unexpected + ", missing=" + missing + ")");
        }
    }

    private void verifyPrimaryKey(Connection connection, String table, String expectedColumn)
            throws SQLException {
        Set<String> columns = new HashSet<>();
        try (ResultSet resultSet = connection.getMetaData().getPrimaryKeys(
                null, connection.getSchema(), table)) {
            while (resultSet.next()) {
                columns.add(resultSet.getString("COLUMN_NAME"));
            }
        }
        if (!columns.equals(Set.of(expectedColumn))) {
            throw incompatible("has an incompatible primary key for " + table);
        }
    }

    private void verifyForeignKey(
            Connection connection,
            String table,
            String column,
            String referencedTable,
            String referencedColumn) throws SQLException {
        try (ResultSet resultSet = connection.getMetaData().getImportedKeys(
                null, connection.getSchema(), table)) {
            while (resultSet.next()) {
                if (column.equals(resultSet.getString("FKCOLUMN_NAME"))
                        && referencedTable.equals(resultSet.getString("PKTABLE_NAME"))
                        && referencedColumn.equals(resultSet.getString("PKCOLUMN_NAME"))) {
                    return;
                }
            }
        }
        throw incompatible("is missing the required foreign key " + table + "." + column);
    }

    private void verifyUniqueColumns(
            Connection connection,
            String table,
            Set<String> expectedColumns) throws SQLException {
        Map<String, Set<String>> indexes = new LinkedHashMap<>();
        try (ResultSet resultSet = connection.getMetaData().getIndexInfo(
                null, connection.getSchema(), table, true, false)) {
            while (resultSet.next()) {
                String indexName = resultSet.getString("INDEX_NAME");
                String columnName = resultSet.getString("COLUMN_NAME");
                if (indexName != null && columnName != null) {
                    indexes.computeIfAbsent(indexName, ignored -> new HashSet<>()).add(columnName);
                }
            }
        }
        if (indexes.values().stream().noneMatch(expectedColumns::equals)) {
            throw incompatible("is missing the required unique key on " + table);
        }
    }

    private void verifyExistingOperationalIndexes(Connection connection) throws SQLException {
        for (Map.Entry<String, List<String>> expected : OPERATIONAL_INDEXES.entrySet()) {
            String[] key = expected.getKey().split("\\.", 2);
            List<String> actual = indexColumns(connection, key[0], key[1]);
            if (!actual.isEmpty() && !actual.equals(expected.getValue())) {
                throw incompatible("has an incompatible index " + key[1]);
            }
        }
    }

    private List<String> indexColumns(Connection connection, String table, String indexName)
            throws SQLException {
        Map<Short, String> orderedColumns = new TreeMap<>();
        try (ResultSet resultSet = connection.getMetaData().getIndexInfo(
                null, connection.getSchema(), table, false, false)) {
            while (resultSet.next()) {
                if (indexName.equals(resultSet.getString("INDEX_NAME"))
                        && resultSet.getString("COLUMN_NAME") != null) {
                    orderedColumns.put(
                            resultSet.getShort("ORDINAL_POSITION"),
                            resultSet.getString("COLUMN_NAME"));
                }
            }
        }
        return List.copyOf(orderedColumns.values());
    }

    private ColumnKind expectedKind(String qualifiedColumn) {
        if (BIGINT_COLUMNS.contains(qualifiedColumn)) {
            return ColumnKind.BIGINT;
        }
        if (INTEGER_COLUMNS.contains(qualifiedColumn)) {
            return ColumnKind.INTEGER;
        }
        if (BOOLEAN_COLUMNS.contains(qualifiedColumn)) {
            return ColumnKind.BOOLEAN;
        }
        if (DATE_COLUMNS.contains(qualifiedColumn)) {
            return ColumnKind.DATE;
        }
        if (TIMESTAMP_COLUMNS.contains(qualifiedColumn)) {
            return ColumnKind.TIMESTAMP;
        }
        return ColumnKind.STRING;
    }

    private boolean migrationApplied(Connection connection, String historyTable, String version)
            throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(
                     "SELECT COUNT(*) FROM " + historyTable
                             + " WHERE version = '" + version + "' AND success = TRUE")) {
            resultSet.next();
            return resultSet.getInt(1) == 1;
        }
    }

    private IllegalStateException incompatible(String detail) {
        return new IllegalStateException(
                "Existing Closing schema " + detail
                        + "; refusing automatic baseline at version 49");
    }

    private static Map<String, Set<String>> closingV49Shape() {
        Map<String, Set<String>> shape = new LinkedHashMap<>();
        shape.put("closing_calendars", Set.of(
                "id", "fiscal_year", "fiscal_period", "status",
                "close_initiated_by", "close_initiated_at", "closed_by", "closed_at",
                "reopened_by", "reopened_at", "is_current_period",
                "created_at", "updated_at", "audit_user"));
        shape.put("closing_tasks", Set.of(
                "id", "calendar_id", "name", "description", "category", "due_date",
                "assigned_to", "status", "completion_condition_json", "is_mandatory",
                "task_order", "created_at", "updated_at", "audit_user"));
        shape.put("closing_gates", Set.of(
                "id", "calendar_id", "name", "description", "status",
                "check_condition_json", "passed_by", "passed_at",
                "created_at", "updated_at", "audit_user"));
        shape.put("closing_audit_logs", Set.of(
                "id", "calendar_id", "action_type", "previous_status", "current_status",
                "action_reason", "action_user", "action_at", "detail_info"));
        shape.put("period_locks", Set.of(
                "id", "fiscal_period_id", "lock_type", "locked_by", "locked_at", "reason",
                "created_at", "updated_at", "audit_user"));
        shape.put("reopen_approvals", Set.of(
                "id", "fiscal_period_id", "requested_by", "requested_at", "reason", "status",
                "approved_by", "approved_at", "impact_analysis_report",
                "created_at", "updated_at", "audit_user"));
        shape.put("valuation_batches", Set.of(
                "id", "fiscal_period_id", "valuation_type", "run_date_time", "status",
                "generated_journal_entry_id", "report_link", "run_by",
                "created_at", "updated_at", "audit_user"));
        shape.put("provision_batches", Set.of(
                "id", "fiscal_period_id", "provision_type", "run_date_time", "status",
                "generated_journal_entry_id", "report_link", "run_by",
                "created_at", "updated_at", "audit_user"));
        shape.put("closing_adjustments", Set.of(
                "id", "fiscal_period_id", "journal_entry_id", "adjustment_type", "description",
                "approved_by", "approved_at", "created_at", "updated_at", "audit_user"));
        shape.put("daily_closing_status", Set.of(
                "date", "is_closed", "closed_at", "closed_by"));
        return Map.copyOf(shape);
    }

    private static Map<String, Integer> stringLengths() {
        return Map.ofEntries(
                Map.entry("closing_calendars.fiscal_year", 4),
                Map.entry("closing_calendars.fiscal_period", 20),
                Map.entry("closing_calendars.status", 30),
                Map.entry("closing_calendars.close_initiated_by", 50),
                Map.entry("closing_calendars.closed_by", 50),
                Map.entry("closing_calendars.reopened_by", 50),
                Map.entry("closing_calendars.audit_user", 50),
                Map.entry("closing_tasks.name", 200),
                Map.entry("closing_tasks.description", 1000),
                Map.entry("closing_tasks.category", 50),
                Map.entry("closing_tasks.assigned_to", 50),
                Map.entry("closing_tasks.status", 30),
                Map.entry("closing_tasks.audit_user", 50),
                Map.entry("closing_gates.name", 100),
                Map.entry("closing_gates.description", 500),
                Map.entry("closing_gates.status", 30),
                Map.entry("closing_gates.passed_by", 50),
                Map.entry("closing_gates.audit_user", 50),
                Map.entry("closing_audit_logs.action_type", 50),
                Map.entry("closing_audit_logs.previous_status", 50),
                Map.entry("closing_audit_logs.current_status", 50),
                Map.entry("closing_audit_logs.action_reason", 1000),
                Map.entry("closing_audit_logs.action_user", 50),
                Map.entry("period_locks.lock_type", 50),
                Map.entry("period_locks.locked_by", 50),
                Map.entry("period_locks.reason", 1000),
                Map.entry("period_locks.audit_user", 50),
                Map.entry("reopen_approvals.requested_by", 50),
                Map.entry("reopen_approvals.reason", 1000),
                Map.entry("reopen_approvals.status", 30),
                Map.entry("reopen_approvals.approved_by", 50),
                Map.entry("reopen_approvals.audit_user", 50),
                Map.entry("valuation_batches.valuation_type", 50),
                Map.entry("valuation_batches.status", 30),
                Map.entry("valuation_batches.report_link", 200),
                Map.entry("valuation_batches.run_by", 50),
                Map.entry("valuation_batches.audit_user", 50),
                Map.entry("provision_batches.provision_type", 50),
                Map.entry("provision_batches.status", 30),
                Map.entry("provision_batches.report_link", 200),
                Map.entry("provision_batches.run_by", 50),
                Map.entry("provision_batches.audit_user", 50),
                Map.entry("closing_adjustments.adjustment_type", 50),
                Map.entry("closing_adjustments.description", 1000),
                Map.entry("closing_adjustments.approved_by", 50),
                Map.entry("closing_adjustments.audit_user", 50),
                Map.entry("daily_closing_status.closed_by", 80));
    }

    private static Map<String, List<String>> operationalIndexes() {
        return Map.ofEntries(
                Map.entry("closing_tasks.idx_closing_task_calendar_order",
                        List.of("calendar_id", "task_order")),
                Map.entry("closing_gates.idx_closing_gate_calendar", List.of("calendar_id")),
                Map.entry("closing_audit_logs.idx_closing_audit_calendar_time",
                        List.of("calendar_id", "action_at")),
                Map.entry("period_locks.idx_period_lock_fiscal_period", List.of("fiscal_period_id")),
                Map.entry("reopen_approvals.idx_reopen_approval_period_requested",
                        List.of("fiscal_period_id", "requested_at")),
                Map.entry("valuation_batches.idx_valuation_batch_fiscal_period",
                        List.of("fiscal_period_id")),
                Map.entry("provision_batches.idx_provision_batch_fiscal_period",
                        List.of("fiscal_period_id")),
                Map.entry("closing_adjustments.idx_closing_adjustment_fiscal_period",
                        List.of("fiscal_period_id")),
                Map.entry("closing_adjustments.idx_closing_adjustment_journal_entry",
                        List.of("journal_entry_id")));
    }

    private enum ColumnKind {
        BIGINT {
            @Override
            boolean matches(int jdbcType) {
                return jdbcType == Types.BIGINT;
            }
        },
        INTEGER {
            @Override
            boolean matches(int jdbcType) {
                return jdbcType == Types.INTEGER;
            }
        },
        BOOLEAN {
            @Override
            boolean matches(int jdbcType) {
                return jdbcType == Types.BOOLEAN || jdbcType == Types.BIT;
            }
        },
        DATE {
            @Override
            boolean matches(int jdbcType) {
                return jdbcType == Types.DATE;
            }
        },
        TIMESTAMP {
            @Override
            boolean matches(int jdbcType) {
                return jdbcType == Types.TIMESTAMP;
            }
        },
        STRING {
            @Override
            boolean matches(int jdbcType) {
                return jdbcType == Types.VARCHAR
                        || jdbcType == Types.LONGVARCHAR
                        || jdbcType == Types.CHAR
                        || jdbcType == Types.CLOB;
            }
        };

        abstract boolean matches(int jdbcType);
    }
}
