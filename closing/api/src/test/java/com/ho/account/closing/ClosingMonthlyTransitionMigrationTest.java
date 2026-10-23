package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ClosingMonthlyTransitionMigrationTest {
    @Test
    void v52PreservesExistingCalendarAndEnforcesCompleteFailClosedIntent() {
        DriverManagerDataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:monthly-forward-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(source).locations("classpath:db/closing-migration")
                .table("flyway_schema_history_closing").target("51").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.update("""
                INSERT INTO closing_calendars(id, fiscal_year, fiscal_period, status, is_current_period, audit_user)
                VALUES (10, '2050', '01', 'CLOSED', FALSE, 'original-closer')
                """);

        Flyway.configure().dataSource(source).locations("classpath:db/closing-migration")
                .table("flyway_schema_history_closing").load().migrate();

        assertThat(jdbc.queryForMap("SELECT status, audit_user, transition_id FROM closing_calendars WHERE id=10"))
                .containsEntry("STATUS", "CLOSED").containsEntry("AUDIT_USER", "original-closer")
                .containsEntry("TRANSITION_ID", null);
        assertThatThrownBy(() -> jdbc.update("UPDATE closing_calendars SET transition_id='incomplete' WHERE id=10"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("""
                UPDATE closing_calendars SET transition_id=?, transition_target='OPEN', transition_stage='DISPATCHED',
                    transition_fiscal_period_id=1, transition_approval_id=7, transition_actor='checker',
                    transition_prepared_at=CURRENT_TIMESTAMP WHERE id=10
                """, UUID.randomUUID().toString());
        assertThatThrownBy(() -> jdbc.update("UPDATE closing_calendars SET status='OPEN' WHERE id=10"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE closing_calendars SET transition_stage='RETRY' WHERE id=10"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT status FROM closing_calendars WHERE id=10", String.class))
                .isEqualTo("CLOSED");
        // V58 permits OPEN only after the durable authorization stage, while retaining the fence.
        jdbc.update("UPDATE closing_calendars SET transition_protocol='RECEIPT_V1' WHERE id=10");
        assertThatThrownBy(() -> jdbc.update("UPDATE closing_calendars SET transition_stage='AUTHORIZED' WHERE id=10"))
                .isInstanceOf(DataIntegrityViolationException.class);
        jdbc.update("UPDATE closing_calendars SET transition_stage='AUTHORIZED', status='OPEN' WHERE id=10");
        assertThat(jdbc.queryForMap("SELECT status, transition_stage, transition_protocol FROM closing_calendars WHERE id=10"))
                .containsEntry("STATUS", "OPEN")
                .containsEntry("TRANSITION_STAGE", "AUTHORIZED")
                .containsEntry("TRANSITION_PROTOCOL", "RECEIPT_V1");
        jdbc.update("UPDATE closing_calendars SET transition_stage='READY_TO_CONFIRM' WHERE id=10");
        assertThat(jdbc.queryForObject("SELECT transition_stage FROM closing_calendars WHERE id=10", String.class))
                .isEqualTo("READY_TO_CONFIRM");
        assertThatThrownBy(() -> jdbc.update("UPDATE closing_calendars SET transition_protocol='UNKNOWN' WHERE id=10"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE closing_calendars SET transition_target='CLOSED' WHERE id=10"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE closing_calendars SET transition_approval_id=NULL WHERE id=10"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
