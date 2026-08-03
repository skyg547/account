package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class DailyClosingStatusMigrationTest {

    @Test
    void migrationBackfillsLegacyBooleanRowsAndRemovesAmbiguousFlag() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:eod-migration;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        JdbcTemplate jdbc = new JdbcTemplate(dataSource);
        jdbc.execute("""
                CREATE TABLE daily_closing_status (
                    date DATE PRIMARY KEY,
                    is_closed BOOLEAN NOT NULL,
                    closed_at TIMESTAMP,
                    closed_by VARCHAR(80)
                )
                """);
        LocalDateTime closedAt = LocalDateTime.of(2026, 7, 30, 23, 10);
        jdbc.update(
                "INSERT INTO daily_closing_status(date, is_closed, closed_at, closed_by) VALUES (?, ?, ?, ?)",
                java.sql.Date.valueOf("2026-07-30"),
                true,
                Timestamp.valueOf(closedAt),
                "closer");
        jdbc.update(
                "INSERT INTO daily_closing_status(date, is_closed, closed_at, closed_by) VALUES (?, ?, ?, ?)",
                java.sql.Date.valueOf("2026-07-31"),
                false,
                null,
                null);
        jdbc.execute("""
                CREATE TABLE "flyway_schema_history" (
                    "version" VARCHAR(50),
                    "success" BOOLEAN
                )
                """);
        jdbc.update("""
                INSERT INTO "flyway_schema_history"("version", "success")
                VALUES ('70', TRUE)
                """);

        Flyway.configure()
                .dataSource(dataSource)
                .locations("classpath:db/closing-migration")
                .table("flyway_schema_history_closing")
                .baselineOnMigrate(true)
                .baselineVersion("49")
                // This fixture intentionally contains only the pre-V50 daily table. V51 is
                // covered by the complete legacy-schema runner tests.
                .target("50")
                .load()
                .migrate();

        Map<String, Object> closed = jdbc.queryForMap(
                "SELECT state, version, created_by, updated_by, closed_by FROM daily_closing_status WHERE date = DATE '2026-07-30'");
        assertThat(closed)
                .containsEntry("STATE", "CLOSED")
                .containsEntry("VERSION", 0L)
                .containsEntry("CREATED_BY", "closer")
                .containsEntry("UPDATED_BY", "closer")
                .containsEntry("CLOSED_BY", "closer");

        Map<String, Object> open = jdbc.queryForMap(
                "SELECT state, created_by, opened_by, opened_at FROM daily_closing_status WHERE date = DATE '2026-07-31'");
        assertThat(open)
                .containsEntry("STATE", "OPEN")
                .containsEntry("CREATED_BY", "MIGRATION")
                .containsEntry("OPENED_BY", "MIGRATION");
        assertThat(open.get("OPENED_AT")).isNotNull();

        Integer legacyColumnCount = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.COLUMNS
                WHERE TABLE_NAME = 'DAILY_CLOSING_STATUS'
                  AND COLUMN_NAME = 'IS_CLOSED'
                """, Integer.class);
        assertThat(legacyColumnCount).isZero();

        Integer appliedMigrationCount = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM "flyway_schema_history_closing"
                WHERE "version" = '50'
                  AND "success" = TRUE
                """, Integer.class);
        assertThat(appliedMigrationCount).isOne();

        Integer untouchedSharedHistoryCount = jdbc.queryForObject("""
                SELECT COUNT(*)
                FROM "flyway_schema_history"
                WHERE "version" = '70'
                  AND "success" = TRUE
                """, Integer.class);
        assertThat(untouchedSharedHistoryCount).isOne();
    }
}
