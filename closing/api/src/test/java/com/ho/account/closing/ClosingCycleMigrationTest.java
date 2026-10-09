package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ClosingCycleMigrationTest {
    @Test
    void v54BackfillsExistingCalendarAndControlRowsAsFirstCycle() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:closing-cycle-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa", "");
        Flyway.configure().dataSource(source).locations("classpath:db/closing-migration")
                .table("flyway_schema_history_closing").target("53").load().migrate();
        var jdbc = new JdbcTemplate(source);
        jdbc.update("""
                INSERT INTO closing_calendars(id, fiscal_year, fiscal_period, status, is_current_period)
                VALUES (10, '2054', '01', 'CLOSED', FALSE)
                """);
        jdbc.update("""
                INSERT INTO closing_tasks(id, calendar_id, name, status, is_mandatory, task_order)
                VALUES (11, 10, 'Reconcile', 'COMPLETED', TRUE, 1)
                """);
        jdbc.update("""
                INSERT INTO closing_gates(id, calendar_id, name, status)
                VALUES (12, 10, 'Approve', 'PASSED')
                """);
        jdbc.update("""
                INSERT INTO closing_calendars(id, fiscal_year, fiscal_period, status, reopened_at, is_current_period)
                VALUES (20, '2054', '02', 'OPEN', CURRENT_TIMESTAMP, FALSE)
                """);
        jdbc.update("""
                INSERT INTO closing_tasks(id, calendar_id, name, status, is_mandatory, task_order)
                VALUES (21, 20, 'Legacy reconciliation', 'COMPLETED', TRUE, 1)
                """);
        jdbc.update("""
                INSERT INTO closing_gates(id, calendar_id, name, status)
                VALUES (22, 20, 'Legacy approval', 'PASSED')
                """);

        Flyway.configure().dataSource(source).locations("classpath:db/closing-migration")
                .table("flyway_schema_history_closing").load().migrate();

        assertThat(jdbc.queryForObject("SELECT cycle_number FROM closing_calendars WHERE id=10", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT cycle_number FROM closing_tasks WHERE id=11", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT cycle_number FROM closing_gates WHERE id=12", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM closing_tasks WHERE id=11", String.class))
                .isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT status FROM closing_gates WHERE id=12", String.class))
                .isEqualTo("PASSED");
        assertThat(jdbc.queryForObject("SELECT cycle_number FROM closing_calendars WHERE id=20", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT last_source_changed_at FROM closing_calendars WHERE id=20",
                java.sql.Timestamp.class)).isNull();
        assertThat(jdbc.queryForObject("SELECT cycle_number FROM closing_tasks WHERE id=21", Integer.class))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT cycle_number FROM closing_gates WHERE id=22", Integer.class))
                .isEqualTo(1);
        assertThatThrownBy(() -> jdbc.update("UPDATE closing_tasks SET cycle_number=0 WHERE id=11"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
