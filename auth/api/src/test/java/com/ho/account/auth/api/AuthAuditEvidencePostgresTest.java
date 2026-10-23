package com.ho.account.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Proves the V75 audit tables can be operated with separated grants and verified exports. */
class AuthAuditEvidencePostgresTest {
    private static final String SUBJECT = "synthetic_audit_export_924";
    private static EmbeddedPostgres postgres;
    private static Flyway flyway;

    @BeforeAll
    static void migrateRealPostgresSchema() throws IOException {
        postgres = EmbeddedPostgres.builder().start();
        flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl("postgres", "postgres"), "postgres", "")
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();
    }

    @AfterAll
    static void stopPostgres() throws IOException {
        if (postgres != null) postgres.close();
    }

    @Test
    void auditWriterCannotModifyEventsAndReaderCannotWrite() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.execute("CREATE ROLE auth_audit_writer_924 NOLOGIN");
            statement.execute("CREATE ROLE auth_audit_reader_924 NOLOGIN");
            statement.execute("GRANT USAGE ON SCHEMA public TO auth_audit_writer_924, auth_audit_reader_924");
            statement.execute("GRANT INSERT ON auth_login_security_events, auth_pat_lifecycle_events "
                    + "TO auth_audit_writer_924");
            statement.execute("GRANT SELECT ON auth_login_security_events, auth_pat_lifecycle_events "
                    + "TO auth_audit_reader_924");

            // SET ROLE checks actual PostgreSQL grants without storing credentials for test roles.
            try {
                statement.execute("SET ROLE auth_audit_writer_924");
                statement.executeUpdate("INSERT INTO auth_login_security_events "
                        + "(event_id, claimed_username, event_type, reason_code, "
                        + "before_failure_count, after_failure_count, occurred_at) VALUES "
                        + "('writer-evidence-924', 'synthetic_writer', 'FAILURE', 'INVALID_PASSWORD', "
                        + "0, 1, TIMESTAMP '2026-01-01 00:00:00')");
                statement.executeUpdate("INSERT INTO auth_pat_lifecycle_events "
                        + "(event_id, token_id, action, actor_username, owner_username, occurred_at, "
                        + "reason_code, before_status, after_status) VALUES "
                        + "('writer-pat-924', 'synthetic-pat', 'REVOKED', 'synthetic_admin', "
                        + "'synthetic_owner', TIMESTAMP '2026-01-01 00:00:00', "
                        + "'ADMIN_SECURITY', 'ACTIVE', 'REVOKED')");
                assertPermissionDenied(statement, "UPDATE auth_login_security_events "
                        + "SET reason_code = 'TAMPERED' WHERE event_id = 'writer-evidence-924'");
                assertPermissionDenied(statement, "DELETE FROM auth_pat_lifecycle_events "
                        + "WHERE event_id = 'writer-pat-924'");

                statement.execute("SET ROLE auth_audit_reader_924");
                try (ResultSet rows = statement.executeQuery("SELECT count(*) FROM auth_login_security_events")) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getInt(1)).isPositive();
                }
                assertPermissionDenied(statement, "UPDATE auth_pat_lifecycle_events "
                        + "SET reason_code = 'TAMPERED' WHERE event_id = 'writer-pat-924'");
                assertPermissionDenied(statement, "DELETE FROM auth_login_security_events "
                        + "WHERE event_id = 'writer-evidence-924'");
                assertPermissionDenied(statement, "INSERT INTO auth_pat_lifecycle_events "
                        + "(event_id, token_id, action, actor_username, owner_username, occurred_at, "
                        + "reason_code, before_status, after_status) VALUES "
                        + "('reader-forbidden-924', 'synthetic-pat', 'REVOKED', 'synthetic_admin', "
                        + "'synthetic_owner', TIMESTAMP '2026-01-01 00:00:00', "
                        + "'ADMIN_SECURITY', 'ACTIVE', 'REVOKED')");
            } finally {
                statement.execute("RESET ROLE");
            }
        }
    }

    @Test
    void orderedExportRestoresWithMatchingChecksumAndDetectsTampering() throws SQLException {
        try (Connection connection = connect()) {
            insertEvent(connection, new ExportRow("export-2-924", SUBJECT, SUBJECT,
                    "SUCCESS", "AUTHENTICATED", 1, 0,
                    LocalDateTime.parse("2026-01-01T00:15:00"), null,
                    LocalDateTime.parse("2026-01-02T00:00:00")));
            insertEvent(connection, new ExportRow("export-1-924", SUBJECT, null,
                    "FAILURE", "INVALID_PASSWORD", 0, 1,
                    null, LocalDateTime.parse("2026-01-01T00:15:00"),
                    LocalDateTime.parse("2026-01-01T00:00:00")));

            List<ExportRow> original = export(connection, "auth_login_security_events");
            assertThat(original).extracting(ExportRow::eventId)
                    .containsExactly("export-1-924", "export-2-924");
            String manifestHash = checksum(original);

            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TEMP TABLE audit_export_restore "
                        + "(LIKE auth_login_security_events INCLUDING ALL)");
            }
            for (ExportRow row : original) insertEventInto(connection, "audit_export_restore", row);
            assertThat(checksum(export(connection, "audit_export_restore"))).isEqualTo(manifestHash);

            try (Statement statement = connection.createStatement()) {
                // Authenticated identity was previously omitted from the checksum contract.
                statement.executeUpdate("UPDATE audit_export_restore SET authenticated_username = 'tampered' "
                        + "WHERE event_id = 'export-2-924'");
            }
            assertThat(checksum(export(connection, "audit_export_restore"))).isNotEqualTo(manifestHash);
        }
    }

    @Test
    void patExportRestoresActorReasonAndStateWithMatchingChecksum() throws SQLException {
        PatExportRow created = new PatExportRow("export-pat-created-924", "synthetic-export-pat",
                "CREATED", "synthetic_owner", "synthetic_owner", "OWNER_CREATED",
                "ABSENT", "ACTIVE", LocalDateTime.parse("2026-01-02T00:00:00"));
        PatExportRow revoked = new PatExportRow("export-pat-revoked-924", "synthetic-export-pat",
                "REVOKED", "synthetic_admin", "synthetic_owner", "ADMIN_ROTATION",
                "ACTIVE", "REVOKED", LocalDateTime.parse("2026-01-03T00:00:00"));
        try (Connection connection = connect()) {
            insertPatEvent(connection, "auth_pat_lifecycle_events", revoked);
            insertPatEvent(connection, "auth_pat_lifecycle_events", created);
            List<PatExportRow> exported = exportPat(connection, "auth_pat_lifecycle_events");
            assertThat(exported).containsExactly(created, revoked);
            String manifestHash = checksumText(exported.stream().map(PatExportRow::canonical).toList());

            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TEMP TABLE pat_export_restore "
                        + "(LIKE auth_pat_lifecycle_events INCLUDING ALL)");
            }
            for (PatExportRow row : exported) insertPatEvent(connection, "pat_export_restore", row);
            assertThat(checksumText(exportPat(connection, "pat_export_restore").stream()
                    .map(PatExportRow::canonical).toList())).isEqualTo(manifestHash);
        }
    }

    @Test
    void repeatMigrationDoesNotPurgeEvidenceBeyondSevenYearBoundary() throws SQLException {
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO auth_login_security_events "
                    + "(event_id, claimed_username, event_type, reason_code, "
                    + "before_failure_count, after_failure_count, occurred_at) VALUES "
                    + "('old-evidence-924', 'synthetic_old', 'FAILURE', 'INVALID_PASSWORD', "
                    + "0, 1, TIMESTAMP '2017-01-01 00:00:00')");
        }

        flyway.migrate();

        try (Connection connection = connect(); Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery("SELECT count(*) FROM auth_login_security_events "
                        + "WHERE event_id = 'old-evidence-924' AND occurred_at < TIMESTAMP '2018-01-01 00:00:00'")) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isEqualTo(1);
        }
    }

    private static void assertPermissionDenied(Statement statement, String sql) {
        assertThatThrownBy(() -> statement.executeUpdate(sql))
                .isInstanceOf(SQLException.class)
                .satisfies(error -> assertThat(((SQLException) error).getSQLState()).isEqualTo("42501"));
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl("postgres", "postgres"), "postgres", "");
    }

    private static void insertEvent(Connection connection, ExportRow row) throws SQLException {
        insertEventInto(connection, "auth_login_security_events", row);
    }

    private static void insertEventInto(Connection connection, String table, ExportRow row) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table + " "
                + "(event_id, claimed_username, authenticated_username, event_type, reason_code, "
                + "before_failure_count, after_failure_count, before_locked_until, after_locked_until, occurred_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, row.eventId());
            insert.setString(2, row.claimedUsername());
            insert.setString(3, row.authenticatedUsername());
            insert.setString(4, row.eventType());
            insert.setString(5, row.reasonCode());
            insert.setInt(6, row.beforeCount());
            insert.setInt(7, row.afterCount());
            insert.setObject(8, row.beforeLockedUntil());
            insert.setObject(9, row.afterLockedUntil());
            insert.setObject(10, row.occurredAt());
            insert.executeUpdate();
        }
    }

    private static List<ExportRow> export(Connection connection, String table) throws SQLException {
        List<ExportRow> rows = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement("SELECT event_id, claimed_username, "
                + "authenticated_username, event_type, reason_code, before_failure_count, "
                + "after_failure_count, before_locked_until, after_locked_until, occurred_at FROM "
                + table + " WHERE claimed_username = ? ORDER BY occurred_at, event_id")) {
            query.setString(1, SUBJECT);
            try (ResultSet result = query.executeQuery()) {
                while (result.next()) {
                    rows.add(new ExportRow(result.getString(1), result.getString(2), result.getString(3),
                            result.getString(4), result.getString(5), result.getInt(6), result.getInt(7),
                            result.getObject(8, LocalDateTime.class),
                            result.getObject(9, LocalDateTime.class),
                            result.getObject(10, LocalDateTime.class)));
                }
            }
        }
        return rows;
    }

    private static void insertPatEvent(Connection connection, String table, PatExportRow row)
            throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("INSERT INTO " + table + " "
                + "(event_id, token_id, action, actor_username, owner_username, reason_code, "
                + "before_status, after_status, occurred_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, row.eventId());
            insert.setString(2, row.tokenId());
            insert.setString(3, row.action());
            insert.setString(4, row.actorUsername());
            insert.setString(5, row.ownerUsername());
            insert.setString(6, row.reasonCode());
            insert.setString(7, row.beforeStatus());
            insert.setString(8, row.afterStatus());
            insert.setObject(9, row.occurredAt());
            insert.executeUpdate();
        }
    }

    private static List<PatExportRow> exportPat(Connection connection, String table) throws SQLException {
        List<PatExportRow> rows = new ArrayList<>();
        try (PreparedStatement query = connection.prepareStatement("SELECT event_id, token_id, action, "
                + "actor_username, owner_username, reason_code, before_status, after_status, occurred_at "
                + "FROM " + table + " WHERE token_id = ? ORDER BY occurred_at, event_id")) {
            query.setString(1, "synthetic-export-pat");
            try (ResultSet result = query.executeQuery()) {
                while (result.next()) {
                    rows.add(new PatExportRow(result.getString(1), result.getString(2),
                            result.getString(3), result.getString(4), result.getString(5),
                            result.getString(6), result.getString(7), result.getString(8),
                            result.getObject(9, LocalDateTime.class)));
                }
            }
        }
        return rows;
    }

    private static String checksum(List<ExportRow> rows) {
        return checksumText(rows.stream().map(ExportRow::canonical).toList());
    }

    private static String checksumText(List<String> rows) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            String canonical = String.join("\n", rows);
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 unavailable", error);
        }
    }

    private record ExportRow(String eventId, String claimedUsername, String authenticatedUsername,
            String eventType, String reasonCode, int beforeCount, int afterCount,
            LocalDateTime beforeLockedUntil, LocalDateTime afterLockedUntil, LocalDateTime occurredAt) {
        String canonical() {
            return String.join("", field(eventId), field(claimedUsername), field(authenticatedUsername),
                    field(eventType), field(reasonCode), field(beforeCount), field(afterCount),
                    field(beforeLockedUntil), field(afterLockedUntil), field(occurredAt));
        }
    }

    private record PatExportRow(String eventId, String tokenId, String action, String actorUsername,
            String ownerUsername, String reasonCode, String beforeStatus, String afterStatus,
            LocalDateTime occurredAt) {
        String canonical() {
            return String.join("", field(eventId), field(tokenId), field(action), field(actorUsername),
                    field(ownerUsername), field(reasonCode), field(beforeStatus), field(afterStatus),
                    field(occurredAt));
        }
    }

    private static String field(Object value) {
        // Length framing keeps null and delimiter-bearing identifiers unambiguous across exports.
        if (value == null) return "-1:";
        String text = value.toString();
        return text.getBytes(StandardCharsets.UTF_8).length + ":" + text;
    }
}
