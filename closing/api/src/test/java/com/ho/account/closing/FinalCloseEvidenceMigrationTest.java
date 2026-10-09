package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class FinalCloseEvidenceMigrationTest {
    @Test
    void v53CreatesImmutableEvidenceConstraintsAndTransitionBindingColumn() {
        DriverManagerDataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:evidence-v53-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(source).locations("classpath:db/closing-migration")
                .table("flyway_schema_history_closing").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(source);
        jdbc.update("""
                INSERT INTO closing_calendars(id, fiscal_year, fiscal_period, status, is_current_period, audit_user)
                VALUES (10, '2050', '01', 'IN_PROGRESS', FALSE, 'closer')
                """);
        jdbc.update("""
                INSERT INTO final_close_evidence_sets(evidence_set_id, calendar_id, fiscal_period_id, fiscal_year,
                    fiscal_period, ledger_cutoff, observed_at, submitted_by, content_digest)
                VALUES ('set-1', 10, 20, '2050', '01', DATE '2050-01-31', CURRENT_TIMESTAMP, 'provider', ?)
                """, "a".repeat(64));
        Long setId = jdbc.queryForObject("SELECT id FROM final_close_evidence_sets WHERE evidence_set_id='set-1'", Long.class);
        jdbc.update("""
                INSERT INTO final_close_evidence_controls(evidence_set_db_id, control_type, source_system, source_run_id,
                    outcome, blocking_item_count) VALUES (?, 'AP_SUBLEDGER', 'payable', 'run-1', 'PASS', 0)
                """, setId);
        Long controlId = jdbc.queryForObject("SELECT id FROM final_close_evidence_controls", Long.class);
        jdbc.update("""
                INSERT INTO final_close_evidence_totals(control_id, account_code, currency_code, source_total, posted_total)
                VALUES (?, '1000', 'KRW', 0.000000000000000000, 0.000000000000000000)
                """, controlId);

        jdbc.update("""
                UPDATE closing_calendars SET transition_id=?, transition_target='CLOSED', transition_stage='PREPARED',
                    transition_fiscal_period_id=20, transition_actor='closer', transition_prepared_at=CURRENT_TIMESTAMP,
                    transition_evidence_set_id='set-1' WHERE id=10
                """, UUID.randomUUID().toString());
        assertThat(jdbc.queryForObject("SELECT transition_evidence_set_id FROM closing_calendars WHERE id=10", String.class))
                .isEqualTo("set-1");
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO final_close_evidence_sets(evidence_set_id, calendar_id, fiscal_period_id, fiscal_year,
                    fiscal_period, ledger_cutoff, observed_at, submitted_by, content_digest)
                VALUES ('set-1', 10, 20, '2050', '01', DATE '2050-01-31', CURRENT_TIMESTAMP, 'provider', ?)
                """, "b".repeat(64))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE final_close_evidence_controls SET blocking_item_count=-1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE final_close_evidence_controls SET outcome='UNKNOWN'"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE closing_calendars SET transition_evidence_set_id='missing' WHERE id=10"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
