package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalReversalPersistencePort;
import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;
import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.application.service.journal.JournalEntryService;
import com.ho.account.journalledger.application.service.journal.JournalRuleEngine;
import com.ho.account.journalledger.application.service.journal.validator.ClosingLockValidationFilter;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.application.service.ledger.BalanceReaggregationService;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.infrastructure.persistence.repository.JournalDetailRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.domain.GeneralLedger;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import com.ho.account.journalledger.infrastructure.persistence.repository.GlBalanceRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.GlEntryRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.SlBalanceRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.SlEntryRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = PostingConcurrencyIntegrationTest.PostingApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.name=posting-concurrency-test",
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
class PostingConcurrencyIntegrationTest {
    private static final PostingTestDatabase DATABASE = PostingTestDatabase.create();
    private static final LocalDate DATE = LocalDate.of(2026, 9, 24);
    private static final String POSTING_CONFLICT = "승인된 전표만 원장으로 전기할 수 있습니다.";

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::url);
        registry.add("spring.datasource.username", DATABASE::username);
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", DATABASE::driver);
        registry.add("spring.flyway.schemas", DATABASE::schema);
        registry.add("spring.jpa.properties.hibernate.default_schema", DATABASE::schema);
    }

    enum Mode { JPA, JDBC }

    @Autowired @Qualifier("jpaPosting") private PostingService jpaPosting;
    @Autowired @Qualifier("jdbcPosting") private PostingService jdbcPosting;
    @Autowired @Qualifier("jpaEntries") private LedgerEntryPersistencePort jpaEntries;
    @Autowired @Qualifier("jdbcEntries") private LedgerEntryPersistencePort jdbcEntries;
    @Autowired private JournalEntryRepository journals;
    @Autowired private JournalPersistencePort journalPersistence;
    @Autowired private GlBalanceRepository glBalances;
    @Autowired private SlBalanceRepository slBalances;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntryGate gate;
    @Autowired @Qualifier("jpaBalances") private LedgerBalancePersistencePort jpaBalances;
    @Autowired private BalanceReaggregationService reaggregationService;
    @Autowired private JournalEntryService reversalCommands;
    private TransactionTemplate transactions;
    private Long journalId;

    @BeforeEach
    void seedApprovedJournalAndExistingBalances() {
        transactions = new TransactionTemplate(transactionManager);
        gate.reset();
        transactions.executeWithoutResult(status -> {
            jdbc.update("UPDATE ledger_reaggregation_control SET status = 'OPEN',"
                    + " owner_job_instance_id = NULL, range_start = NULL, range_end = NULL, epoch = epoch + 1"
                    + " WHERE control_id = 1");
            for (String table : List.of("gl_entries", "sl_entries", "gl_balances", "sl_balances",
                    "journal_reversal_operations", "journal_details", "journal_entries")) {
                jdbc.update("DELETE FROM " + table);
            }
            JournalEntry journal = journal();
            journal.requestApproval("maker");
            journal.approve("approver");
            journalId = journals.saveAndFlush(journal).getId();
            for (JournalDetail detail : journal.getDetails()) {
                GlBalance gl = new GlBalance();
                gl.setAccountCode(detail.getAccountCode());
                gl.setCurrencyCode("KRW");
                gl.setBalanceDate(DATE);
                gl.setPeriod(YearMonth.from(DATE));
                glBalances.save(gl);
                SlBalance sl = new SlBalance();
                sl.setAccountCode(detail.getAccountCode());
                sl.setCurrencyCode("KRW");
                sl.setBusinessPartnerCode("BP-760");
                sl.setDepartmentCode("D-760");
                sl.setBalanceDate(DATE);
                sl.setPeriod(YearMonth.from(DATE));
                slBalances.save(sl);
            }
        });
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void overlappingRequestsWithPreviouslyLoadedApprovedStatePostExactlyOnce(Mode mode) throws Exception {
        CountDownLatch bothLoaded = new CountDownLatch(2);
        CountDownLatch secondAttempted = new CountDownLatch(1);
        AtomicLong secondConnection = new AtomicLong();
        gate.holdFirstWriter = true;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> first = executor.submit(() -> attempt(mode, "winner", bothLoaded, null, null));
            Future<Attempt> second = executor.submit(() -> attempt(mode, "loser", bothLoaded, secondAttempted, secondConnection));
            await(secondAttempted);
            boolean databaseLockObserved = !DATABASE.driver().equals("org.postgresql.Driver")
                    || awaitPostgresLock(secondConnection.get(), second);
            boolean secondWaited = false;
            try {
                second.get(300, TimeUnit.MILLISECONDS);
            } catch (TimeoutException expected) {
                secondWaited = true;
            } finally {
                gate.release.countDown();
            }
            Attempt winner = first.get(15, TimeUnit.SECONDS);
            Attempt loser = second.get(15, TimeUnit.SECONDS);
            assertThat(winner.connectionId()).isNotEqualTo(loser.connectionId());
            assertThat(winner.failure()).isNull();
            assertEntryCounts(2);
            assertThat(loser.failure()).isInstanceOf(IllegalStateException.class).hasMessage(POSTING_CONFLICT);
            assertThat(databaseLockObserved).as("PostgreSQL reports the second connection waiting on a lock").isTrue();
            assertThat(secondWaited).as("second transaction waited for the journal claim").isTrue();
            assertThat(gate.writes.get()).isEqualTo(1);
            assertPostedExactlyOnce("winner");

            assertThatThrownBy(() -> posting(mode).postJournalEntry(journalId, "retry"))
                    .isInstanceOf(IllegalStateException.class).hasMessage(POSTING_CONFLICT);
            assertPostedExactlyOnce("winner");
        } finally {
            gate.release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Attempt attempt(Mode mode, String actor, CountDownLatch bothLoaded, CountDownLatch secondAttempted,
                            AtomicLong secondConnection) {
        long[] connectionId = {-1};
        try {
            transactions.executeWithoutResult(status -> {
                connectionId[0] = jdbc.queryForObject(DATABASE.driver().equals("org.postgresql.Driver")
                        ? "SELECT pg_backend_pid()" : "SELECT session_id()", Long.class);
                // Both persistence contexts already contain APPROVED before either claims the journal.
                // A lock query without a refresh incorrectly reuses this stale managed object.
                assertThat(journals.findByIdWithDetails(journalId).orElseThrow().getStatus())
                        .isEqualTo(JournalEntryStatus.APPROVED);
                bothLoaded.countDown();
                await(bothLoaded);
                if (secondAttempted != null) {
                    await(gate.beforeEntries);
                    secondConnection.set(connectionId[0]);
                    secondAttempted.countDown();
                }
                posting(mode).postJournalEntry(journalId, actor);
            });
            return new Attempt(connectionId[0], null);
        } catch (RuntimeException failure) {
            return new Attempt(connectionId[0], failure);
        }
    }

    private boolean awaitPostgresLock(long connectionId, Future<Attempt> request) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline && !request.isDone()) {
            if (jdbc.queryForObject("SELECT COUNT(*) FROM pg_stat_activity WHERE pid = ? AND wait_event_type = 'Lock'",
                    Integer.class, connectionId) == 1) {
                return true;
            }
            Thread.sleep(10);
        }
        return false;
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void failureAfterClaimAndStatusFlushBeforeEntryInsertRollsBackAndCanRetry(Mode mode) {
        gate.failBeforeInsert = true;
        assertThatThrownBy(() -> posting(mode).postJournalEntry(journalId, "failed-poster"))
                .isInstanceOf(IllegalStateException.class).hasMessage("injected failure before entries");
        assertThat(jdbc.queryForObject("SELECT status FROM journal_entries WHERE id = ?", String.class, journalId))
                .isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("SELECT audit_user FROM journal_entries WHERE id = ?", String.class, journalId))
                .isEqualTo("approver");
        assertEntryCounts(0);
        assertBalances("0.00");
        gate.failBeforeInsert = false;
        posting(mode).postJournalEntry(journalId, "retry-poster");
        assertPostedExactlyOnce("retry-poster");
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void outerFailureAfterReversalOperationIsMarkedPostedRollsBackAndWholePostingCanRetry(Mode mode) {
        long originalId = journalId + 100_000L;
        jdbc.update("INSERT INTO journal_entries"
                + " (id, slip_no, slip_date, accounting_date, status, entry_type, currency_code, created_by, audit_user)"
                + " VALUES (?, ?, ?, ?, 'POSTED', 'NORMAL', 'KRW', 'maker', 'poster')",
                originalId, "GL759-RB-" + mode, DATE, DATE);
        jdbc.update("UPDATE journal_entries SET entry_type = 'REVERSAL',"
                + " lineage_source_type = 'JOURNAL_ENTRY', lineage_source_id = ? WHERE id = ?",
                Long.toString(originalId), journalId);
        jdbc.update("INSERT INTO journal_reversal_operations"
                + " (original_journal_entry_id, reversal_journal_entry_id, status, created_at, updated_at)"
                + " VALUES (?, ?, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)", originalId, journalId);

        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            posting(mode).postJournalEntry(journalId, "failed-reversal-poster");
            entityManager.flush();
            assertThat(jdbc.queryForObject("SELECT status FROM journal_reversal_operations"
                    + " WHERE original_journal_entry_id = ?", String.class, originalId)).isEqualTo("POSTED");
            throw new IllegalStateException("injected failure after reversal operation mark");
        })).isInstanceOf(IllegalStateException.class)
                .hasMessage("injected failure after reversal operation mark");

        assertThat(jdbc.queryForObject("SELECT status FROM journal_entries WHERE id = ?", String.class, journalId))
                .isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("SELECT status FROM journal_reversal_operations"
                + " WHERE original_journal_entry_id = ?", String.class, originalId)).isEqualTo("PENDING");
        assertEntryCounts(0);
        assertBalances("0.00");

        posting(mode).postJournalEntry(journalId, "retry-reversal-poster");

        assertThat(jdbc.queryForObject("SELECT status FROM journal_reversal_operations"
                + " WHERE original_journal_entry_id = ?", String.class, originalId)).isEqualTo("POSTED");
        assertPostedExactlyOnce("retry-reversal-poster");
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void postingWinnerMakesConcurrentCancellationFailWithOnePostedEconomicEffect(Mode mode) throws Exception {
        long originalId = seedPendingReversalOperation("PW-" + mode);
        gate.holdFirstWriter = true;
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> posting = executor.submit(() -> posting(mode).postJournalEntry(journalId, "race-poster"));
            await(gate.beforeEntries);
            Future<?> cancellation = executor.submit(() ->
                    reversalCommands.cancelReversal(originalId, "race-canceller", "too late"));

            assertThatThrownBy(() -> cancellation.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            gate.release.countDown();
            posting.get(15, TimeUnit.SECONDS);
            assertThatThrownBy(() -> cancellation.get(15, TimeUnit.SECONDS))
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .rootCause().hasMessageContaining("POSTED");

            assertThat(jdbc.queryForObject("SELECT status FROM journal_entries WHERE id = ?", String.class, journalId))
                    .isEqualTo("POSTED");
            assertThat(jdbc.queryForObject("SELECT status FROM journal_reversal_operations"
                    + " WHERE original_journal_entry_id = ?", String.class, originalId)).isEqualTo("POSTED");
            assertPostedExactlyOnce("race-poster");
        } finally {
            gate.release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void cancellationWinnerMakesConcurrentPostingFailWithoutEconomicEffect(Mode mode) throws Exception {
        long originalId = seedPendingReversalOperation("CW-" + mode);
        CountDownLatch cancellationApplied = new CountDownLatch(1);
        CountDownLatch releaseCancellation = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> cancellation = executor.submit(() -> transactions.executeWithoutResult(status -> {
                reversalCommands.cancelReversal(originalId, "race-canceller", "abandoned");
                entityManager.flush();
                cancellationApplied.countDown();
                await(releaseCancellation);
            }));
            await(cancellationApplied);
            Future<?> posting = executor.submit(() -> posting(mode).postJournalEntry(journalId, "late-poster"));

            assertThatThrownBy(() -> posting.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            releaseCancellation.countDown();
            cancellation.get(15, TimeUnit.SECONDS);
            assertThatThrownBy(() -> posting.get(15, TimeUnit.SECONDS))
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .rootCause().hasMessageContaining("승인된 전표만");

            assertThat(jdbc.queryForObject("SELECT status FROM journal_entries WHERE id = ?", String.class, journalId))
                    .isEqualTo("REJECTED");
            assertThat(jdbc.queryForObject("SELECT status FROM journal_reversal_operations"
                    + " WHERE original_journal_entry_id = ?", String.class, originalId)).isEqualTo("CANCELLED");
            assertEntryCounts(0);
            assertBalances("0.00");
        } finally {
            releaseCancellation.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void postingBlockedByReaggregationRollsBackThenRetriesExactlyOnceAfterRelease(Mode mode) {
        jdbc.update("UPDATE ledger_reaggregation_control SET status = 'REBUILDING',"
                + " owner_job_instance_id = 767, range_start = ?, range_end = ?, epoch = epoch + 1"
                + " WHERE control_id = 1", DATE, DATE);

        assertThatThrownBy(() -> posting(mode).postJournalEntry(journalId, "blocked-poster"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("reaggregation");
        assertThat(jdbc.queryForObject("SELECT status FROM journal_entries WHERE id = ?", String.class, journalId))
                .isEqualTo("APPROVED");
        assertEntryCounts(0);
        assertBalances("0.00");

        jdbc.update("UPDATE ledger_reaggregation_control SET status = 'OPEN',"
                + " owner_job_instance_id = NULL, range_start = NULL, range_end = NULL, epoch = epoch + 1"
                + " WHERE control_id = 1");
        posting(mode).postJournalEntry(journalId, "retry-after-release");
        assertPostedExactlyOnce("retry-after-release");
    }

    @Test
    void postingThatWinsAnAffectedStripeCommitsBeforeBarrierAndIsInStablePostedSourceOnce() throws Exception {
        CountDownLatch stripeHeld = new CountDownLatch(1);
        CountDownLatch allowPosting = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> posting = executor.submit(() -> transactions.executeWithoutResult(status -> {
                jpaBalances.lockBalanceAccounts(List.of(
                        new LedgerBalancePersistencePort.BalanceAccount("10100", "KRW"),
                        new LedgerBalancePersistencePort.BalanceAccount("40100", "KRW")));
                stripeHeld.countDown();
                await(allowPosting);
                jpaPosting.postJournalEntry(journalId, "pre-barrier-poster");
            }));
            await(stripeHeld);
            Future<?> barrier = executor.submit(() -> reaggregationService.start(767, DATE, DATE));

            assertThatThrownBy(() -> barrier.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(TimeoutException.class);
            allowPosting.countDown();
            posting.get(15, TimeUnit.SECONDS);
            barrier.get(15, TimeUnit.SECONDS);

            assertPostedExactlyOnce("pre-barrier-poster");
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries"
                    + " WHERE status = 'POSTED' AND accounting_date = ?", Integer.class, DATE)).isOne();
            assertThat(jdbc.queryForObject("SELECT owner_job_instance_id FROM ledger_reaggregation_control"
                    + " WHERE control_id = 1", Long.class)).isEqualTo(767L);
        } finally {
            allowPosting.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void readOnlyDetailLookupReturnsDetailsWithoutRequiringAWriteLock() {
        TransactionTemplate readOnly = new TransactionTemplate(transactionManager);
        readOnly.setReadOnly(true);
        JournalEntry result = readOnly.execute(status ->
                journalPersistence.findByIdWithDetails(journalId).orElseThrow());
        assertThat(result.getDetails()).hasSize(2);
        assertThat(result.getStatus()).isEqualTo(JournalEntryStatus.APPROVED);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void approvalAndPostingInOneTransactionRetainsPendingApproval(Mode mode) {
        transactions.executeWithoutResult(status -> {
            JournalEntry draft = journal();
            draft.setSlipNo("GL760-SAME-TX");
            journals.saveAndFlush(draft);
            draft.requestApproval("maker");
            draft.approve("same-tx-approver");
            posting(mode).postJournalEntry(draft.getId(), "same-tx-poster");
            assertThat(draft.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        });
        assertEntryCounts(2);
        assertBalances("100.00");
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void durableIdentityRejectsAdapterReplayWithoutAdditionalFinancialEffects(Mode mode) {
        GeneralLedger snapshot = transactions.execute(status ->
                GeneralLedger.fromApproved(journals.findByIdWithDetails(journalId).orElseThrow()));
        posting(mode).postJournalEntry(journalId, "original");
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> entries(mode).save(snapshot)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertPostedExactlyOnce("original");
        // Verify SL independently; a replay rejected at GL alone would not prove the SL constraint.
        for (String table : List.of("gl_entries", "sl_entries")) {
            assertThatThrownBy(() -> jdbc.update("INSERT INTO " + table
                    + " (journal_detail_id, account_code, dr_amount, cr_amount, base_dr_amount, base_cr_amount)"
                    + " SELECT journal_detail_id, account_code, dr_amount, cr_amount, base_dr_amount, base_cr_amount"
                    + " FROM " + table + " ORDER BY id LIMIT 1"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbc.update("INSERT INTO " + table
                    + " (journal_detail_id, account_code, dr_amount, cr_amount, base_dr_amount, base_cr_amount)"
                    + " VALUES (NULL, '10100', 100, 0, 100, 0)"))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
        assertPostedExactlyOnce("original");
    }

    private void assertPostedExactlyOnce(String poster) {
        assertThat(jdbc.queryForObject("SELECT status FROM journal_entries WHERE id = ?", String.class, journalId))
                .isEqualTo("POSTED");
        assertThat(jdbc.queryForObject("SELECT audit_user FROM journal_entries WHERE id = ?", String.class, journalId))
                .isEqualTo(poster);
        assertEntryCounts(2);
        for (String table : List.of("gl_entries", "sl_entries")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT journal_detail_id) FROM " + table, Integer.class))
                    .isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT SUM(base_dr_amount) FROM " + table, BigDecimal.class))
                    .isEqualByComparingTo("100.00");
            assertThat(jdbc.queryForObject("SELECT SUM(base_cr_amount) FROM " + table, BigDecimal.class))
                    .isEqualByComparingTo("100.00");
        }
        assertBalances("100.00");
    }

    private long seedPendingReversalOperation(String suffix) {
        long originalId = journalId + 200_000L;
        jdbc.update("INSERT INTO journal_entries"
                        + " (id, slip_no, slip_date, accounting_date, status, entry_type, currency_code,"
                        + " created_by, audit_user)"
                        + " VALUES (?, ?, ?, ?, 'POSTED', 'NORMAL', 'KRW', 'maker', 'poster')",
                originalId, "GL759-" + suffix, DATE, DATE);
        jdbc.update("UPDATE journal_entries SET entry_type = 'REVERSAL',"
                        + " lineage_source_type = 'JOURNAL_ENTRY', lineage_source_id = ? WHERE id = ?",
                Long.toString(originalId), journalId);
        jdbc.update("INSERT INTO journal_reversal_operations"
                        + " (original_journal_entry_id, reversal_journal_entry_id, status, created_at, updated_at)"
                        + " VALUES (?, ?, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)",
                originalId, journalId);
        return originalId;
    }

    private void assertEntryCounts(int expected) {
        for (String table : List.of("gl_entries", "sl_entries")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).as(table + " entry count").isEqualTo(expected);
        }
    }

    private void assertBalances(String amount) {
        for (String table : List.of("gl_balances", "sl_balances")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT SUM(debit_amount) FROM " + table, BigDecimal.class))
                    .isEqualByComparingTo(amount);
            assertThat(jdbc.queryForObject("SELECT SUM(credit_amount) FROM " + table, BigDecimal.class))
                    .isEqualByComparingTo(amount);
        }
    }

    private PostingService posting(Mode mode) { return mode == Mode.JPA ? jpaPosting : jdbcPosting; }
    private LedgerEntryPersistencePort entries(Mode mode) { return mode == Mode.JPA ? jpaEntries : jdbcEntries; }

    private JournalEntry journal() {
        JournalEntry journal = new JournalEntry();
        journal.setSlipNo("GL760-CONCURRENT");
        journal.setSlipDate(DATE);
        journal.setAccountingDate(DATE);
        journal.setCurrencyCode("KRW");
        journal.setLineageSourceType("TEST");
        journal.setLineageSourceId("GL760");
        journal.setCreatedBy("maker");
        journal.addDetail(detail(JournalSide.DEBIT, "10100"));
        journal.addDetail(detail(JournalSide.CREDIT, "40100"));
        journal.initializeDraft();
        return journal;
    }

    private JournalDetail detail(JournalSide side, String account) {
        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountCode(account);
        detail.setAmount(new BigDecimal("100.00"));
        detail.setBaseAmount(new BigDecimal("100.00"));
        detail.setBusinessPartnerCode("BP-760");
        detail.setDepartmentCode("D-760");
        return detail;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timed out waiting for posting test coordination");
            }
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("posting test interrupted", interrupted);
        }
    }

    private record Attempt(long connectionId, RuntimeException failure) { }

    static class EntryGate {
        volatile boolean holdFirstWriter;
        volatile boolean failBeforeInsert;
        CountDownLatch beforeEntries;
        CountDownLatch release;
        final AtomicInteger writes = new AtomicInteger();

        void reset() {
            holdFirstWriter = false;
            failBeforeInsert = false;
            beforeEntries = new CountDownLatch(1);
            release = new CountDownLatch(1);
            writes.set(0);
        }

        LedgerEntryPersistencePort wrap(LedgerEntryPersistencePort delegate, EntityManager entityManager) {
            return ledger -> {
                if (holdFirstWriter && writes.incrementAndGet() == 1) {
                    beforeEntries.countDown();
                    await(release);
                }
                if (failBeforeInsert) {
                    // Persist the claimed POSTED header first, proving database rollback, not just
                    // disposal of an unflushed in-memory state. No ledger entry has been inserted.
                    entityManager.flush();
                    throw new IllegalStateException("injected failure before entries");
                }
                delegate.save(ledger);
            };
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.ho.account.journalledger.infrastructure.persistence")
    @EnableJpaRepositories("com.ho.account.journalledger.infrastructure.persistence.repository")
    @Import({JournalPersistenceAdapter.class, JournalReversalPersistenceAdapter.class})
    static class PostingApplication {
        @Bean EntryGate entryGate() { return new EntryGate(); }

        @Bean LedgerEntryPersistencePort jpaEntries(GlEntryRepository gl, SlEntryRepository sl, EntityManager em) {
            return new LedgerEntryPersistenceAdapter(gl, sl, em);
        }

        @Bean LedgerEntryPersistencePort jdbcEntries(JdbcTemplate jdbc) {
            return new JdbcLedgerEntryBulkPersistenceAdapter(jdbc);
        }

        @Bean("jpaBalances") LedgerBalancePersistencePort jpaBalances(
                GlBalanceRepository gl, SlBalanceRepository sl, JournalDetailRepository details, EntityManager em) {
            LedgerBalancePersistencePort balances = new LedgerBalancePersistenceAdapter(gl, sl, details);
            org.springframework.test.util.ReflectionTestUtils.setField(balances, "entityManager", em);
            return balances;
        }

        @Bean("jdbcBalances") LedgerBalancePersistencePort jdbcBalances(
                JdbcTemplate jdbc, GlBalanceRepository gl, SlBalanceRepository sl,
                JournalDetailRepository details, EntityManager em) {
            LedgerBalancePersistencePort balances = new JdbcLedgerBalanceBulkPersistenceAdapter(jdbc, gl, sl, details);
            org.springframework.test.util.ReflectionTestUtils.setField(balances, "entityManager", em);
            return balances;
        }

        @Bean JdbcBalanceReaggregationControlAdapter reaggregationControl(JdbcTemplate jdbc) {
            return new JdbcBalanceReaggregationControlAdapter(jdbc);
        }

        @Bean BalanceReaggregationService reaggregationService(
                @Qualifier("jpaBalances") LedgerBalancePersistencePort balances,
                JdbcBalanceReaggregationControlAdapter control) {
            return new BalanceReaggregationService(balances, control);
        }

        @Bean JournalEntryService reversalCommands(
                JournalPersistencePort journals,
                JournalReversalPersistencePort reversals,
                @Qualifier("jpaPosting") PostingService posting) {
            return new JournalEntryService(journals, reversals,
                    org.mockito.Mockito.mock(JournalRuleEngine.class), posting,
                    new JournalValidationEngine(List.of()));
        }

        @Bean PostingService jpaPosting(JournalPersistencePort journals,
                JournalReversalPersistencePort reversals,
                @Qualifier("jpaEntries") LedgerEntryPersistencePort entries,
                @Qualifier("jpaBalances") LedgerBalancePersistencePort balances,
                EntryGate gate, EntityManager em, JdbcBalanceReaggregationControlAdapter control) {
            return service(journals, reversals, gate.wrap(entries, em), balances, em, control);
        }

        @Bean PostingService jdbcPosting(JournalPersistencePort journals,
                JournalReversalPersistencePort reversals,
                @Qualifier("jdbcEntries") LedgerEntryPersistencePort entries, JdbcTemplate jdbc,
                @Qualifier("jdbcBalances") LedgerBalancePersistencePort balances,
                EntryGate gate, EntityManager em, JdbcBalanceReaggregationControlAdapter control) {
            return service(journals, reversals, gate.wrap(entries, em), balances, em, control);
        }

        private PostingService service(JournalPersistencePort journals, JournalReversalPersistencePort reversals,
                LedgerEntryPersistencePort entries,
                LedgerBalancePersistencePort balances, EntityManager em,
                JdbcBalanceReaggregationControlAdapter control) {
            // These adapters are nested fixture objects, so Spring cannot inject their persistence context.
            org.springframework.test.util.ReflectionTestUtils.setField(balances, "entityManager", em);
            return new PostingService(journals, reversals, entries, new LedgerService(balances, control),
                    new ClosingLockValidationFilter(date -> false));
        }
    }
}
