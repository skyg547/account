package com.ho.account.internalaudit.core.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ho.account.internalaudit.core.application.port.out.CommandReceiptPort;
import com.ho.account.internalaudit.core.domain.CommandReceipt;
import com.ho.account.internalaudit.core.infrastructure.persistence.adapter.CommandReceiptPersistenceAdapter;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.AnnotationTransactionAttributeSource;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.transaction.support.TransactionTemplate;

class CommandReceiptPersistenceAdapterTest {

    private static final String FINGERPRINT = "a".repeat(64);
    private static final String SNAPSHOT = "{\"id\":1,\"name\":\"first\"}";

    private JdbcTemplate jdbcTemplate;
    private TransactionTemplate transactions;
    private CommandReceiptPort adapter;

    @BeforeEach
    void setUp() {
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:receipt-" + UUID.randomUUID()
                        + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000", "sa", "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        new ResourceDatabasePopulator(new ClassPathResource(
                "db/migration/V62__create_internal_audit_command_receipts.sql")).execute(dataSource);
        var transactionManager = new DataSourceTransactionManager(dataSource);
        transactions = new TransactionTemplate(transactionManager);
        ProxyFactory proxy = new ProxyFactory(new CommandReceiptPersistenceAdapter(jdbcTemplate));
        proxy.addAdvice(new TransactionInterceptor(transactionManager, new AnnotationTransactionAttributeSource()));
        adapter = (CommandReceiptPort) proxy.getProxy();
    }

    @AfterEach
    void closeDatabase() {
        jdbcTemplate.execute("SHUTDOWN");
    }

    @Test
    void migrationPreseedsEveryBucketAndBothDialectsHaveIdenticalSql() throws Exception {
        assertThat(jdbcTemplate.queryForList(
                "SELECT bucket FROM internal_audit_key_lock ORDER BY bucket", Integer.class))
                .containsExactlyElementsOf(java.util.stream.IntStream.range(0, 256).boxed().toList());
        try (var h2 = new ClassPathResource(
                "db/migration/V62__create_internal_audit_command_receipts.sql").getInputStream();
             var postgres = new ClassPathResource(
                     "db/postgresql-migration/V62__create_internal_audit_command_receipts.sql").getInputStream()) {
            assertThat(h2.readAllBytes()).isEqualTo(postgres.readAllBytes());
        }
    }

    @Test
    void reserveAndCompletePersistTheNormalizedKeyAndExactFirstSnapshot() {
        transactions.executeWithoutResult(status -> {
            adapter.lockKey(" receipt-key ");
            assertThat(adapter.findByKey("receipt-key")).isEmpty();
            adapter.reserve(" receipt-key ", 1, FINGERPRINT);
            assertThat(adapter.findByKey("receipt-key")).contains(
                    new CommandReceipt("receipt-key", 1, FINGERPRINT, 0, null));
            adapter.complete(" receipt-key ", 1, SNAPSHOT);
        });

        assertThat(readReceipt(" receipt-key ")).contains(
                new CommandReceipt("receipt-key", 1, FINGERPRINT, 1, SNAPSHOT));
    }

    @Test
    void completionCanNeitherCreateAMissingReceiptNorOverwriteTheFirstResult() {
        assertThatThrownBy(() -> transactions.executeWithoutResult(status ->
                adapter.complete("missing", 1, SNAPSHOT))).isInstanceOf(IllegalStateException.class);
        transactions.executeWithoutResult(status -> {
            adapter.lockKey("receipt-key");
            adapter.reserve("receipt-key", 1, FINGERPRINT);
            adapter.complete("receipt-key", 1, SNAPSHOT);
        });

        assertThatThrownBy(() -> transactions.executeWithoutResult(status ->
                adapter.complete("receipt-key", 1, "{\"id\":2}")))
                .isInstanceOf(IllegalStateException.class);
        assertThat(readReceipt("receipt-key")).contains(
                new CommandReceipt("receipt-key", 1, FINGERPRINT, 1, SNAPSHOT));
    }

    @Test
    void failureAfterCompletionRollsBackTheEntireReceiptAndAllowsFreshRetry() {
        var failure = new IllegalStateException("synthetic business failure");
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            adapter.lockKey("rollback-key");
            adapter.reserve("rollback-key", 1, FINGERPRINT);
            adapter.complete("rollback-key", 1, SNAPSHOT);
            throw failure;
        })).isSameAs(failure);
        assertThat(readReceipt("rollback-key")).isEmpty();

        transactions.executeWithoutResult(status -> {
            adapter.lockKey("rollback-key");
            adapter.reserve("rollback-key", 1, FINGERPRINT);
            adapter.complete("rollback-key", 1, SNAPSHOT);
        });
        assertThat(readReceipt("rollback-key")).isPresent();
    }

    @Test
    void duplicateReservationFailsWithoutReplacingTheCommittedReceipt() {
        transactions.executeWithoutResult(status -> {
            adapter.lockKey("receipt-key");
            adapter.reserve("receipt-key", 1, FINGERPRINT);
            adapter.complete("receipt-key", 1, SNAPSHOT);
        });

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            adapter.lockKey("receipt-key");
            adapter.reserve("receipt-key", 1, "b".repeat(64));
        })).isInstanceOf(DataIntegrityViolationException.class);
        assertThat(readReceipt("receipt-key")).contains(
                new CommandReceipt("receipt-key", 1, FINGERPRINT, 1, SNAPSHOT));
    }

    @Test
    void everyPortOperationRequiresAnExistingTransaction() {
        assertThatThrownBy(() -> adapter.lockKey("key")).isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> adapter.findByKey("key")).isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> adapter.reserve("key", 1, FINGERPRINT))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> adapter.complete("key", 1, SNAPSHOT))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM internal_audit_command_receipt", Integer.class)).isZero();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t\n"})
    void emptyReceiptKeysFailBeforeSql(String key) {
        assertInvalidKeyBeforeSql(key);
    }

    @Test
    void excessiveKeyLengthFailsBeforeSql() {
        assertInvalidKeyBeforeSql("x".repeat(256));
    }

    private void assertInvalidKeyBeforeSql(String key) {
        JdbcTemplate untouchedJdbc = mock(JdbcTemplate.class);
        var target = new CommandReceiptPersistenceAdapter(untouchedJdbc);
        assertThatThrownBy(() -> target.lockKey(key)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> target.findByKey(key)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> target.reserve(key, 1, FINGERPRINT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> target.complete(key, 1, SNAPSHOT)).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(untouchedJdbc);
    }

    @Test
    void maximumLengthKeyAndNegativeHashUseAnExistingLockBucket() {
        String key = "x".repeat(255);
        transactions.executeWithoutResult(status -> {
            adapter.lockKey(key);
            adapter.reserve(key, 1, FINGERPRINT);
            adapter.complete(key, 1, SNAPSHOT);
            // This Java string hashes to Integer.MIN_VALUE, exposing abs(hash) mistakes.
            assertThat("polygenelubricants".hashCode()).isEqualTo(Integer.MIN_VALUE);
            adapter.lockKey("polygenelubricants");
        });
        assertThat(readReceipt(key)).isPresent();
    }

    @Test
    void absentBucketFailsClosedBeforeReceiptReservation() {
        String key = "missing-bucket";
        jdbcTemplate.update("DELETE FROM internal_audit_key_lock WHERE bucket = ?", Math.floorMod(key.hashCode(), 256));

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            adapter.lockKey(key);
            adapter.reserve(key, 1, FINGERPRINT);
        })).isInstanceOf(EmptyResultDataAccessException.class);
        assertThat(readReceipt(key)).isEmpty();
    }

    @Test
    void invalidVersionsAndSnapshotAreRejectedBeforeSql() {
        JdbcTemplate untouchedJdbc = mock(JdbcTemplate.class);
        var target = new CommandReceiptPersistenceAdapter(untouchedJdbc);
        assertThatThrownBy(() -> target.reserve("key", 0, FINGERPRINT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> target.reserve("key", 1, "not-sha256")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> target.complete("key", 0, SNAPSHOT)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> target.complete("key", 1, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> target.complete("key", 1, " ")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(untouchedJdbc);
    }

    @Test
    void concurrentFirstUseWaitsUntilTheCommittedSnapshotIsVisible() throws Exception {
        CountDownLatch firstReserved = new CountDownLatch(1);
        CountDownLatch allowCommit = new CountDownLatch(1);
        CountDownLatch secondAttemptedLock = new CountDownLatch(1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = workers.submit(() -> transactions.executeWithoutResult(status -> {
                adapter.lockKey("concurrent-key");
                adapter.reserve("concurrent-key", 1, FINGERPRINT);
                firstReserved.countDown();
                await(allowCommit);
                adapter.complete("concurrent-key", 1, SNAPSHOT);
            }));
            assertThat(firstReserved.await(5, TimeUnit.SECONDS)).isTrue();
            Future<CommandReceipt> second = workers.submit(() -> transactions.execute(status -> {
                secondAttemptedLock.countDown();
                adapter.lockKey("concurrent-key");
                return adapter.findByKey("concurrent-key").orElseThrow();
            }));
            assertThat(secondAttemptedLock.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> second.get(150, TimeUnit.MILLISECONDS)).isInstanceOf(TimeoutException.class);

            allowCommit.countDown();
            first.get(5, TimeUnit.SECONDS);
            assertThat(second.get(5, TimeUnit.SECONDS)).isEqualTo(
                    new CommandReceipt("concurrent-key", 1, FINGERPRINT, 1, SNAPSHOT));
        } finally {
            allowCommit.countDown();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void hashCollisionSerializesWithoutMergingDistinctReceiptIdentities() {
        assertThat("Aa".hashCode()).isEqualTo("BB".hashCode());
        transactions.executeWithoutResult(status -> {
            adapter.lockKey("Aa");
            adapter.reserve("Aa", 1, FINGERPRINT);
            adapter.complete("Aa", 1, SNAPSHOT);
            adapter.lockKey("BB");
            adapter.reserve("BB", 1, "b".repeat(64));
            adapter.complete("BB", 1, "{\"id\":2}");
        });

        assertThat(readReceipt("Aa")).contains(
                new CommandReceipt("Aa", 1, FINGERPRINT, 1, SNAPSHOT));
        assertThat(readReceipt("BB")).contains(
                new CommandReceipt("BB", 1, "b".repeat(64), 1, "{\"id\":2}"));
    }

    private Optional<CommandReceipt> readReceipt(String key) {
        return transactions.execute(status -> adapter.findByKey(key));
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for synthetic transaction test");
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Synthetic transaction test interrupted", ex);
        }
    }
}
