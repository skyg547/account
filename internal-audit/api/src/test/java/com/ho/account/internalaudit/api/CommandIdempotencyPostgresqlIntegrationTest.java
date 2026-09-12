package com.ho.account.internalaudit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.ho.account.internalaudit.core.application.AuditActorContext;
import com.ho.account.internalaudit.core.application.port.in.EvaluationUseCase;
import com.ho.account.internalaudit.core.application.port.in.RcmUseCase;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.IdempotencyConflictException;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import java.sql.Connection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Run only with the dedicated postgresTest task and an ephemeral loopback fixture.
 * No environmental database URL, credential file, or existing database is consulted.
 */
@Tag("postgresql")
class CommandIdempotencyPostgresqlIntegrationTest extends CommandIdempotencyIntegrationTest {
    @Autowired private RcmUseCase rcmService;
    @Autowired private EvaluationUseCase evaluationService;
    @Autowired private DataSource dataSource;
    @Autowired private Flyway flyway;

    @DynamicPropertySource
    static void useDedicatedPostgresql(DynamicPropertyRegistry properties) {
        String url = System.getProperty("internalAudit.test.postgresql.url", "");
        if (!url.matches("jdbc:postgresql://(127\\.0\\.0\\.1|localhost):[0-9]+/account_idempotency_test")) {
            throw new IllegalStateException("postgresTest requires the dedicated loopback account_idempotency_test fixture");
        }
        properties.add("spring.datasource.url", () -> url);
        properties.add("spring.datasource.username", () -> "account_test");
        properties.add("spring.datasource.password", () -> "");
        properties.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        properties.add("spring.flyway.locations", () -> "classpath:db/postgresql-migration");
        properties.add("spring.flyway.target", () -> "62");
        properties.add("spring.datasource.hikari.maximum-pool-size", () -> "5");
    }

    @Test
    void realPostgresqlMigrationsValidateAndPreseedEveryLockBucket() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(connection.getMetaData().getDatabaseMajorVersion()).isEqualTo(16);
            assertThat(connection.getTransactionIsolation()).isEqualTo(Connection.TRANSACTION_READ_COMMITTED);
        }
        flyway.validate();
        assertThat(flyway.info().current().getVersion().getVersion()).isEqualTo("62");
        assertThat(jdbc.queryForList("SELECT bucket FROM internal_audit_key_lock ORDER BY bucket", Integer.class))
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, 256).boxed().toList());
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void concurrentIdenticalFirstCommandsExecuteOnceAndReturnSameResult(Command command) throws Exception {
        Gate gate = new Gate();
        AtomicInteger saves = pauseFirstBusinessWrite(command, gate, false);

        List<Attempt> pair = race(
                () -> invokeService(command, command.effectiveBody(), "winner-trace"),
                () -> invokeService(command, command.effectiveBody(), "waiter-trace"), gate);

        assertThat(pair.get(0).failure).isNull();
        assertThat(pair.get(1).failure).isNull();
        assertThat(pair.get(1).result).isEqualTo(pair.get(0).result);
        assertThat(saves).hasValue(1);
        assertOneCommittedCommand(command, "winner-trace");
        assertThat(audits.findByCorrelationId("waiter-trace")).isEmpty();
        verify(receipts, times(1)).reserve(anyString(), anyInt(), anyString());
        verify(receipts, times(1)).complete(anyString(), anyInt(), anyString());
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void concurrentDifferentFirstCommandsCommitWinnerAndRejectWaiter(Command command) throws Exception {
        Gate gate = new Gate();
        AtomicInteger saves = pauseFirstBusinessWrite(command, gate, false);
        ObjectNode changed = command.effectiveBody().put(command.textField, "Losing changed payload");

        List<Attempt> pair = race(
                () -> invokeService(command, command.effectiveBody(), "winner-trace"),
                () -> invokeService(command, changed, "waiter-trace"), gate);

        assertThat(pair.get(0).failure).isNull();
        assertThat(pair.get(1).failure).isInstanceOf(IdempotencyConflictException.class);
        assertThat(saves).hasValue(1);
        assertOneCommittedCommand(command, "winner-trace");
        assertThat(jdbc.queryForObject("SELECT " + command.textColumn + " FROM " + command.table
                + " WHERE " + command.idColumn + "=?", String.class, "target-1")).isEqualTo("Original text");
        assertThat(audits.findByCorrelationId("waiter-trace")).isEmpty();
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void rolledBackWinnerReleasesReservationAndWaitingCommandSucceeds(Command command) throws Exception {
        Gate gate = new Gate();
        AtomicInteger saves = pauseFirstBusinessWrite(command, gate, true);

        List<Attempt> pair = race(
                () -> invokeService(command, command.effectiveBody(), "failed-trace"),
                () -> invokeService(command, command.effectiveBody(), "waiter-trace"), gate);

        assertThat(pair.get(0).failure).isInstanceOf(IllegalStateException.class)
                .hasMessage("synthetic-winning-transaction-failure");
        assertThat(pair.get(1).failure).isNull();
        assertThat(saves).hasValue(2);
        assertOneCommittedCommand(command, "waiter-trace");
        assertThat(audits.findByCorrelationId("failed-trace")).isEmpty();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM internal_audit_command_receipt WHERE snapshot_version=0",
                Long.class)).isZero();
    }

    @ParameterizedTest
    @EnumSource(Command.class)
    void directAppendWinningFirstUseMakesWaitingCommandALegacyConflict(Command command) throws Exception {
        Gate gate = new Gate();
        long businessBefore = rowCount(command.table);

        List<Attempt> pair = race(() -> new TransactionTemplate(transactionManager).execute(status -> {
            AuditLogEntry saved = audits.append(auditEvent(command, KEY, "legacy direct event"));
            entityManager.flush();
            gate.holdWinner();
            return saved;
        }), () -> invokeService(command, command.effectiveBody(), "waiter-trace"), gate);

        assertThat(pair.get(0).failure).isNull();
        assertThat(pair.get(1).failure).isInstanceOf(IdempotencyConflictException.class);
        assertThat(rowCount(command.table)).isEqualTo(businessBefore);
        assertThat(rowCount("internal_audit_log")).isEqualTo(1);
        assertThat(rowCount("internal_audit_command_receipt")).isZero();
        verify(receipts, never()).reserve(anyString(), anyInt(), anyString());
        verify(rcm, never()).saveProcess(any());
        verify(rcm, never()).saveRisk(any());
        verify(rcm, never()).saveControlActivity(any());
        verify(evaluations, never()).saveDesignEvaluation(any());
        verify(evaluations, never()).saveOperatingEvaluation(any());
        verify(evaluations, never()).saveDeficiency(any());
    }

    @ParameterizedTest
    @MethodSource("directAppendContenders")
    void commandWinnerSerializesEquivalentAndConflictingDirectAppends(Command command, boolean equivalent) throws Exception {
        Gate gate = new Gate();
        AtomicReference<AuditLogEntry> committedEvent = new AtomicReference<>();
        AtomicInteger appends = new AtomicInteger();
        doAnswer(invocation -> {
            AuditLogEntry saved = (AuditLogEntry) invocation.callRealMethod();
            if (appends.incrementAndGet() == 1) {
                committedEvent.set(saved);
                entityManager.flush();
                gate.holdWinner();
            }
            return saved;
        }).when(audits).append(any());

        List<Attempt> pair = race(
                () -> invokeService(command, command.effectiveBody(), "winner-trace"),
                () -> {
                    AuditLogEntry original = committedEvent.get();
                    AuditLogEntry retry = original.toBuilder().id(null).correlationId("direct-retry-trace")
                            .actionTimestamp(original.actionTimestamp().plusMinutes(1))
                            .detailsJson(equivalent ? original.detailsJson() : "different direct payload").build();
                    return new TransactionTemplate(transactionManager).execute(status -> audits.append(retry));
                }, gate);

        assertThat(pair.get(0).failure).isNull();
        if (equivalent) {
            assertThat(pair.get(1).failure).isNull();
            // PostgreSQL timestamps have microsecond precision after persistence.
            assertThat(pair.get(1).result).isEqualTo(audits.findByIdempotencyKey(KEY).orElseThrow());
        } else {
            assertThat(pair.get(1).failure).isInstanceOf(IdempotencyConflictException.class);
        }
        assertOneCommittedCommand(command, "winner-trace");
        assertThat(audits.findByCorrelationId("direct-retry-trace")).isEmpty();
    }

    private AtomicInteger pauseFirstBusinessWrite(Command command, Gate gate, boolean failWinner) {
        AtomicInteger saves = new AtomicInteger();
        interceptBusinessSave(command, invocation -> {
            Object saved = invocation.callRealMethod();
            entityManager.flush();
            if (saves.incrementAndGet() == 1) {
                gate.holdWinner();
                if (failWinner) {
                    throw new IllegalStateException("synthetic-winning-transaction-failure");
                }
            }
            return saved;
        });
        return saves;
    }

    private Object invokeService(Command command, ObjectNode payload, String trace) throws Exception {
        AuditActorContext.setActor(ACTOR);
        AuditActorContext.setIdempotencyKey(KEY);
        AuditActorContext.setCorrelationId(trace);
        try {
            return switch (command) {
                case PROCESS -> rcmService.createProcess(JSON.treeToValue(payload, RcmProcess.class));
                case RISK -> rcmService.addRisk(command.parentId, JSON.treeToValue(payload, RcmRisk.class));
                case CONTROL -> rcmService.addControl(command.parentId, JSON.treeToValue(payload, ControlActivity.class));
                case DESIGN -> evaluationService.submitDesignEvaluation(JSON.treeToValue(payload, DesignEvaluation.class));
                case OPERATING -> evaluationService.submitOperatingEvaluation(JSON.treeToValue(payload, OperatingEvaluation.class));
                case DEFICIENCY -> evaluationService.registerDeficiency(JSON.treeToValue(payload, Deficiency.class));
            };
        } finally {
            AuditActorContext.clear();
        }
    }

    private List<Attempt> race(Callable<?> winner, Callable<?> contender, Gate gate) throws Exception {
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> first = workers.submit(() -> attempt(winner));
            assertThat(gate.winnerReady.await(10, TimeUnit.SECONDS)).as("winner holds transaction lock").isTrue();
            Future<Attempt> second = workers.submit(() -> attempt(contender));
            awaitRealPostgresqlLockWait(second);
            gate.releaseWinner.countDown();
            return List.of(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
        } finally {
            gate.releaseWinner.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS)).as("database worker cleanup").isTrue();
        }
    }

    private void awaitRealPostgresqlLockWait(Future<?> contender) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            assertThat(contender.isDone()).as("contender must wait while winner owns the key").isFalse();
            Long waiting = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM pg_stat_activity
                    WHERE datname = current_database() AND wait_event_type = 'Lock'
                      AND query LIKE '%internal_audit_key_lock%'
                    """, Long.class);
            if (waiting != null && waiting > 0) {
                return;
            }
            Thread.sleep(20);
        }
        throw new AssertionError("No PostgreSQL transaction waiting on the shared idempotency lock was observed");
    }

    private Attempt attempt(Callable<?> operation) {
        try {
            return new Attempt(operation.call(), null);
        } catch (Exception failure) {
            return new Attempt(null, failure);
        }
    }

    private void assertOneCommittedCommand(Command command, String trace) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + command.table + " WHERE " + command.idColumn + "=?",
                Long.class, "target-1")).isEqualTo(1);
        assertThat(rowCount("internal_audit_log")).isEqualTo(1);
        assertThat(rowCount("internal_audit_command_receipt")).isEqualTo(1);
        assertThat(audits.findByCorrelationId(trace)).singleElement().satisfies(entry -> {
            assertThat(entry.actor()).isEqualTo(ACTOR);
            assertThat(entry.action()).isEqualTo(command.action);
            assertThat(entry.aggregateType()).isEqualTo(command.aggregateType);
            assertThat(entry.idempotencyKey()).isEqualTo(KEY);
        });
    }

    static Stream<Arguments> directAppendContenders() {
        return Stream.of(Command.values()).flatMap(command -> Stream.of(true, false)
                .map(equivalent -> Arguments.of(command, equivalent)));
    }

    private record Attempt(Object result, Exception failure) { }

    private static final class Gate {
        private final CountDownLatch winnerReady = new CountDownLatch(1);
        private final CountDownLatch releaseWinner = new CountDownLatch(1);

        void holdWinner() {
            winnerReady.countDown();
            try {
                if (!releaseWinner.await(20, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("Timed out waiting for the test to release the winning transaction");
                }
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Winning transaction interrupted", interrupted);
            }
        }
    }
}
