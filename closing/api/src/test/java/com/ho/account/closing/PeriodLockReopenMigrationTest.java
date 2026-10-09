package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** V54 must fail an ambiguous legacy upgrade instead of silently selecting a winner. */
class PeriodLockReopenMigrationTest {
    @Test
    void duplicateLegacyActiveLocksBlockUpgradeUntilExplicitRepair() {
        Fixture fixture = atV53();
        fixture.lock(11, 101);
        fixture.lock(12, 101);

        assertThatThrownBy(fixture::upgrade).hasRootCauseInstanceOf(java.sql.SQLException.class);
        assertThat(fixture.jdbc.queryForObject("SELECT COUNT(*) FROM period_locks WHERE fiscal_period_id=101", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void duplicateLegacyPendingApprovalsBlockUpgradeUntilExplicitRepair() {
        Fixture fixture = atV53();
        fixture.approval(21, 201, "PENDING");
        fixture.approval(22, 201, "PENDING");

        assertThatThrownBy(fixture::upgrade).hasRootCauseInstanceOf(java.sql.SQLException.class);
        assertThat(fixture.jdbc.queryForObject(
                "SELECT COUNT(*) FROM reopen_approvals WHERE fiscal_period_id=201 AND status='PENDING'", Integer.class))
                .isEqualTo(2);
    }

    @Test
    void cleanUpgradeRetainsHistoryAndEnforcesOnlyOneCurrentSlot() {
        Fixture fixture = atV53();
        fixture.lock(31, 301);
        fixture.approval(41, 401, "APPROVED");
        fixture.approval(42, 401, "PENDING");
        fixture.upgrade();
        JdbcTemplate jdbc = fixture.jdbc;

        assertThat(jdbc.queryForMap("SELECT active, active_fiscal_period_id FROM period_locks WHERE id=31"))
                .containsEntry("ACTIVE", true).containsEntry("ACTIVE_FISCAL_PERIOD_ID", 301L);
        assertThat(jdbc.queryForObject(
                "SELECT pending_fiscal_period_id FROM reopen_approvals WHERE id=41", Long.class)).isNull();
        assertThat(jdbc.queryForObject(
                "SELECT pending_fiscal_period_id FROM reopen_approvals WHERE id=42", Long.class)).isEqualTo(401L);

        assertThatThrownBy(() -> fixture.activeLock(32, 301)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> fixture.pendingApproval(43, 401))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE period_locks SET active=FALSE WHERE id=31"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "UPDATE reopen_approvals SET status='APPROVED' WHERE id=42"))
                .isInstanceOf(DataIntegrityViolationException.class);

        jdbc.update("UPDATE period_locks SET active=FALSE, active_fiscal_period_id=NULL, "
                + "unlocked_by='operator', unlocked_at=CURRENT_TIMESTAMP WHERE id=31");
        jdbc.update("UPDATE reopen_approvals SET status='REJECTED', pending_fiscal_period_id=NULL, "
                + "approved_by='checker', approved_at=CURRENT_TIMESTAMP WHERE id=42");
        fixture.activeLock(33, 301);
        fixture.pendingApproval(44, 401);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM period_locks WHERE fiscal_period_id=301", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM reopen_approvals WHERE fiscal_period_id=401", Integer.class))
                .isEqualTo(3);
    }

    private static Fixture atV53() {
        var source = new DriverManagerDataSource(
                "jdbc:h2:mem:period-slot-v54-" + UUID.randomUUID() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        Flyway.configure().dataSource(source).locations("classpath:db/closing-migration")
                .table("flyway_schema_history_closing").target("53").load().migrate();
        return new Fixture(source, new JdbcTemplate(source));
    }

    private record Fixture(DriverManagerDataSource source, JdbcTemplate jdbc) {
        void upgrade() {
            Flyway.configure().dataSource(source).locations("classpath:db/closing-migration")
                    .table("flyway_schema_history_closing").load().migrate();
        }

        void lock(long id, long periodId) {
            jdbc.update("INSERT INTO period_locks(id, fiscal_period_id, lock_type, locked_by, locked_at) "
                    + "VALUES (?, ?, 'ALL_TRANSACTIONS', 'operator', CURRENT_TIMESTAMP)", id, periodId);
        }

        void approval(long id, long periodId, String status) {
            jdbc.update("INSERT INTO reopen_approvals(id, fiscal_period_id, status, requested_by, requested_at) "
                    + "VALUES (?, ?, ?, 'requester', CURRENT_TIMESTAMP)", id, periodId, status);
        }

        void activeLock(long id, long periodId) {
            jdbc.update("INSERT INTO period_locks(id, fiscal_period_id, active, active_fiscal_period_id, "
                    + "lock_type, locked_by, locked_at) VALUES (?, ?, TRUE, ?, 'ALL_TRANSACTIONS', "
                    + "'operator', CURRENT_TIMESTAMP)", id, periodId, periodId);
        }

        void pendingApproval(long id, long periodId) {
            jdbc.update("INSERT INTO reopen_approvals(id, fiscal_period_id, pending_fiscal_period_id, "
                    + "status, requested_by, requested_at) VALUES (?, ?, ?, 'PENDING', 'requester', "
                    + "CURRENT_TIMESTAMP)", id, periodId, periodId);
        }
    }
}
