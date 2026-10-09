package com.ho.account.auth.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.application.port.out.LoginAttemptPort;
import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Exercises committed, separate transactions against PostgreSQL's real unique-key and row locking rules. */
@DataJpaTest(properties = {"spring.flyway.enabled=false", "spring.jpa.hibernate.ddl-auto=create"})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
@ContextConfiguration(classes = JpaLoginAttemptPostgresConcurrencyTest.JpaTestConfiguration.class)
class JpaLoginAttemptPostgresConcurrencyTest {

    private static final EmbeddedPostgres POSTGRES = startPostgres();
    private static final Instant START = Instant.parse("2026-06-18T00:00:00Z");

    @Autowired private LoginAttemptJpaRepository repository;
    @Autowired private LoginAttemptPort adapter;
    @Autowired private MutableClock clock;
    @Autowired private TransactionTemplate transactions;

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> POSTGRES.getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @AfterEach
    void clearRows() {
        repository.deleteAll();
        clock.set(START);
    }

    @AfterAll
    static void stopPostgres() throws IOException {
        POSTGRES.close();
    }

    @Test
    void concurrentFirstFailuresAllCommitExactlyOnceAndReachThreshold() throws Exception {
        clock.set(START);
        runTogether(8, () -> adapter.recordFailure("  Synthetic_First  ", "INVALID_PASSWORD"));

        LoginAttemptJpaEntity state = repository.findById("synthetic_first").orElseThrow();
        assertThat(state.getFailureCount()).isEqualTo(8);
        assertThat(state.getLockedUntil()).isEqualTo(START.plusSeconds(15 * 60));
    }

    @Test
    void concurrentExistingRowFailuresDoNotLoseUpdatesAtThreshold() throws Exception {
        clock.set(START);
        adapter.recordFailure("synthetic_existing", "INVALID_PASSWORD");
        runTogether(3, () -> adapter.recordFailure("synthetic_existing", "INVALID_PASSWORD"));
        assertThat(repository.findById("synthetic_existing").orElseThrow().getFailureCount()).isEqualTo(4);
        assertThat(adapter.isLocked("synthetic_existing")).isFalse();

        adapter.recordFailure("synthetic_existing", "INVALID_PASSWORD");

        LoginAttemptJpaEntity state = repository.findById("synthetic_existing").orElseThrow();
        assertThat(state.getFailureCount()).isEqualTo(5);
        assertThat(state.getLockedUntil()).isEqualTo(START.plusSeconds(15 * 60));
        assertThat(adapter.isLocked("synthetic_existing")).isTrue();
    }

    @Test
    void simultaneousFailuresAfterExpiredLockBeginFreshWindowWithoutLostUpdates() throws Exception {
        clock.set(START);
        for (int i = 0; i < 5; i++) {
            adapter.recordFailure("synthetic_expired", "INVALID_PASSWORD");
        }
        assertThat(adapter.isLocked("synthetic_expired")).isTrue();

        clock.set(START.plusSeconds(15 * 60));
        runTogether(4, () -> adapter.recordFailure("synthetic_expired", "INVALID_PASSWORD"));

        LoginAttemptJpaEntity state = repository.findById("synthetic_expired").orElseThrow();
        assertThat(state.getFailureCount()).isEqualTo(4);
        assertThat(state.getLockedUntil()).isNull();
        assertThat(adapter.isLocked("synthetic_expired")).isFalse();
    }

    @Test
    void successCannotClearAnActiveRefreshedLock() {
        clock.set(START);
        for (int i = 0; i < 4; i++) {
            adapter.recordFailure("synthetic_race", "INVALID_PASSWORD");
        }
        adapter.recordFailure("synthetic_race", "INVALID_PASSWORD");
        assertThat(adapter.isLocked("synthetic_race")).isTrue();

        adapter.recordSuccess("synthetic_race");
        assertThat(adapter.isLocked("synthetic_race")).isTrue();
        assertThat(repository.findById("synthetic_race").orElseThrow().getFailureCount()).isEqualTo(5);
    }

    @Test
    void successClearsAnUnlockedFailureRow() {
        adapter.recordFailure("synthetic_success_reset", "INVALID_PASSWORD");
        assertThat(repository.findById("synthetic_success_reset").orElseThrow().getFailureCount()).isEqualTo(1);

        adapter.recordSuccess("synthetic_success_reset");

        assertThat(repository.findById("synthetic_success_reset")).isEmpty();
        assertThat(adapter.isLocked("synthetic_success_reset")).isFalse();
    }

    @Test
    void failureAfterConcurrentSuccessDeletePersistsAsNewFirstAttempt() throws Exception {
        adapter.recordFailure("synthetic_deleted_race", "INVALID_PASSWORD");
        CountDownLatch successDeleteFlushed = new CountDownLatch(1);
        CountDownLatch releaseSuccess = new CountDownLatch(1);
        CountDownLatch failureStarted = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> success = pool.submit(() -> transactions.executeWithoutResult(status -> {
                adapter.recordSuccess("synthetic_deleted_race");
                repository.flush();
                successDeleteFlushed.countDown();
                await(releaseSuccess);
            }));
            assertThat(successDeleteFlushed.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> failure = pool.submit(() -> {
                failureStarted.countDown();
                adapter.recordFailure("synthetic_deleted_race", "INVALID_PASSWORD");
            });
            assertThat(failureStarted.await(10, TimeUnit.SECONDS)).isTrue();
            // The DELETE is uncommitted; a separate failure transaction must wait for its row lock.
            assertThatThrownBy(() -> failure.get(250, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            releaseSuccess.countDown();
            success.get(20, TimeUnit.SECONDS);
            failure.get(20, TimeUnit.SECONDS);

            LoginAttemptJpaEntity state = repository.findById("synthetic_deleted_race").orElseThrow();
            assertThat(state.getFailureCount()).isEqualTo(1);
            assertThat(state.getLockedUntil()).isNull();
            assertThat(adapter.isLocked("synthetic_deleted_race")).isFalse();
        } finally {
            releaseSuccess.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void successWaitingOnConcurrentThresholdFailureCannotEraseCommittedLock() throws Exception {
        clock.set(START);
        for (int i = 0; i < 4; i++) {
            adapter.recordFailure("synthetic_interleaved", "INVALID_PASSWORD");
        }
        CountDownLatch failureWritten = new CountDownLatch(1);
        CountDownLatch releaseFailure = new CountDownLatch(1);
        CountDownLatch successStarted = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> failure = pool.submit(() -> transactions.executeWithoutResult(status -> {
                adapter.recordFailure("synthetic_interleaved", "INVALID_PASSWORD");
                repository.flush();
                failureWritten.countDown();
                await(releaseFailure);
            }));
            assertThat(failureWritten.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> success = pool.submit(() -> {
                successStarted.countDown();
                adapter.recordSuccess("synthetic_interleaved");
            });
            assertThat(successStarted.await(10, TimeUnit.SECONDS)).isTrue();
            // The success call is inside its own transaction while PostgreSQL holds the failure row.
            assertThat(success.isDone()).isFalse();
            releaseFailure.countDown();
            failure.get(20, TimeUnit.SECONDS);
            success.get(20, TimeUnit.SECONDS);
            assertThat(adapter.isLocked("synthetic_interleaved")).isTrue();
            assertThat(repository.findById("synthetic_interleaved").orElseThrow().getFailureCount()).isEqualTo(5);
        } finally {
            releaseFailure.countDown();
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new AssertionError("transaction barrier timed out");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("transaction barrier interrupted", e);
        }
    }

    private static void runTogether(int workers, Runnable operation) throws Exception {
        List<Callable<Void>> calls = new ArrayList<>();
        for (int i = 0; i < workers; i++) {
            calls.add(() -> { operation.run(); return null; });
        }
        runTogether(calls);
    }

    private static void runTogether(List<Callable<Void>> calls) throws Exception {
        CountDownLatch ready = new CountDownLatch(calls.size());
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(calls.size());
        try {
            List<Future<Void>> results = new ArrayList<>();
            for (Callable<Void> call : calls) {
                results.add(pool.submit(() -> {
                    ready.countDown();
                    if (!start.await(10, TimeUnit.SECONDS)) throw new AssertionError("start barrier timed out");
                    return call.call();
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<Void> result : results) {
                result.get(20, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static EmbeddedPostgres startPostgres() {
        try {
            return EmbeddedPostgres.builder().start();
        } catch (IOException e) {
            throw new IllegalStateException("Cannot start embedded PostgreSQL", e);
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = LoginAttemptJpaRepository.class)
    @EntityScan(basePackageClasses = LoginAttemptJpaEntity.class)
    static class JpaTestConfiguration {
        @Bean MutableClock mutableClock() { return new MutableClock(); }
        @Bean JpaLoginAttemptAdapter loginAttemptAdapter(LoginAttemptJpaRepository repository, MutableClock clock) {
            return new JpaLoginAttemptAdapter(repository, 5, 15, clock);
        }
        @Bean TransactionTemplate transactionTemplate(PlatformTransactionManager manager) {
            return new TransactionTemplate(manager);
        }
    }

    static class MutableClock extends Clock {
        private final AtomicReference<Instant> instant = new AtomicReference<>(START);
        void set(Instant value) { instant.set(value); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zone) { return Clock.fixed(instant(), zone); }
        @Override public Instant instant() { return instant.get(); }
    }
}
