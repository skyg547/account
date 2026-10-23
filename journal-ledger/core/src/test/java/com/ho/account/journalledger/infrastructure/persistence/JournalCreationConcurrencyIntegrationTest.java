package com.ho.account.journalledger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.journalledger.application.port.in.JournalCreationIdentity;
import com.ho.account.journalledger.application.port.out.JournalCreationOperationPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.service.journal.JournalCreationFingerprint;
import com.ho.account.journalledger.application.service.journal.JournalEntryService;
import com.ho.account.journalledger.application.service.journal.JournalRuleEngine;
import com.ho.account.journalledger.application.service.journal.validator.BalanceValidationFilter;
import com.ho.account.journalledger.application.service.journal.validator.ClosingLockValidationFilter;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalCreationOperation;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = JournalCreationConcurrencyIntegrationTest.TestApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.name=journal-creation-concurrency-test",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/journal-migration",
        "spring.sql.init.mode=never",
        "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false",
        "spring.cloud.vault.enabled=false",
        "eureka.client.enabled=false"
})
class JournalCreationConcurrencyIntegrationTest {

    private static final PostingTestDatabase DATABASE = PostingTestDatabase.create();
    private static final LocalDate JOURNAL_DATE = LocalDate.of(2026, 9, 25);

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::url);
        registry.add("spring.datasource.username", DATABASE::username);
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", DATABASE::driver);
        registry.add("spring.flyway.schemas", DATABASE::schema);
        registry.add("spring.jpa.properties.hibernate.default_schema", DATABASE::schema);
    }

    @Autowired private JournalEntryService service;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private OperationWriteGate gate;
    @Autowired private JournalCreationOperationPersistenceAdapter durableOperations;
    private TransactionTemplate transactions;

    @BeforeEach
    void clearCreationState() {
        transactions = new TransactionTemplate(transactionManager);
        gate.reset();
        jdbc.update("DELETE FROM journal_creation_operations");
        jdbc.update("DELETE FROM journal_details");
        jdbc.update("DELETE FROM journal_entries");
    }

    @Test
    void equivalentConcurrentRetriesWaitAndConvergeToOneJournalAndFingerprint() throws Exception {
        JournalCreationIdentity identity = JournalCreationIdentity.primary("ISSUE_765", "CONCURRENT-1");
        JournalEntry firstRequest = journal(identity, false, "100.00");
        JournalEntry equivalentRetry = journal(identity, true, "100.0");
        String expectedFingerprint = JournalCreationFingerprint.calculate(firstRequest);
        gate.holdFirstOperationSave.set(true);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> first = executor.submit(() -> createInNewTransaction(identity, firstRequest));
            await(gate.beforeOperationSave);
            Future<Attempt> second = executor.submit(() -> createInNewTransaction(identity, equivalentRetry));

            assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
                    .as("the loser must wait on the V18 stripe until the winner transaction commits")
                    .isInstanceOf(TimeoutException.class);
            gate.release.countDown();

            Attempt winner = first.get(15, TimeUnit.SECONDS);
            Attempt retry = second.get(15, TimeUnit.SECONDS);
            assertThat(retry.connectionId()).isNotEqualTo(winner.connectionId());
            assertThat(retry.journalId()).isEqualTo(winner.journalId());
            // Both TransactionTemplates have committed; the retry result must still carry initialized lines
            // so HTTP/contract response mapping cannot fail on a detached lazy collection.
            assertThat(retry.entry().getDetails()).hasSize(2);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isOne();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_details", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isOne();
            assertThat(jdbc.queryForObject("SELECT fingerprint_version FROM journal_creation_operations",
                    Integer.class)).isEqualTo(JournalCreationFingerprint.CURRENT_VERSION);
            assertThat(jdbc.queryForObject("SELECT request_fingerprint FROM journal_creation_operations",
                    String.class)).isEqualTo(expectedFingerprint);
            assertThat(jdbc.queryForObject("SELECT journal_entry_id FROM journal_creation_operations",
                    Long.class)).isEqualTo(winner.journalId());
        } finally {
            gate.release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void sameLineageWithDistinctOperationKeysCreatesDistinctJournals() {
        JournalCreationIdentity primary = new JournalCreationIdentity("ISSUE_765", "MULTI-1", "PRIMARY");
        JournalCreationIdentity tax = new JournalCreationIdentity("ISSUE_765", "MULTI-1", "TAX");

        JournalEntry first = service.createJournalEntry(primary, journal(primary, false, "100.00"));
        JournalEntry second = service.createJournalEntry(tax, journal(tax, false, "100.00"));

        assertThat(second.getId()).isNotEqualTo(first.getId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT operation_key FROM journal_creation_operations"
                + " ORDER BY operation_key", String.class)).containsExactly("PRIMARY", "TAX");
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT request_fingerprint)"
                + " FROM journal_creation_operations", Integer.class)).isOne();
    }

    @Test
    void operationFailureRollsBackJournalAndFreshRetryCreatesOneAtomicResult() {
        JournalCreationIdentity identity = JournalCreationIdentity.primary("ISSUE_765", "ROLLBACK-1");
        gate.failNextOperationSave.set(true);

        assertThatThrownBy(() -> service.createJournalEntry(identity, journal(identity, false, "100.00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("injected creation operation save failure");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_details", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isZero();

        JournalEntry retried = service.createJournalEntry(identity, journal(identity, true, "100.0"));
        assertThat(retried.getId()).isPositive();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_details", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isOne();
        assertThat(jdbc.queryForObject("SELECT journal_entry_id FROM journal_creation_operations", Long.class))
                .isEqualTo(retried.getId());
    }

    @Test
    void durableOperationLockRejectsMissingReadOnlyAndSnapshotTransactions() {
        JournalCreationIdentity identity = JournalCreationIdentity.primary("ISSUE_765", "TX-GUARD");

        assertThatThrownBy(() -> durableOperations.acquireOperationLock(identity))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Journal creation operations require an active write transaction");

        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);
        assertThatThrownBy(() -> readOnly.executeWithoutResult(
                        status -> durableOperations.acquireOperationLock(identity)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Journal creation operations require an active write transaction");

        TransactionTemplate repeatableRead = new TransactionTemplate(transactionManager);
        repeatableRead.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThatThrownBy(() -> repeatableRead.executeWithoutResult(
                        status -> durableOperations.acquireOperationLock(identity)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Journal creation operations require READ_COMMITTED transaction isolation");
    }

    private Attempt createInNewTransaction(JournalCreationIdentity identity, JournalEntry request) {
        return transactions.execute(status -> {
            long connectionId = jdbc.queryForObject(DATABASE.driver().equals("org.postgresql.Driver")
                    ? "SELECT pg_backend_pid()" : "SELECT session_id()", Long.class);
            JournalEntry entry = service.createJournalEntry(identity, request);
            return new Attempt(connectionId, entry);
        });
    }

    private static JournalEntry journal(
            JournalCreationIdentity identity,
            boolean reverseLines,
            String amount) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(JOURNAL_DATE);
        entry.setAccountingDate(JOURNAL_DATE);
        entry.setDescription("durable retry");
        entry.setEntryType("NORMAL");
        entry.setCurrencyCode("KRW");
        entry.setExchangeRate(new BigDecimal("1.00000000"));
        entry.setCreatedBy("service:test-maker");
        entry.setAuditUser("service:test-maker");
        entry.setLineageSourceType(identity.sourceType());
        entry.setLineageSourceId(identity.sourceId());
        JournalDetail debit = detail(JournalSide.DEBIT, "10100", amount, "cash");
        JournalDetail credit = detail(JournalSide.CREDIT, "40100", amount, "revenue");
        (reverseLines ? List.of(credit, debit) : List.of(debit, credit)).forEach(entry::addDetail);
        return entry;
    }

    private static JournalDetail detail(
            JournalSide side, String accountCode, String amount, String description) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal(amount));
        detail.setBaseAmount(new BigDecimal(amount));
        detail.setDepartmentCode("D-765");
        detail.setBusinessPartnerCode("BP-765");
        detail.setDetailDescription(description);
        detail.setAuditUser("service:test-maker");
        return detail;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("journal creation concurrency coordination timed out");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("journal creation concurrency test interrupted", interrupted);
        }
    }

    private record Attempt(long connectionId, JournalEntry entry) {
        Long journalId() {
            return entry.getId();
        }
    }

    static class OperationWriteGate {
        final AtomicBoolean holdFirstOperationSave = new AtomicBoolean();
        final AtomicBoolean failNextOperationSave = new AtomicBoolean();
        CountDownLatch beforeOperationSave;
        CountDownLatch release;
        private final JournalCreationOperationPersistencePort delegate;
        private final EntityManager entityManager;

        OperationWriteGate(
                JournalCreationOperationPersistencePort delegate,
                EntityManager entityManager) {
            this.delegate = delegate;
            this.entityManager = entityManager;
            reset();
        }

        void reset() {
            holdFirstOperationSave.set(false);
            failNextOperationSave.set(false);
            beforeOperationSave = new CountDownLatch(1);
            release = new CountDownLatch(1);
        }

        JournalCreationOperationPersistencePort gatedPort() {
            return new JournalCreationOperationPersistencePort() {
                @Override
                public void acquireOperationLock(JournalCreationIdentity identity) {
                    delegate.acquireOperationLock(identity);
                }

                @Override
                public Optional<JournalCreationOperation> findByIdentity(JournalCreationIdentity identity) {
                    return delegate.findByIdentity(identity);
                }

                @Override
                public JournalCreationOperation save(JournalCreationOperation operation) {
                    if (holdFirstOperationSave.compareAndSet(true, false)) {
                        beforeOperationSave.countDown();
                        await(release);
                    }
                    if (failNextOperationSave.compareAndSet(true, false)) {
                        entityManager.flush();
                        throw new IllegalStateException("injected creation operation save failure");
                    }
                    return delegate.save(operation);
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.ho.account.journalledger.domain")
    @EnableJpaRepositories("com.ho.account.journalledger.domain")
    @Import({JournalPersistenceAdapter.class, JournalCreationOperationPersistenceAdapter.class})
    static class TestApplication {
        @Bean
        OperationWriteGate operationWriteGate(
                JournalCreationOperationPersistenceAdapter delegate,
                EntityManager entityManager) {
            return new OperationWriteGate(delegate, entityManager);
        }

        @Bean
        @Primary
        JournalCreationOperationPersistencePort gatedCreationOperationPort(OperationWriteGate gate) {
            return gate.gatedPort();
        }

        @Bean
        JournalEntryService journalEntryService(
                JournalPersistencePort journals,
                @Qualifier("gatedCreationOperationPort") JournalCreationOperationPersistencePort operations) {
            JournalValidationEngine validation = new JournalValidationEngine(List.of(
                    new BalanceValidationFilter(), new ClosingLockValidationFilter(date -> false)));
            return new JournalEntryService(journals, operations,
                    org.mockito.Mockito.mock(JournalRuleEngine.class),
                    org.mockito.Mockito.mock(PostingService.class), validation);
        }
    }
}
