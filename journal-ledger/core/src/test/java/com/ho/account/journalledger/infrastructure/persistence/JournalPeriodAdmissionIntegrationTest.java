package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.contracts.journal.*;
import java.time.LocalDate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Runs on H2 or the explicitly supplied synthetic JOURNAL_POSTING_TEST_POSTGRES_URL. */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JournalPeriodAdmissionIntegrationTest {
    private static final LocalDate START = LocalDate.of(2026, 9, 1);
    private static final JournalPeriodScope SCOPE = new JournalPeriodScope(762L, START, START.plusDays(29));
    private JdbcTemplate jdbc;
    private DataSourceTransactionManager manager;
    private TransactionTemplate tx;
    private JdbcJournalPeriodAdmissionAdapter admission;

    @BeforeAll
    void createDatabase() {
        PostingTestDatabase database = PostingTestDatabase.create();
        var source = database.dataSource();
        jdbc = new JdbcTemplate(source);
        jdbc.execute("CREATE SCHEMA IF NOT EXISTS " + database.schema());
        // These controls do not depend on business tables. Execute the production DDL once while
        // the full posting fixtures retain Flyway coverage of the complete migration chain.
        new ResourceDatabasePopulator(
                new ClassPathResource("db/journal-migration/V15__ledger_reaggregation_control.sql"),
                new ClassPathResource("db/journal-migration/V20__journal_period_admission.sql"))
                .execute(source);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_monetary_revision", Integer.class)).isOne();
        assertThat(revision()).isZero();
        manager = new DataSourceTransactionManager(source);
        tx = new TransactionTemplate(manager);
        admission = recreate();
    }

    @BeforeEach
    void resetControlState() {
        jdbc.update("DELETE FROM journal_period_fences");
        jdbc.update("DELETE FROM journal_monetary_revision");
        jdbc.update("INSERT INTO journal_monetary_revision VALUES (1, 0)");
        jdbc.update("DELETE FROM ledger_reaggregation_control");
        jdbc.update("INSERT INTO ledger_reaggregation_control (control_id, status, epoch) VALUES (1, 'OPEN', 0)");
        tx = new TransactionTemplate(manager);
        admission = recreate();
    }

    @Test
    void migrationCreatesConstrainedSingletonAndImmutablePayloadColumns() {
        assertThat(revision()).isZero();
        assertThatThrownBy(() -> jdbc.update("INSERT INTO journal_monetary_revision VALUES (2, 0)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE journal_monetary_revision SET revision = -1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        var command = command("valid");
        admission.fence(command);
        assertThatThrownBy(() -> jdbc.update("UPDATE journal_period_fences SET state = 'RELEASED'"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void checkpointWaitsForAdmittedMovementCommitBeforeReturningRevision() throws Exception {
        var writerAdmitted = new java.util.concurrent.CountDownLatch(1);
        var allowCommit = new java.util.concurrent.CountDownLatch(1);
        var checkpointStarted = new java.util.concurrent.CountDownLatch(1);
        var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            var writer = executor.submit(() -> tx.executeWithoutResult(status -> {
                admission.admit(START);
                writerAdmitted.countDown();
                await(allowCommit);
            }));
            await(writerAdmitted);
            var checkpoint = executor.submit(() -> {
                checkpointStarted.countDown();
                return admission.checkpoint(SCOPE);
            });
            await(checkpointStarted);
            assertThatThrownBy(() -> checkpoint.get(300, java.util.concurrent.TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            allowCommit.countDown();
            writer.get(15, java.util.concurrent.TimeUnit.SECONDS);
            assertThat(checkpoint.get(15, java.util.concurrent.TimeUnit.SECONDS).revision()).isEqualTo(1);
        } finally {
            allowCommit.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }
    }

    private static void await(java.util.concurrent.CountDownLatch latch) {
        try {
            if (!latch.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new IllegalStateException("Journal admission test barrier timed out");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Journal admission test barrier interrupted", interrupted);
        }
    }

    @Test
    void activeReplaySurvivesRevisionChangeAndAdapterRecreation() {
        var command = command("active");
        var receipt = admission.fence(command);
        // A future movement is allowed but conservatively changes the global revision.
        tx.executeWithoutResult(status -> admission.admit(SCOPE.endDate().plusDays(1)));
        assertThat(revision()).isGreaterThan(command.checkpoint().revision());
        assertThat(recreate().fence(command)).isEqualTo(receipt);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> recreate().admit(START.minusYears(1))))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("fenced");
    }

    @Test
    void staleCheckpointRejectsAndRolledBackMovementDoesNotAdvanceRevision() {
        var command = command("stale");
        tx.executeWithoutResult(status -> {
            admission.admit(START);
            status.setRollbackOnly();
        });
        assertThat(revision()).isEqualTo(command.checkpoint().revision());
        tx.executeWithoutResult(status -> admission.admit(START));
        assertThatThrownBy(() -> admission.fence(command)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("changed after checkpoint");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_period_fences", Integer.class)).isZero();
    }

    @Test
    void everyImmutablePayloadFieldRejectsConflictingReplayAndRelease() {
        var command = command("same-operation");
        admission.fence(command);
        var changedScope = new JournalPeriodScope(763L, START, SCOPE.endDate());
        var changedDates = new JournalPeriodScope(762L, START.minusDays(1), SCOPE.endDate());
        var changedEnd = new JournalPeriodScope(762L, START, SCOPE.endDate().plusDays(1));
        for (var conflicting : java.util.List.of(
                new JournalPeriodFenceCommand(command.operationId(), command.checkpoint(), "other-actor"),
                new JournalPeriodFenceCommand(command.operationId(), new JournalPeriodCheckpoint(changedScope, 0), "closer"),
                new JournalPeriodFenceCommand(command.operationId(), new JournalPeriodCheckpoint(changedDates, 0), "closer"),
                new JournalPeriodFenceCommand(command.operationId(), new JournalPeriodCheckpoint(changedEnd, 0), "closer"),
                new JournalPeriodFenceCommand(command.operationId(), new JournalPeriodCheckpoint(SCOPE, 1), "closer"))) {
            assertThatThrownBy(() -> admission.fence(conflicting)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("immutable command");
            assertThatThrownBy(() -> admission.release(conflicting)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("immutable command");
        }
        assertThat(admission.fence(command).state()).isEqualTo(JournalPeriodFenceResult.State.ACTIVE);
    }

    @Test
    void releaseBeforeDelayedFenceCreatesDurableTombstoneAndInvalidatesOtherCheckpoint() {
        var command = command("uncertain-fence");
        var other = command("older-validation");
        var receipt = admission.release(command);
        long revisionAfterRelease = revision();
        assertThatThrownBy(() -> recreate().fence(command)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("was released");
        assertThat(recreate().release(command)).isEqualTo(receipt);
        assertThat(revision()).isEqualTo(revisionAfterRelease);
        assertThat(receipt.state()).isEqualTo(JournalPeriodFenceResult.State.RELEASED);
        assertThatThrownBy(() -> admission.fence(other)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("changed after checkpoint");
        tx.executeWithoutResult(status -> admission.admit(START));
    }

    @Test
    void oldReleaseCannotRemoveNewFenceForSamePeriod() {
        var first = command("first");
        admission.fence(first);
        admission.release(first);
        var second = command("second");
        admission.fence(second);
        admission.release(first);
        assertThatThrownBy(() -> recreate().fence(first)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("was released");
        assertThat(recreate().fence(second).state()).isEqualTo(JournalPeriodFenceResult.State.ACTIVE);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> admission.admit(START)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("fenced");
    }

    @Test
    void controlCommitsSurviveRollbackOfCallingTransaction() {
        var command = command("independent-commit");
        tx.executeWithoutResult(status -> {
            admission.fence(command);
            status.setRollbackOnly();
        });
        assertThat(recreate().fence(command).state()).isEqualTo(JournalPeriodFenceResult.State.ACTIVE);
        tx.executeWithoutResult(status -> {
            admission.release(command);
            status.setRollbackOnly();
        });
        assertThat(recreate().release(command).state()).isEqualTo(JournalPeriodFenceResult.State.RELEASED);
        assertThatThrownBy(() -> recreate().fence(command)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("was released");
    }

    @Test
    void missingRevisionAndReaggregationControlFailClosed() {
        var command = command("missing");
        jdbc.update("DELETE FROM journal_monetary_revision");
        assertThatThrownBy(() -> admission.checkpoint(SCOPE)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> admission.fence(command)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> admission.release(command)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> admission.admit(START)))
                .isInstanceOf(IllegalStateException.class);
        jdbc.update("INSERT INTO journal_monetary_revision VALUES (1, 0)");
        jdbc.update("DELETE FROM ledger_reaggregation_control");
        assertThatThrownBy(() -> admission.checkpoint(SCOPE)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> admission.fence(command)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> admission.admit(START)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rebuildBlocksNewEvidenceAndChangesRevisionAtStartAndPublication() {
        var command = command("before-rebuild");
        var control = new JdbcBalanceReaggregationControlAdapter(jdbc);
        tx.executeWithoutResult(status -> {
            admission.beginReaggregation(START);
            control.start(762, START, SCOPE.endDate());
        });
        assertThatThrownBy(() -> admission.checkpoint(SCOPE)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reaggregation");
        assertThatThrownBy(() -> admission.fence(command)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reaggregation");
        tx.executeWithoutResult(status -> {
            admission.completeReaggregation();
            control.release(762, START, SCOPE.endDate());
        });
        assertThat(revision()).isEqualTo(command.checkpoint().revision() + 2);
        assertThatThrownBy(() -> admission.fence(command)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("changed after checkpoint");
    }

    @Test
    void monetaryAdmissionRequiresActualReadCommittedWriteTransaction() {
        assertThatThrownBy(() -> admission.admit(START)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active write transaction");
        tx.setReadOnly(true);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> admission.admit(START)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("active write transaction");
        tx.setReadOnly(false);
        tx.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> admission.admit(START)))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("READ_COMMITTED");
    }

    private long revision() {
        return jdbc.queryForObject("SELECT revision FROM journal_monetary_revision WHERE control_id = 1", Long.class);
    }

    private JournalPeriodFenceCommand command(String operation) {
        return new JournalPeriodFenceCommand(operation, admission.checkpoint(SCOPE), "closer");
    }

    private JdbcJournalPeriodAdmissionAdapter recreate() {
        return new JdbcJournalPeriodAdmissionAdapter(jdbc, new JdbcBalanceReaggregationControlAdapter(jdbc), manager);
    }
}
