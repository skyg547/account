package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.adapter.out.persistence.unsettled.UnsettledItemRepository;
import com.ho.account.journalledger.application.port.out.UnsettledItemPersistencePort;
import com.ho.account.journalledger.application.service.unsettled.UnsettledService;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.unsettled.UnsettledItem;
import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.JdbcTemplateAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real JPA repositories and independent transactions expose stale-state settlement races. */
@SpringBootTest(classes = UnsettledSettlementConcurrencyIntegrationTest.SettlementApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.name=unsettled-settlement-concurrency-test",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=false",
        "spring.sql.init.mode=never",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.vault.enabled=false",
        "eureka.client.enabled=false"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UnsettledSettlementConcurrencyIntegrationTest {
    private static final SettlementTestDatabase DATABASE = SettlementTestDatabase.create();
    private static final LocalDate DATE = LocalDate.of(2026, 9, 25);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::url);
        registry.add("spring.datasource.username", DATABASE::username);
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", DATABASE::driver);
        registry.add("spring.jpa.properties.hibernate.default_schema", DATABASE::schema);
        if (DATABASE.postgres()) {
            registry.add("spring.datasource.hikari.connection-init-sql", DATABASE::createSchemaSql);
            registry.add("spring.jpa.properties.hibernate.hbm2ddl.create_namespaces", () -> "true");
        }
    }

    @Autowired private UnsettledService service;
    @Autowired private UnsettledItemRepository repository;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private SettlementGate gate;

    private TransactionTemplate transactions;
    private Long itemId;

    @BeforeEach
    void seedOpenItem() {
        transactions = new TransactionTemplate(transactionManager);
        gate.reset();
        transactions.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM unsettled_item_settlement_references");
            jdbc.update("DELETE FROM unsettled_items");
            jdbc.update("DELETE FROM journal_details");
            jdbc.update("DELETE FROM journal_entries");

            JournalEntry entry = new JournalEntry();
            entry.setSlipNo("UNS770-FIXTURE");
            entry.setSlipDate(DATE);
            entry.setAccountingDate(DATE);
            entry.setCurrencyCode("KRW");
            entry.setCreatedBy("fixture");
            entry.initializeDraft();
            JournalDetail receivable = detail(JournalSide.DEBIT, "11000");
            entry.addDetail(receivable);
            entry.addDetail(detail(JournalSide.CREDIT, "41000"));
            entry.validateInvariants();
            entityManager.persist(entry);

            UnsettledItem item = new UnsettledItem();
            item.setManagementNo("UNS-770-" + UUID.randomUUID());
            item.setJournalDetail(receivable);
            item.setAccountCode("11000");
            item.setBusinessPartnerCode("BP-770");
            item.setOccurrenceDate(DATE);
            item.setOriginalAmount(new BigDecimal("100.00"));
            item.setSettledAmount(new BigDecimal("0.00"));
            item.setRemainingAmount(new BigDecimal("100.00"));
            item.setStatus("OPEN");
            itemId = repository.saveAndFlush(item).getId();
        });
    }

    @Test
    void distinctConcurrentReferencesPreserveBothAmountsAndReferences() throws Exception {
        ConcurrentResult result = runConcurrent(
                new SettlementRequest("40.00", "collector-40", "REF-40", false),
                new SettlementRequest("50.00", "collector-50", "REF-50", true));

        assertThat(result.leader().failure()).isNull();
        assertThat(result.contender().failure()).isNull();
        assertThat(result.leader().connectionId()).isNotEqualTo(result.contender().connectionId());
        assertThat(result.contenderWaited()).as("contender waits while the first transaction owns the claim").isTrue();
        assertThat(result.postgresLockObserved()).as("PostgreSQL reports a database lock wait").isTrue();
        SettlementState state = readState();
        assertThat(state.settled()).isEqualByComparingTo("90.00");
        assertThat(state.remaining()).isEqualByComparingTo("10.00");
        assertThat(state.status()).isEqualTo("PARTIAL");
        assertThat(state.resolved()).isFalse();
        assertThat(state.references()).containsExactlyInAnyOrder("REF-40", "REF-50");
        assertThat(state.lastReference()).isEqualTo("REF-50");
        assertThat(state.actor()).isEqualTo("collector-50");
        assertThat(state.settledAt()).isNotNull();
    }

    @Test
    void concurrentReplayOfSameReferenceAppliesExactlyOnce() throws Exception {
        ConcurrentResult result = runConcurrent(
                new SettlementRequest("40.00", "first-collector", "SAME-REF", false),
                new SettlementRequest("40.00", "replay-collector", "SAME-REF", true));

        assertThat(result.leader().failure()).isNull();
        assertThat(result.contender().failure()).isNull();
        SettlementState state = readState();
        assertThat(state.settled()).isEqualByComparingTo("40.00");
        assertThat(state.remaining()).isEqualByComparingTo("60.00");
        assertThat(state.references()).containsExactly("SAME-REF");
        assertThat(state.lastReference()).isEqualTo("SAME-REF");
        assertThat(state.actor()).isEqualTo("first-collector");
    }

    @Test
    void knownReferenceReplayAfterFullSettlementRemainsNoOp() {
        service.settleItem(itemId, new BigDecimal("100.00"), "clearing-collector", "CLEAR-REF");
        SettlementState cleared = readState();
        assertThat(cleared.status()).isEqualTo("CLEARED");
        assertThat(cleared.resolved()).isTrue();

        service.settleItem(itemId, null, "replay-collector", " CLEAR-REF ");

        assertThat(readState()).isEqualTo(cleared);
    }

    @Test
    void waitingOverSettlementReloadsRemainingBalanceAndRejectsOnlyContender() throws Exception {
        ConcurrentResult result = runConcurrent(
                new SettlementRequest("60.00", "winner", "REF-60", false),
                new SettlementRequest("50.00", "contender", "REF-50", true));

        assertThat(result.leader().failure()).isNull();
        assertThat(result.contender().failure()).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("40.00").hasMessageContaining("50.00");
        SettlementState state = readState();
        assertThat(state.settled()).isEqualByComparingTo("60.00");
        assertThat(state.remaining()).isEqualByComparingTo("40.00");
        assertThat(state.status()).isEqualTo("PARTIAL");
        assertThat(state.references()).containsExactly("REF-60");
        assertThat(state.lastReference()).isEqualTo("REF-60");
        assertThat(state.actor()).isEqualTo("winner");
    }

    @Test
    void failureAfterFlushRollsBackAggregateAndFreshTransactionCanRetry() {
        SettlementState before = readState();
        gate.failAfterFlush.set(true);

        assertThatThrownBy(() -> service.settleItem(
                itemId, new BigDecimal("40.00"), "failed-collector", "FAILED-REF"))
                .isInstanceOf(InjectedSettlementFailure.class);

        assertThat(gate.flushReached).isTrue();
        assertThat(readState()).isEqualTo(before);
        gate.failAfterFlush.set(false);

        service.settleItem(itemId, new BigDecimal("40.00"), "retry-collector", "RETRY-REF");

        SettlementState retried = readState();
        assertThat(retried.settled()).isEqualByComparingTo("40.00");
        assertThat(retried.remaining()).isEqualByComparingTo("60.00");
        assertThat(retried.status()).isEqualTo("PARTIAL");
        assertThat(retried.resolved()).isFalse();
        assertThat(retried.references()).containsExactly("RETRY-REF");
        assertThat(retried.lastReference()).isEqualTo("RETRY-REF");
        assertThat(retried.actor()).isEqualTo("retry-collector");
        assertThat(retried.settledAt()).isNotNull();
    }

    private ConcurrentResult runConcurrent(SettlementRequest leaderRequest, SettlementRequest contenderRequest)
            throws Exception {
        gate.startConcurrency();
        AtomicLong contenderConnection = new AtomicLong(-1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> leader = executor.submit(() -> attempt(true, leaderRequest, null));
            Future<Attempt> contender = executor.submit(() -> attempt(false, contenderRequest, contenderConnection));
            await(gate.leaderReady);
            await(gate.contenderAttempted);

            boolean postgresLockObserved = !DATABASE.postgres() || !gate.lockMethodUsed.get()
                    || awaitPostgresLock(contenderConnection.get(), contender);
            boolean contenderWaited = false;
            try {
                contender.get(300, TimeUnit.MILLISECONDS);
            } catch (TimeoutException expected) {
                contenderWaited = true;
            } finally {
                gate.releaseLeader.countDown();
            }
            ConcurrentResult result = new ConcurrentResult(leader.get(15, TimeUnit.SECONDS),
                    contender.get(15, TimeUnit.SECONDS), contenderWaited, postgresLockObserved);
            assertThat(result.leader().connectionId()).isNotEqualTo(result.contender().connectionId());
            assertThat(result.contenderWaited()).as("contender remains incomplete while the leader is held").isTrue();
            assertThat(result.postgresLockObserved()).as("PostgreSQL reports the contender's lock wait").isTrue();
            return result;
        } finally {
            gate.releaseLeader.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Attempt attempt(boolean leader, SettlementRequest request, AtomicLong connectionOut) {
        long[] connection = {-1};
        try {
            transactions.executeWithoutResult(status -> {
                connection[0] = connectionId();
                if (connectionOut != null) connectionOut.set(connection[0]);
                if (request.preload()) {
                    // The contender deliberately carries a stale managed aggregate into the lock lookup.
                    assertThat(repository.findById(itemId).orElseThrow().getRemainingAmount())
                            .isEqualByComparingTo("100.00");
                }
                gate.leaderThread.set(leader);
                try {
                    service.settleItem(itemId, new BigDecimal(request.amount()), request.actor(), request.reference());
                } finally {
                    gate.leaderThread.remove();
                }
            });
            return new Attempt(connection[0], null);
        } catch (RuntimeException failure) {
            return new Attempt(connection[0], failure);
        }
    }

    private boolean awaitPostgresLock(long connectionId, Future<Attempt> contender) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline && !contender.isDone()) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM pg_stat_activity"
                    + " WHERE pid = ? AND wait_event_type = 'Lock'", Integer.class, connectionId) == 1) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
    }

    private long connectionId() {
        return jdbc.queryForObject(DATABASE.postgres() ? "SELECT pg_backend_pid()" : "SELECT session_id()",
                Long.class);
    }

    private SettlementState readState() {
        return transactions.execute(status -> {
            entityManager.clear();
            UnsettledItem item = repository.findById(itemId).orElseThrow();
            return new SettlementState(item.getOriginalAmount(), item.getSettledAmount(), item.getRemainingAmount(),
                    item.getStatus(), item.isResolved(), new LinkedHashSet<>(item.getSettlementReferences()),
                    item.getLastSettlementReference(), item.getLastSettledBy(), item.getLastSettledAt());
        });
    }

    private static JournalDetail detail(JournalSide side, String accountCode) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal("100.00"));
        detail.setBaseAmount(new BigDecimal("100.00"));
        detail.setBusinessPartnerCode("BP-770");
        detail.setAuditUser("fixture");
        return detail;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for settlement test coordination");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("settlement test interrupted", interrupted);
        }
    }

    private record SettlementRequest(String amount, String actor, String reference, boolean preload) { }
    private record Attempt(long connectionId, RuntimeException failure) { }
    private record ConcurrentResult(Attempt leader, Attempt contender, boolean contenderWaited,
                                    boolean postgresLockObserved) { }
    private record SettlementState(BigDecimal original, BigDecimal settled, BigDecimal remaining, String status,
                                   boolean resolved, Set<String> references, String lastReference, String actor,
                                   LocalDateTime settledAt) { }

    private static final class InjectedSettlementFailure extends RuntimeException {
        private InjectedSettlementFailure() {
            super("injected failure after settlement flush");
        }
    }

    /**
     * The method-name routing keeps this source compilable against the audited pre-fix port. On that version both
     * ordinary reads complete before the leader commits; on the fixed port both calls meet before the lock lookup.
     */
    static final class SettlementGate {
        private final EntityManager entityManager;
        private final ThreadLocal<Boolean> leaderThread = new ThreadLocal<>();
        private final AtomicBoolean coordinate = new AtomicBoolean();
        private final AtomicBoolean lockMethodUsed = new AtomicBoolean();
        private final AtomicBoolean failAfterFlush = new AtomicBoolean();
        private final AtomicBoolean flushReached = new AtomicBoolean();
        private CountDownLatch bothAtBoundary;
        private CountDownLatch leaderReady;
        private CountDownLatch contenderAttempted;
        private CountDownLatch releaseLeader;
        private CountDownLatch leaderCompleted;

        private SettlementGate(EntityManager entityManager) {
            this.entityManager = entityManager;
            reset();
        }

        private void reset() {
            coordinate.set(false);
            lockMethodUsed.set(false);
            failAfterFlush.set(false);
            flushReached.set(false);
            bothAtBoundary = new CountDownLatch(2);
            leaderReady = new CountDownLatch(1);
            contenderAttempted = new CountDownLatch(1);
            releaseLeader = new CountDownLatch(1);
            leaderCompleted = new CountDownLatch(1);
        }

        private void startConcurrency() {
            reset();
            coordinate.set(true);
        }

        private UnsettledItemPersistencePort wrap(UnsettledItemPersistencePort delegate) {
            return (UnsettledItemPersistencePort) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{UnsettledItemPersistencePort.class}, (proxy, method, args) -> {
                        if (method.getName().equals("findByIdForSettlement") && coordinate.get()) {
                            lockMethodUsed.set(true);
                            bothAtBoundary.countDown();
                            await(bothAtBoundary);
                            if (Boolean.TRUE.equals(leaderThread.get())) {
                                Object result = invoke(delegate, method, args);
                                // Keep the returned entity and its row lock inside the first transaction.
                                leaderReady.countDown();
                                await(releaseLeader);
                                return result;
                            }
                            await(leaderReady);
                            contenderAttempted.countDown();
                            return invoke(delegate, method, args);
                        }
                        if (method.getName().equals("findById") && coordinate.get()) {
                            Object stale = invoke(delegate, method, args);
                            bothAtBoundary.countDown();
                            await(bothAtBoundary);
                            if (Boolean.TRUE.equals(leaderThread.get())) {
                                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                                    @Override
                                    public void afterCompletion(int status) {
                                        leaderCompleted.countDown();
                                    }
                                });
                                leaderReady.countDown();
                                await(releaseLeader);
                            } else {
                                contenderAttempted.countDown();
                                await(leaderCompleted);
                            }
                            return stale;
                        }
                        Object result = invoke(delegate, method, args);
                        if (method.getName().equals("save") && failAfterFlush.get()) {
                            // Flush proves the rollback covers SQL updates and the reference-row insert.
                            entityManager.flush();
                            flushReached.set(true);
                            throw new InjectedSettlementFailure();
                        }
                        return result;
                    });
        }

        private static Object invoke(Object target, java.lang.reflect.Method method, Object[] args) throws Throwable {
            try {
                return method.invoke(target, args);
            } catch (InvocationTargetException failure) {
                throw failure.getCause();
            }
        }
    }

    /** Only an explicitly supplied disposable PostgreSQL URL can replace the isolated in-memory H2 database. */
    private record SettlementTestDatabase(String url, String username, String driver, String schema,
                                          boolean postgres) {
        private static SettlementTestDatabase create() {
            String unique = "unsettled_" + UUID.randomUUID().toString().replace("-", "");
            String postgresUrl = System.getenv("JOURNAL_UNSETTLED_TEST_POSTGRES_URL");
            if (postgresUrl == null || postgresUrl.isBlank()) {
                return new SettlementTestDatabase("jdbc:h2:mem:" + unique
                        + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
                        "sa", "org.h2.Driver", "public", false);
            }
            return new SettlementTestDatabase(postgresUrl
                    + (postgresUrl.contains("?") ? "&" : "?") + "currentSchema=" + unique,
                    "postgres", "org.postgresql.Driver", unique, true);
        }

        private String createSchemaSql() {
            return "CREATE SCHEMA IF NOT EXISTS " + schema;
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ImportAutoConfiguration({DataSourceAutoConfiguration.class, JdbcTemplateAutoConfiguration.class,
            HibernateJpaAutoConfiguration.class, TransactionAutoConfiguration.class})
    @EntityScan(basePackages = "com.ho.account.journalledger.domain")
    @EnableJpaRepositories(basePackageClasses = UnsettledItemRepository.class)
    @Import({UnsettledItemPersistenceAdapter.class, UnsettledService.class})
    static class SettlementApplication {
        @Bean
        SettlementGate settlementGate(EntityManager entityManager) {
            return new SettlementGate(entityManager);
        }

        @Bean
        @Primary
        UnsettledItemPersistencePort gatedPersistence(UnsettledItemPersistenceAdapter delegate,
                                                      SettlementGate gate) {
            return gate.wrap(delegate);
        }
    }
}
