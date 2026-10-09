package com.ho.account.journalledger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalReversalPersistencePort;
import com.ho.account.journalledger.application.service.journal.JournalEntryService;
import com.ho.account.journalledger.application.service.journal.JournalRuleEngine;
import com.ho.account.journalledger.application.service.journal.validator.BalanceValidationFilter;
import com.ho.account.journalledger.application.service.journal.validator.ClosingLockValidationFilter;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalReversalOperation;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.infrastructure.persistence.repository.JournalEntryRepository;
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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest(classes = ReversalConcurrencyIntegrationTest.ReversalApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.name=reversal-concurrency-test",
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
class ReversalConcurrencyIntegrationTest {
    private static final PostingTestDatabase DATABASE = PostingTestDatabase.create();
    private static final LocalDate ORIGINAL_DATE = LocalDate.of(2026, 9, 25);
    private static final LocalDate REVERSAL_DATE = LocalDate.of(2026, 9, 30);

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
    @Autowired private JournalEntryRepository journals;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private ReversalWriteGate gate;
    private TransactionTemplate transactions;
    private Long originalId;

    @BeforeEach
    void seedPostedOriginal() {
        transactions = new TransactionTemplate(transactionManager);
        gate.reset();
        transactions.executeWithoutResult(status -> {
            jdbc.update("DELETE FROM journal_reversal_operations");
            jdbc.update("DELETE FROM journal_details");
            jdbc.update("DELETE FROM journal_entries");
            JournalEntry original = journal("GL759-ORIGINAL", ORIGINAL_DATE);
            original.requestApproval("maker");
            original.approve("checker");
            journals.saveAndFlush(original);
            original.post("poster");
            journals.saveAndFlush(original);
            originalId = original.getId();
        });
    }

    @Test
    void twoTransactionsSerializeAndReturnOnePersistedReversal() throws Exception {
        gate.holdFirstSave.set(true);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> first = executor.submit(() -> reverseInNewTransaction(
                    REVERSAL_DATE, "maker-one", "correction"));
            await(gate.beforeSave);
            Future<Attempt> second = executor.submit(() -> reverseInNewTransaction(
                    REVERSAL_DATE.plusDays(1), "maker-two", "retry payload"));

            assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            gate.release.countDown();

            Attempt firstAttempt = first.get(15, TimeUnit.SECONDS);
            Attempt secondAttempt = second.get(15, TimeUnit.SECONDS);
            assertThat(secondAttempt.connectionId()).isNotEqualTo(firstAttempt.connectionId());
            assertThat(secondAttempt.reversalId()).isEqualTo(firstAttempt.reversalId());
            Long firstId = firstAttempt.reversalId();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries"
                    + " WHERE entry_type = 'REVERSAL'", Integer.class)).isOne();
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_reversal_operations", Integer.class)).isOne();
            assertThat(jdbc.queryForObject("SELECT reversal_journal_entry_id FROM journal_reversal_operations"
                    + " WHERE original_journal_entry_id = ?", Long.class, originalId)).isEqualTo(firstId);
            assertThat(jdbc.queryForObject("SELECT status FROM journal_reversal_operations"
                    + " WHERE original_journal_entry_id = ?", String.class, originalId)).isEqualTo("PENDING");
            assertOriginalUnchanged();
        } finally {
            gate.release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void operationSaveFailureRollsBackReversalAndRetryCreatesOneEffectiveOperation() {
        gate.failNextSave.set(true);

        assertThatThrownBy(() -> service.reverseJournalEntry(
                originalId, REVERSAL_DATE, "maker", "first attempt"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("injected reversal operation save failure");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isOne();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_reversal_operations", Integer.class)).isZero();
        assertOriginalUnchanged();

        JournalEntry retried = service.reverseJournalEntry(
                originalId, REVERSAL_DATE, "maker", "retry after rollback");
        JournalEntry duplicate = service.reverseJournalEntry(
                originalId, REVERSAL_DATE.plusDays(1), "other", "duplicate");

        assertThat(duplicate.getId()).isEqualTo(retried.getId());
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_reversal_operations", Integer.class)).isOne();
        assertOriginalUnchanged();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("lifecycleRaces")
    void cancellationAndApprovalLifecycleSerializeWithoutSplitState(
            String scenario,
            LifecycleCommand lifecycle,
            boolean cancellationFirst) throws Exception {
        JournalEntry reversal = service.reverseJournalEntry(
                originalId, REVERSAL_DATE, "maker", "lifecycle race");
        Long reversalId = reversal.getId();
        if (lifecycle == LifecycleCommand.APPROVE) {
            service.requestJournalEntryApproval(reversalId, "maker");
        }
        CountDownLatch firstApplied = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> transactions.executeWithoutResult(status -> {
                if (cancellationFirst) {
                    service.cancelReversal(originalId, "canceller", "race cancellation");
                } else {
                    executeLifecycle(lifecycle, reversalId);
                }
                entityManager.flush();
                firstApplied.countDown();
                await(releaseFirst);
            }));
            await(firstApplied);
            Future<?> second = executor.submit(() -> {
                if (cancellationFirst) {
                    executeLifecycle(lifecycle, reversalId);
                } else {
                    service.cancelReversal(originalId, "canceller", "race cancellation");
                }
            });

            assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
                    .as(scenario + " second transaction must wait for the journal row")
                    .isInstanceOf(TimeoutException.class);
            releaseFirst.countDown();
            first.get(15, TimeUnit.SECONDS);
            if (cancellationFirst) {
                assertThatThrownBy(() -> second.get(15, TimeUnit.SECONDS))
                        .hasRootCauseInstanceOf(IllegalStateException.class);
            } else {
                second.get(15, TimeUnit.SECONDS);
            }

            String journalStatus = jdbc.queryForObject(
                    "SELECT status FROM journal_entries WHERE id = ?", String.class, reversalId);
            String operationStatus = jdbc.queryForObject(
                    "SELECT status FROM journal_reversal_operations WHERE original_journal_entry_id = ?",
                    String.class, originalId);
            assertThat(journalStatus).isEqualTo("REJECTED");
            assertThat(operationStatus).isEqualTo("CANCELLED");
            assertThat(operationStatus.equals("CANCELLED")
                    && (journalStatus.equals("REQUESTED") || journalStatus.equals("APPROVED"))).isFalse();
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    private static Stream<Arguments> lifecycleRaces() {
        return Stream.of(
                Arguments.of("cancel wins request race", LifecycleCommand.REQUEST, true),
                Arguments.of("request commits before cancel", LifecycleCommand.REQUEST, false),
                Arguments.of("cancel wins approve race", LifecycleCommand.APPROVE, true),
                Arguments.of("approve commits before cancel", LifecycleCommand.APPROVE, false));
    }

    private void executeLifecycle(LifecycleCommand command, Long reversalId) {
        if (command == LifecycleCommand.REQUEST) {
            service.requestJournalEntryApproval(reversalId, "maker");
        } else {
            service.approveJournalEntry(reversalId, "checker");
        }
    }

    private void assertOriginalUnchanged() {
        assertThat(jdbc.queryForObject("SELECT status FROM journal_entries WHERE id = ?", String.class, originalId))
                .isEqualTo("POSTED");
        assertThat(jdbc.queryForObject("SELECT SUM(amount) FROM journal_details"
                + " WHERE journal_entry_id = ?", BigDecimal.class, originalId)).isEqualByComparingTo("200.00");
        assertThat(jdbc.queryForObject("SELECT SUM(base_amount) FROM journal_details"
                + " WHERE journal_entry_id = ?", BigDecimal.class, originalId)).isEqualByComparingTo("200.00");
    }

    private Attempt reverseInNewTransaction(LocalDate date, String creator, String reason) {
        return transactions.execute(status -> {
            long connectionId = jdbc.queryForObject(DATABASE.driver().equals("org.postgresql.Driver")
                    ? "SELECT pg_backend_pid()" : "SELECT session_id()", Long.class);
            Long reversalId = service.reverseJournalEntry(originalId, date, creator, reason).getId();
            return new Attempt(connectionId, reversalId);
        });
    }

    private static JournalEntry journal(String slipNo, LocalDate date) {
        JournalEntry journal = new JournalEntry();
        journal.setSlipNo(slipNo);
        journal.setSlipDate(date);
        journal.setAccountingDate(date);
        journal.setDescription("posted source");
        journal.setCurrencyCode("KRW");
        journal.setCreatedBy("maker");
        journal.addDetail(detail(JournalSide.DEBIT, "10100", "D-1", "BP-1"));
        journal.addDetail(detail(JournalSide.CREDIT, "40100", "D-2", "BP-2"));
        journal.initializeDraft();
        return journal;
    }

    private static JournalDetail detail(JournalSide side, String account, String department, String partner) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(account);
        detail.setAmount(new BigDecimal("100.00"));
        detail.setBaseAmount(new BigDecimal("100.00"));
        detail.setDepartmentCode(department);
        detail.setBusinessPartnerCode(partner);
        return detail;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("reversal concurrency coordination timed out");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("reversal concurrency test interrupted", interrupted);
        }
    }

    private record Attempt(long connectionId, Long reversalId) { }

    private enum LifecycleCommand { REQUEST, APPROVE }

    static class ReversalWriteGate {
        final AtomicBoolean holdFirstSave = new AtomicBoolean();
        final AtomicBoolean failNextSave = new AtomicBoolean();
        CountDownLatch beforeSave;
        CountDownLatch release;
        private final JournalReversalPersistencePort delegate;
        private final EntityManager entityManager;

        ReversalWriteGate(JournalReversalPersistencePort delegate, EntityManager entityManager) {
            this.delegate = delegate;
            this.entityManager = entityManager;
            reset();
        }

        void reset() {
            holdFirstSave.set(false);
            failNextSave.set(false);
            beforeSave = new CountDownLatch(1);
            release = new CountDownLatch(1);
        }

        JournalReversalPersistencePort gatedPort() {
            return new JournalReversalPersistencePort() {
                @Override
                public JournalReversalOperation save(JournalReversalOperation operation) {
                    if (holdFirstSave.compareAndSet(true, false)) {
                        beforeSave.countDown();
                        await(release);
                    }
                    if (failNextSave.compareAndSet(true, false)) {
                        entityManager.flush();
                        throw new IllegalStateException("injected reversal operation save failure");
                    }
                    return delegate.save(operation);
                }

                @Override
                public Optional<JournalReversalOperation> findByOriginalJournalEntryId(Long originalId) {
                    return delegate.findByOriginalJournalEntryId(originalId);
                }

                @Override
                public Optional<JournalReversalOperation> findByReversalJournalEntryId(Long reversalId) {
                    return delegate.findByReversalJournalEntryId(reversalId);
                }
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.ho.account.journalledger.infrastructure.persistence")
    @EnableJpaRepositories("com.ho.account.journalledger.infrastructure.persistence.repository")
    @Import({JournalPersistenceAdapter.class, JournalReversalPersistenceAdapter.class})
    static class ReversalApplication {
        @Bean ReversalWriteGate reversalWriteGate(
                JournalReversalPersistenceAdapter delegate, EntityManager entityManager) {
            return new ReversalWriteGate(delegate, entityManager);
        }

        @Bean
        @Primary
        JournalReversalPersistencePort gatedReversalPort(ReversalWriteGate gate) {
            return gate.gatedPort();
        }

        @Bean
        JournalEntryService journalEntryService(
                JournalPersistencePort journals,
                @Qualifier("gatedReversalPort") JournalReversalPersistencePort reversals) {
            JournalValidationEngine validation = new JournalValidationEngine(List.of(
                    new BalanceValidationFilter(), new ClosingLockValidationFilter(date -> false)));
            return new JournalEntryService(journals, reversals,
                    org.mockito.Mockito.mock(JournalRuleEngine.class),
                    org.mockito.Mockito.mock(PostingService.class), validation);
        }
    }
}
