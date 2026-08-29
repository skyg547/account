package com.ho.account.migration;

import java.math.BigInteger;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

final class BatchMetadataVerifier {

    private static final Map<String, Set<String>> EXPECTED_COLUMNS = expectedColumns();
    private static final Set<String> EXPECTED_SEQUENCES = Set.of(
            "batch_step_execution_seq",
            "batch_job_execution_seq",
            "batch_job_seq");

    void verify(MigrationConfiguration configuration) {
        try (Connection connection = DriverManager.getConnection(
                configuration.url(),
                configuration.user(),
                configuration.password())) {
            verifyColumns(connection);
            verifySequences(connection);
        } catch (SQLException exception) {
            throw new IllegalStateException(
                    "Spring Batch metadata shape verification failed; inspect restricted DB logs",
                    exception);
        }
    }

    private void verifyColumns(Connection connection) throws SQLException {
        Map<String, Set<String>> actual = new LinkedHashMap<>();
        Map<String, ColumnDefinition> definitions = new LinkedHashMap<>();
        String query = """
                SELECT LOWER(table_name), LOWER(column_name), LOWER(data_type),
                       character_maximum_length, is_nullable
                  FROM information_schema.columns
                 WHERE LOWER(table_schema) = LOWER(CURRENT_SCHEMA())
                """;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                String table = normalized(resultSet.getString(1));
                if (EXPECTED_COLUMNS.containsKey(table)) {
                    actual.computeIfAbsent(table, ignored -> new LinkedHashSet<>())
                            .add(normalized(resultSet.getString(2)));
                    definitions.put(
                            key(table, resultSet.getString(2)),
                            new ColumnDefinition(
                                    normalized(resultSet.getString(3)),
                                    readNullableLong(resultSet, 4),
                                    "YES".equalsIgnoreCase(resultSet.getString(5))));
                }
            }
        }

        EXPECTED_COLUMNS.forEach((table, columns) -> {
            if (!columns.equals(actual.get(table))) {
                throw new IllegalStateException(
                        "Spring Batch metadata table has an incompatible shape: " + table);
            }
        });
        verifyCriticalDefinitions(definitions);
        verifyKeyConstraints(connection);
    }

    static Long readNullableLong(ResultSet resultSet, int columnIndex) throws SQLException {
        long value = resultSet.getLong(columnIndex);
        return resultSet.wasNull() ? null : value;
    }

    private void verifyCriticalDefinitions(Map<String, ColumnDefinition> definitions) {
        require(definitions, "batch_job_instance", "job_instance_id", "bigint", null, false);
        require(definitions, "batch_job_instance", "version", "bigint", null, true);
        require(definitions, "batch_job_instance", "job_name", "character varying", 100L, false);
        require(definitions, "batch_job_instance", "job_key", "character varying", 32L, false);
        require(definitions, "batch_job_execution", "job_execution_id", "bigint", null, false);
        require(definitions, "batch_job_execution", "version", "bigint", null, true);
        require(definitions, "batch_job_execution", "job_instance_id", "bigint", null, false);
        require(definitions, "batch_job_execution", "create_time", "timestamp", null, false);
        require(definitions, "batch_job_execution", "start_time", "timestamp", null, true);
        require(definitions, "batch_job_execution", "end_time", "timestamp", null, true);
        require(definitions, "batch_job_execution", "status", "character varying", 10L, true);
        require(definitions, "batch_job_execution", "exit_code", "character varying", 2500L, true);
        require(definitions, "batch_job_execution", "exit_message", "character varying", 2500L, true);
        require(definitions, "batch_job_execution", "last_updated", "timestamp", null, true);
        require(definitions, "batch_job_execution_params", "job_execution_id", "bigint", null, false);
        require(definitions, "batch_job_execution_params", "parameter_name", "character varying", 100L, false);
        require(definitions, "batch_job_execution_params", "parameter_type", "character varying", 100L, false);
        require(definitions, "batch_job_execution_params", "parameter_value", "character varying", 2500L, true);
        require(definitions, "batch_job_execution_params", "identifying", "character", 1L, false);
        require(definitions, "batch_step_execution", "step_execution_id", "bigint", null, false);
        require(definitions, "batch_step_execution", "version", "bigint", null, false);
        require(definitions, "batch_step_execution", "job_execution_id", "bigint", null, false);
        require(definitions, "batch_step_execution", "step_name", "character varying", 100L, false);
        require(definitions, "batch_step_execution", "create_time", "timestamp", null, false);
        require(definitions, "batch_step_execution", "start_time", "timestamp", null, true);
        require(definitions, "batch_step_execution", "end_time", "timestamp", null, true);
        require(definitions, "batch_step_execution", "status", "character varying", 10L, true);
        for (String counter : Set.of(
                "commit_count", "read_count", "filter_count", "write_count", "read_skip_count",
                "write_skip_count", "process_skip_count", "rollback_count")) {
            require(definitions, "batch_step_execution", counter, "bigint", null, true);
        }
        require(definitions, "batch_step_execution", "exit_code", "character varying", 2500L, true);
        require(definitions, "batch_step_execution", "exit_message", "character varying", 2500L, true);
        require(definitions, "batch_step_execution", "last_updated", "timestamp", null, true);
        require(definitions, "batch_step_execution_context", "step_execution_id", "bigint", null, false);
        require(definitions, "batch_step_execution_context", "short_context", "character varying", 2500L, false);
        require(definitions, "batch_step_execution_context", "serialized_context", "text", null, true);
        require(definitions, "batch_job_execution_context", "job_execution_id", "bigint", null, false);
        require(definitions, "batch_job_execution_context", "short_context", "character varying", 2500L, false);
        require(definitions, "batch_job_execution_context", "serialized_context", "text", null, true);
    }

    private void require(
            Map<String, ColumnDefinition> definitions,
            String table,
            String column,
            String expectedType,
            Long expectedLength,
            boolean expectedNullable) {
        ColumnDefinition definition = definitions.get(key(table, column));
        boolean typeMatches = definition != null && switch (expectedType) {
            case "timestamp" -> definition.dataType().startsWith("timestamp");
            case "text" -> Set.of("text", "character varying", "character large object")
                    .contains(definition.dataType());
            default -> expectedType.equals(definition.dataType());
        };
        if (!typeMatches
                || (expectedLength != null && !expectedLength.equals(definition.maximumLength()))
                || definition.nullable() != expectedNullable) {
            throw new IllegalStateException(
                    "Spring Batch metadata column has an incompatible definition: "
                            + table + "." + column);
        }
    }

    private void verifyKeyConstraints(Connection connection) throws SQLException {
        Map<String, Set<String>> primaryKeys = new LinkedHashMap<>();
        Map<String, Map<String, Set<String>>> uniqueKeys = new LinkedHashMap<>();
        Map<String, Set<String>> foreignKeys = new LinkedHashMap<>();
        String query = """
                SELECT LOWER(tc.table_name), LOWER(tc.constraint_type), LOWER(kcu.column_name),
                       LOWER(tc.constraint_name)
                  FROM information_schema.table_constraints tc
                  JOIN information_schema.key_column_usage kcu
                    ON tc.constraint_catalog = kcu.constraint_catalog
                   AND tc.constraint_schema = kcu.constraint_schema
                   AND tc.constraint_name = kcu.constraint_name
                 WHERE LOWER(tc.table_schema) = LOWER(CURRENT_SCHEMA())
                """;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                String table = normalized(resultSet.getString(1));
                String type = normalized(resultSet.getString(2));
                String column = normalized(resultSet.getString(3));
                if (EXPECTED_COLUMNS.containsKey(table)) {
                    if ("unique".equals(type)) {
                        uniqueKeys.computeIfAbsent(table, ignored -> new LinkedHashMap<>())
                                .computeIfAbsent(
                                        normalized(resultSet.getString(4)),
                                        ignored -> new LinkedHashSet<>())
                                .add(column);
                    } else {
                        Map<String, Set<String>> target = switch (type) {
                            case "primary key" -> primaryKeys;
                            case "foreign key" -> foreignKeys;
                            default -> null;
                        };
                        if (target != null) {
                            target.computeIfAbsent(table, ignored -> new LinkedHashSet<>()).add(column);
                        }
                    }
                }
            }
        }

        requireColumns(primaryKeys, "batch_job_instance", Set.of("job_instance_id"), "primary key");
        requireColumns(primaryKeys, "batch_job_execution", Set.of("job_execution_id"), "primary key");
        requireColumns(primaryKeys, "batch_step_execution", Set.of("step_execution_id"), "primary key");
        requireColumns(primaryKeys, "batch_step_execution_context", Set.of("step_execution_id"), "primary key");
        requireColumns(primaryKeys, "batch_job_execution_context", Set.of("job_execution_id"), "primary key");
        boolean hasCompositeJobKey = uniqueKeys
                .getOrDefault("batch_job_instance", Map.of())
                .values()
                .stream()
                .anyMatch(columns -> columns.equals(Set.of("job_name", "job_key")));
        if (!hasCompositeJobKey) {
            throw new IllegalStateException("Spring Batch job instance unique key is incomplete");
        }
        requireColumns(foreignKeys, "batch_job_execution", Set.of("job_instance_id"), "foreign key");
        requireColumns(foreignKeys, "batch_job_execution_params", Set.of("job_execution_id"), "foreign key");
        requireColumns(foreignKeys, "batch_step_execution", Set.of("job_execution_id"), "foreign key");
        requireColumns(foreignKeys, "batch_step_execution_context", Set.of("step_execution_id"), "foreign key");
        requireColumns(foreignKeys, "batch_job_execution_context", Set.of("job_execution_id"), "foreign key");
        verifyForeignKeyTargets(connection);
    }

    private void verifyForeignKeyTargets(Connection connection) throws SQLException {
        Map<String, String> actual = new LinkedHashMap<>();
        DatabaseMetaData metadata = connection.getMetaData();
        String schema = connection.getSchema();
        for (String table : EXPECTED_COLUMNS.keySet()) {
            try (ResultSet resultSet = metadata.getImportedKeys(null, schema, table)) {
                while (resultSet.next()) {
                    actual.put(
                            key(resultSet.getString("FKTABLE_NAME"), resultSet.getString("FKCOLUMN_NAME")),
                            key(resultSet.getString("PKTABLE_NAME"), resultSet.getString("PKCOLUMN_NAME")));
                }
            }
        }
        requireReference(actual, "batch_job_execution", "job_instance_id",
                "batch_job_instance", "job_instance_id");
        requireReference(actual, "batch_job_execution_params", "job_execution_id",
                "batch_job_execution", "job_execution_id");
        requireReference(actual, "batch_step_execution", "job_execution_id",
                "batch_job_execution", "job_execution_id");
        requireReference(actual, "batch_step_execution_context", "step_execution_id",
                "batch_step_execution", "step_execution_id");
        requireReference(actual, "batch_job_execution_context", "job_execution_id",
                "batch_job_execution", "job_execution_id");
    }

    private void requireReference(
            Map<String, String> references,
            String table,
            String column,
            String targetTable,
            String targetColumn) {
        if (!key(targetTable, targetColumn).equals(references.get(key(table, column)))) {
            throw new IllegalStateException(
                    "Spring Batch metadata foreign key target is incompatible: "
                            + table + "." + column);
        }
    }

    private void requireColumns(
            Map<String, Set<String>> constraints,
            String table,
            Set<String> expected,
            String kind) {
        if (!constraints.getOrDefault(table, Set.of()).containsAll(expected)) {
            throw new IllegalStateException(
                    "Spring Batch metadata " + kind + " is incomplete: " + table);
        }
    }

    private void verifySequences(Connection connection) throws SQLException {
        Map<String, SequenceDefinition> actual = new LinkedHashMap<>();
        String query = """
                SELECT LOWER(sequence_name), LOWER(data_type), increment, maximum_value, cycle_option
                  FROM information_schema.sequences
                 WHERE LOWER(sequence_schema) = LOWER(CURRENT_SCHEMA())
                """;
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                actual.put(
                        normalized(resultSet.getString(1)),
                        new SequenceDefinition(
                                normalized(resultSet.getString(2)),
                                new BigInteger(resultSet.getString(3)),
                                new BigInteger(resultSet.getString(4)),
                                resultSet.getString(5)));
            }
        }
        for (String sequence : EXPECTED_SEQUENCES) {
            SequenceDefinition definition = actual.get(sequence);
            if (definition == null
                    || !"bigint".equals(definition.dataType())
                    || !BigInteger.ONE.equals(definition.increment())
                    || !BigInteger.valueOf(Long.MAX_VALUE).equals(definition.maximumValue())
                    || !"NO".equalsIgnoreCase(definition.cycleOption())) {
                throw new IllegalStateException(
                        "Spring Batch metadata sequence has an incompatible definition: " + sequence);
            }
        }
    }

    private static Map<String, Set<String>> expectedColumns() {
        Map<String, Set<String>> columns = new LinkedHashMap<>();
        columns.put("batch_job_instance", Set.of(
                "job_instance_id", "version", "job_name", "job_key"));
        columns.put("batch_job_execution", Set.of(
                "job_execution_id", "version", "job_instance_id", "create_time", "start_time",
                "end_time", "status", "exit_code", "exit_message", "last_updated"));
        columns.put("batch_job_execution_params", Set.of(
                "job_execution_id", "parameter_name", "parameter_type", "parameter_value",
                "identifying"));
        columns.put("batch_step_execution", Set.of(
                "step_execution_id", "version", "step_name", "job_execution_id", "create_time",
                "start_time", "end_time", "status", "commit_count", "read_count", "filter_count",
                "write_count", "read_skip_count", "write_skip_count", "process_skip_count",
                "rollback_count", "exit_code", "exit_message", "last_updated"));
        columns.put("batch_step_execution_context", Set.of(
                "step_execution_id", "short_context", "serialized_context"));
        columns.put("batch_job_execution_context", Set.of(
                "job_execution_id", "short_context", "serialized_context"));
        return Map.copyOf(columns);
    }

    private String normalized(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private String key(String table, String column) {
        return normalized(table) + "." + normalized(column);
    }

    private record ColumnDefinition(String dataType, Long maximumLength, boolean nullable) {
    }

    private record SequenceDefinition(
            String dataType,
            BigInteger increment,
            BigInteger maximumValue,
            String cycleOption) {
    }
}
