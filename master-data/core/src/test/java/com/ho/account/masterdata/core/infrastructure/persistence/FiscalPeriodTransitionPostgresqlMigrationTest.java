package com.ho.account.masterdata.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodTransitionRepository;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/** Opt-in PostgreSQL proof for the production V14 schema and partial unique fence. */
@EnabledIfEnvironmentVariable(named = "MASTER_DATA_TEST_POSTGRES_URL", matches = "jdbc:postgresql:.*")
class FiscalPeriodTransitionPostgresqlMigrationTest {
    private static final String SCHEMA = "fiscal_transition_test_"
            + UUID.randomUUID().toString().replace("-", "");

    @Test
    void v14EnforcesOnePreparedOperationAndExpectedStateCas() throws Exception {
        String url = System.getenv("MASTER_DATA_TEST_POSTGRES_URL");
        Flyway.configure().dataSource(url, "postgres", "")
                .schemas(SCHEMA).defaultSchema(SCHEMA).createSchemas(true)
                .locations("classpath:db/migration").load().migrate();

        try (Connection connection = DriverManager.getConnection(url, "postgres", "")) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("SET search_path TO " + SCHEMA);
            }
            long periodId;
            try (PreparedStatement insert = connection.prepareStatement("""
                    INSERT INTO fiscal_periods
                        (fiscal_year, fiscal_period, start_date, end_date, closing_status)
                    VALUES ('2026', '11', DATE '2026-11-01', DATE '2026-11-30', 'CLOSED')
                    RETURNING id
                    """)) {
                try (ResultSet result = insert.executeQuery()) {
                    assertThat(result.next()).isTrue();
                    periodId = result.getLong(1);
                }
            }
            insertReceipt(connection, "first-op", periodId, "CLOSED", "OPEN");
            assertThat(admissionStatus(connection, periodId)).isEqualTo("CLOSED");
            assertThatThrownBy(() -> insertReceipt(connection, "second-op", periodId, "OPEN", "CLOSED"))
                    .isInstanceOf(SQLException.class)
                    .extracting(error -> ((SQLException) error).getSQLState())
                    .isEqualTo("23505");

            try (PreparedStatement apply = connection.prepareStatement("""
                    UPDATE fiscal_period_transitions
                    SET phase='APPLIED', applied_at=now() WHERE operation_id='first-op' AND phase='PREPARED'
                    """)) {
                assertThat(apply.executeUpdate()).isEqualTo(1);
            }
            try (PreparedStatement cas = connection.prepareStatement("""
                    UPDATE fiscal_periods SET closing_status='OPEN'
                    WHERE id=? AND closing_status='CLOSED'
                    """)) {
                cas.setLong(1, periodId);
                assertThat(cas.executeUpdate()).isEqualTo(1);
                assertThat(cas.executeUpdate()).isZero();
            }
            // Persisted OPEN remains admission CLOSED until Closing confirms local completion.
            assertThat(admissionStatus(connection, periodId)).isEqualTo("CLOSED");
            try (Statement confirm = connection.createStatement()) {
                assertThat(confirm.executeUpdate("""
                        UPDATE fiscal_period_transitions SET phase='CONFIRMED', confirmed_at=now()
                        WHERE operation_id='first-op' AND phase='APPLIED'
                        """)).isEqualTo(1);
            }
            assertThat(admissionStatus(connection, periodId)).isEqualTo("OPEN");
            insertReceipt(connection, "second-op", periodId, "OPEN", "CLOSED");
            assertThat(admissionStatus(connection, periodId)).isEqualTo("CLOSED");
            try (Statement cancel = connection.createStatement()) {
                assertThat(cancel.executeUpdate("""
                        UPDATE fiscal_period_transitions SET phase='CANCELLED', cancelled_at=now()
                        WHERE operation_id='second-op' AND phase='PREPARED'
                        """)).isEqualTo(1);
            }
            insertReceipt(connection, "third-op", periodId, "OPEN", "CLOSED");
        }
    }

    private String admissionStatus(Connection connection, long periodId) throws SQLException {
        try (PreparedStatement query = connection.prepareStatement(
                FiscalPeriodTransitionRepository.ADMISSION_SELECT + "WHERE p.id = ?")) {
            query.setLong(1, periodId);
            try (ResultSet result = query.executeQuery()) {
                assertThat(result.next()).isTrue();
                return result.getString("closingStatus");
            }
        }
    }

    private void insertReceipt(Connection connection, String operationId, long periodId,
            String expectedStatus, String targetStatus) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement("""
                INSERT INTO fiscal_period_transitions
                    (operation_id, fiscal_period_id, expected_status, target_status,
                     audit_user, phase, created_at)
                VALUES (?, ?, ?, ?, 'alice', 'PREPARED', now())
                """)) {
            insert.setString(1, operationId);
            insert.setLong(2, periodId);
            insert.setString(3, expectedStatus);
            insert.setString(4, targetStatus);
            insert.executeUpdate();
        }
    }
}
