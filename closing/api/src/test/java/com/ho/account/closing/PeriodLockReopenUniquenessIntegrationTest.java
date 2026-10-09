package com.ho.account.closing;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.closing.ClosingAggregateConcurrencyIntegrationTest.ControlledMaster;
import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.PeriodLock.PeriodLockType;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.web.ClosingController;
import com.ho.account.closing.web.ClosingExceptionHandler;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Separate command transactions race on one period; queries inspect only committed rows. */
@ActiveProfiles("dev")
@SpringBootTest(classes = {ClosingApplication.class, ClosingAggregateConcurrencyIntegrationTest.Configuration.class},
        properties = {
                "spring.cloud.config.enabled=false", "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false", "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false", "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:closing-lock-reopen-uniqueness;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=validate", "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/closing-migration",
                "spring.flyway.table=flyway_schema_history_closing",
                "spring.flyway.baseline-on-migrate=false", "spring.sql.init.mode=never",
                "closing.sources.enabled=false", "closing.master-data.remote.enabled=true",
                "closing.master-data.base-url=http://master-data.test",
                "closing.journal-ledger.base-url=http://journal-ledger.test"
        })
class PeriodLockReopenUniquenessIntegrationTest {
    @Autowired ClosingUseCase closing;
    @Autowired ClosingCalendarPersistencePort calendars;
    @Autowired ControlledMaster master;
    @Autowired JdbcTemplate jdbc;
    @Autowired ClosingController controller;

    @Test
    void concurrentLocksLeaveOneActiveRowAndUnlockKeepsItsHistory() throws Exception {
        assertV54Applied();
        long periodId = period("2060", "01", "OPEN");
        List<Throwable> outcomes = race(() -> closing.lockPeriod(periodId, PeriodLockType.ALL_TRANSACTIONS,
                "first", "month control"), () -> closing.lockPeriod(periodId, PeriodLockType.PARTIAL_LOCK,
                "second", "month control"));

        assertOneSuccessOneConflict(outcomes);
        assertThat(count("period_locks", "active=TRUE", periodId)).isEqualTo(1);
        assertConflict("/api/closing/period-locks", "{\"fiscalPeriodId\":" + periodId
                + ",\"lockType\":\"ALL_TRANSACTIONS\",\"reason\":\"duplicate\"}");
        closing.unlockPeriod(periodId, "releaser");
        assertThat(count("period_locks", "active=TRUE", periodId)).isZero();
        assertThat(count("period_locks", "active=FALSE", periodId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT unlocked_by FROM period_locks WHERE fiscal_period_id=?",
                String.class, periodId)).isEqualTo("releaser");
        assertThat(jdbc.queryForObject("SELECT unlocked_at FROM period_locks WHERE fiscal_period_id=?",
                java.sql.Timestamp.class, periodId)).isNotNull();

        closing.lockPeriod(periodId, PeriodLockType.ALL_TRANSACTIONS, "next", "next review");
        assertThat(count("period_locks", "active=TRUE", periodId)).isEqualTo(1);
        assertThat(count("period_locks", "active=FALSE", periodId)).isEqualTo(1);
    }

    @Test
    void concurrentReopenRequestsLeaveOnePendingAndDecisionKeepsHistory() throws Exception {
        assertV54Applied();
        long periodId = period("2060", "02", "CLOSED");
        List<Throwable> outcomes = race(() -> closing.requestPeriodReopen(periodId, "first", "correction one"),
                () -> closing.requestPeriodReopen(periodId, "second", "correction two"));

        assertOneSuccessOneConflict(outcomes);
        assertThat(count("reopen_approvals", "status='PENDING'", periodId)).isEqualTo(1);
        assertConflict("/api/closing/reopen-approvals", "{\"fiscalPeriodId\":" + periodId
                + ",\"reason\":\"duplicate\"}");
        Long approvalId = jdbc.queryForObject(
                "SELECT id FROM reopen_approvals WHERE fiscal_period_id=? AND status='PENDING'", Long.class, periodId);
        closing.updateReopenApprovalStatus(approvalId, ReopenApprovalStatus.REJECTED, "checker");
        assertThat(count("reopen_approvals", "status='PENDING'", periodId)).isZero();
        assertThat(count("reopen_approvals", "status='REJECTED'", periodId)).isEqualTo(1);
        closing.requestPeriodReopen(periodId, "next", "new correction");
        assertThat(count("reopen_approvals", "status='PENDING'", periodId)).isEqualTo(1);
        assertThat(count("reopen_approvals", "status='REJECTED'", periodId)).isEqualTo(1);
    }

    @Test
    void migratedDatabaseRejectsConcurrentDirectInsertsForBothUniqueSlots() throws Exception {
        assertV54Applied();
        long lockPeriodId = 306003L;
        List<Throwable> locks = race(() -> jdbc.update("""
                INSERT INTO period_locks(fiscal_period_id, active, active_fiscal_period_id,
                    lock_type, locked_by, locked_at)
                VALUES (?, TRUE, ?, 'ALL_TRANSACTIONS', 'one', CURRENT_TIMESTAMP)
                """, lockPeriodId, lockPeriodId), () -> jdbc.update("""
                INSERT INTO period_locks(fiscal_period_id, active, active_fiscal_period_id,
                    lock_type, locked_by, locked_at)
                VALUES (?, TRUE, ?, 'ALL_TRANSACTIONS', 'two', CURRENT_TIMESTAMP)
                """, lockPeriodId, lockPeriodId));
        assertOneSuccessOneDatabaseConflict(locks);
        assertThat(count("period_locks", "active=TRUE", lockPeriodId)).isEqualTo(1);

        long reopenPeriodId = 306004L;
        List<Throwable> approvals = race(() -> jdbc.update("""
                INSERT INTO reopen_approvals(fiscal_period_id, pending_fiscal_period_id,
                    status, requested_by, requested_at)
                VALUES (?, ?, 'PENDING', 'one', CURRENT_TIMESTAMP)
                """, reopenPeriodId, reopenPeriodId), () -> jdbc.update("""
                INSERT INTO reopen_approvals(fiscal_period_id, pending_fiscal_period_id,
                    status, requested_by, requested_at)
                VALUES (?, ?, 'PENDING', 'two', CURRENT_TIMESTAMP)
                """, reopenPeriodId, reopenPeriodId));
        assertOneSuccessOneDatabaseConflict(approvals);
        assertThat(count("reopen_approvals", "status='PENDING'", reopenPeriodId)).isEqualTo(1);
    }

    private void assertV54Applied() {
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM flyway_schema_history_closing
                WHERE version='54' AND success=TRUE
                """, Integer.class)).isEqualTo(1);
    }

    private long period(String year, String month, String status) {
        long id = master.create(year, month, status);
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear(year);
        calendar.setFiscalPeriod(month);
        calendar.setStatus("CLOSED".equals(status) ? ClosingCalendarStatus.CLOSED : ClosingCalendarStatus.OPEN);
        calendar.setAuditUser("fixture");
        calendars.save(calendar);
        return id;
    }

    private int count(String table, String predicate, long periodId) {
        // Table and predicate are fixed literals at call sites; only the period is variable.
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE fiscal_period_id=? AND " + predicate,
                Integer.class, periodId);
    }

    private void assertConflict(String path, String body) throws Exception {
        MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ClosingExceptionHandler()).build()
                .perform(post(path).header("X-Auth-User", "rival")
                        .header("X-Auth-Roles", "CLOSING_MANAGER")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKFLOW_STATE_CONFLICT"));
    }

    private static List<Throwable> race(Runnable first, Runnable second) throws Exception {
        var executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        try {
            Future<Throwable> one = executor.submit(() -> runAtBarrier(first, ready, go));
            Future<Throwable> two = executor.submit(() -> runAtBarrier(second, ready, go));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            return java.util.Arrays.asList(one.get(15, TimeUnit.SECONDS), two.get(15, TimeUnit.SECONDS));
        } finally {
            go.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static Throwable runAtBarrier(Runnable command, CountDownLatch ready, CountDownLatch go) {
        ready.countDown();
        try {
            if (!go.await(5, TimeUnit.SECONDS)) throw new AssertionError("Start barrier timed out");
            command.run();
            return null;
        } catch (Throwable failure) {
            return failure;
        }
    }

    private static void assertOneSuccessOneConflict(List<Throwable> outcomes) {
        assertThat(outcomes.stream().filter(result -> result == null).toList()).hasSize(1);
        assertThat(outcomes.stream().filter(result -> result instanceof IllegalStateException).toList()).hasSize(1);
    }

    private static void assertOneSuccessOneDatabaseConflict(List<Throwable> outcomes) {
        assertThat(outcomes.stream().filter(result -> result == null).toList()).hasSize(1);
        assertThat(outcomes.stream().filter(result -> result instanceof DataIntegrityViolationException).toList())
                .hasSize(1);
    }
}
