package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** H2 PostgreSQL-mode migration coverage; this does not replace PostgreSQL migration validation. */
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

        assertMigrationsApplied(jdbc);

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
        // V59 permits OPEN only after the durable authorization stage, while retaining the fence.
        jdbc.update("""
                UPDATE closing_calendars SET transition_protocol='RECEIPT_V1',
                    journal_fence_operation_id='original-close', journal_fence_period_id=1,
                    journal_fence_start_date=DATE '2050-01-01', journal_fence_end_date=DATE '2050-01-31',
                    journal_fence_revision=23, journal_fence_actor='original-closer' WHERE id=10
                """);
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

    @Test
    void h2V58ToV59PreservesReleaseIntentsAndRequiresTheirOriginalFences() {
        DriverManagerDataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:fence-forward-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(source).locations("classpath:db/closing-migration")
                .table("flyway_schema_history_closing").target("58").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(source);
        insertFencedTransition(jdbc, 20, "OPEN", "OPEN", "RELEASE_PENDING", "reopen", "original-close", 7L);
        insertFencedTransition(jdbc, 21, "IN_PROGRESS", "CLOSED", "CANCEL_RELEASE_PENDING", "cancel", "cancel", null);
        var reopenBefore = jdbc.queryForMap("SELECT * FROM closing_calendars WHERE id=20");
        var cancelBefore = jdbc.queryForMap("SELECT * FROM closing_calendars WHERE id=21");

        Flyway.configure().dataSource(source).locations("classpath:db/closing-migration")
                .table("flyway_schema_history_closing").target("59").load().migrate();

        assertMigrationsApplied(jdbc);
        assertThat(jdbc.queryForMap("SELECT * FROM closing_calendars WHERE id=20"))
                .containsAllEntriesOf(reopenBefore).containsEntry("TRANSITION_PROTOCOL", null);
        assertThat(jdbc.queryForMap("SELECT * FROM closing_calendars WHERE id=21"))
                .containsAllEntriesOf(cancelBefore).containsEntry("TRANSITION_PROTOCOL", null);
        for (long id : new long[] {20, 21}) {
            jdbc.update("UPDATE closing_calendars SET transition_protocol='RECEIPT_V1' WHERE id=?", id);
            // Clear the whole fence so rejection tests the transition rule, not the fence completeness rule.
            assertThatThrownBy(() -> jdbc.update("""
                    UPDATE closing_calendars SET journal_fence_operation_id=NULL, journal_fence_period_id=NULL,
                        journal_fence_start_date=NULL, journal_fence_end_date=NULL,
                        journal_fence_revision=NULL, journal_fence_actor=NULL WHERE id=?
                    """, id)).isInstanceOf(DataIntegrityViolationException.class);
        }
        assertThatThrownBy(() -> jdbc.update("""
                UPDATE closing_calendars SET journal_fence_operation_id='different-close' WHERE id=21
                """)).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT journal_fence_operation_id FROM closing_calendars WHERE id=21", String.class))
                .isEqualTo("cancel");

        clearTransitionAndFence(jdbc, 20);
        clearTransitionAndFence(jdbc, 21);
    }

    @Test
    void h2V59RetainsEveryTransitionStageAndReceiptProtocolThroughReopen() {
        DriverManagerDataSource source = new DriverManagerDataSource(
                "jdbc:h2:mem:receipt-stages-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(source).locations("classpath:db/closing-migration")
                .table("flyway_schema_history_closing").target("59").load().migrate();
        JdbcTemplate jdbc = new JdbcTemplate(source);
        insertFencedTransition(jdbc, 30, "CLOSED", "OPEN", "PREPARED", "reopen", "original-close", 7L);
        jdbc.update("UPDATE closing_calendars SET transition_protocol='RECEIPT_V1' WHERE id=30");
        for (String stage : new String[] {"PREPARED", "DISPATCHED", "AUTHORIZED", "READY_TO_CONFIRM", "RELEASE_PENDING"}) {
            String status = stage.equals("PREPARED") || stage.equals("DISPATCHED") ? "CLOSED" : "OPEN";
            jdbc.update("UPDATE closing_calendars SET transition_stage=?, status=? WHERE id=30", stage, status);
            assertThat(jdbc.queryForMap("SELECT status, transition_stage, transition_protocol, journal_fence_operation_id "
                    + "FROM closing_calendars WHERE id=30"))
                    .containsEntry("STATUS", status).containsEntry("TRANSITION_STAGE", stage)
                    .containsEntry("TRANSITION_PROTOCOL", "RECEIPT_V1")
                    .containsEntry("JOURNAL_FENCE_OPERATION_ID", "original-close");
        }
        insertFencedTransition(jdbc, 31, "IN_PROGRESS", "CLOSED", "PREPARED", "cancel", "cancel", null);
        jdbc.update("UPDATE closing_calendars SET transition_protocol='RECEIPT_V1', "
                + "transition_stage='CANCEL_RELEASE_PENDING' WHERE id=31");
        assertThat(jdbc.queryForMap("SELECT transition_stage, transition_protocol FROM closing_calendars WHERE id=31"))
                .containsEntry("TRANSITION_STAGE", "CANCEL_RELEASE_PENDING")
                .containsEntry("TRANSITION_PROTOCOL", "RECEIPT_V1");
        clearTransitionAndFence(jdbc, 30);
        clearTransitionAndFence(jdbc, 31);
    }

    private void insertFencedTransition(JdbcTemplate jdbc, long id, String status, String target, String stage,
            String transitionId, String fenceOperationId, Long approvalId) {
        LocalDate startDate = LocalDate.of(2050, (int) (id % 10) + 1, 1);
        jdbc.update("""
                INSERT INTO closing_calendars(id, fiscal_year, fiscal_period, status, is_current_period,
                    transition_id, transition_target, transition_stage, transition_fiscal_period_id,
                    transition_approval_id, transition_actor, transition_prepared_at,
                    journal_fence_operation_id, journal_fence_period_id, journal_fence_start_date,
                    journal_fence_end_date, journal_fence_revision, journal_fence_actor)
                VALUES (?, '2050', ?, ?, FALSE, ?, ?, ?, 1, ?, 'checker', CURRENT_TIMESTAMP,
                    ?, 1, ?, ?, 23, 'original-closer')
                """, id, String.format("%02d", startDate.getMonthValue()), status, transitionId, target, stage,
                approvalId, fenceOperationId, startDate, startDate.withDayOfMonth(startDate.lengthOfMonth()));
    }

    private void assertMigrationsApplied(JdbcTemplate jdbc) {
        for (String version : new String[] {"58", "59"}) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM \"flyway_schema_history_closing\" "
                    + "WHERE \"version\"=? AND \"success\"", Integer.class, version)).isEqualTo(1);
        }
    }

    private void clearTransitionAndFence(JdbcTemplate jdbc, long id) {
        assertThat(jdbc.update("""
                UPDATE closing_calendars SET transition_id=NULL, transition_target=NULL, transition_stage=NULL,
                    transition_fiscal_period_id=NULL, transition_approval_id=NULL, transition_actor=NULL,
                    transition_prepared_at=NULL, transition_evidence_set_id=NULL, transition_protocol=NULL,
                    journal_fence_operation_id=NULL, journal_fence_period_id=NULL, journal_fence_start_date=NULL,
                    journal_fence_end_date=NULL, journal_fence_revision=NULL, journal_fence_actor=NULL WHERE id=?
                """, id)).isEqualTo(1);
        assertThat(jdbc.queryForMap("""
                SELECT transition_id, transition_target, transition_stage, transition_fiscal_period_id,
                    transition_approval_id, transition_actor, transition_prepared_at, transition_evidence_set_id,
                    transition_protocol, journal_fence_operation_id, journal_fence_period_id,
                    journal_fence_start_date, journal_fence_end_date, journal_fence_revision, journal_fence_actor
                FROM closing_calendars WHERE id=?
                """, id).values()).allMatch(value -> value == null);
    }
}
