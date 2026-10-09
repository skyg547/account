package com.ho.account.journalledger.infrastructure.persistence;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalReversalPersistencePort;
import com.ho.account.journalledger.application.port.out.LedgerBalancePersistencePort;
import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.application.service.journal.validator.ClosingLockValidationFilter;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.infrastructure.persistence.repository.JournalDetailRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.domain.SlBalance;
import com.ho.account.journalledger.infrastructure.persistence.repository.GlBalanceRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.GlEntryRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.SlBalanceRepository;
import com.ho.account.journalledger.infrastructure.persistence.repository.SlEntryRepository;
import jakarta.persistence.EntityManager;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Real repositories and transactions; the proxy only pauses a read to expose a lost-update race. */
@SpringBootTest(classes = LedgerBalanceConcurrencyIntegrationTest.PostingApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = {
        "spring.config.name=ledger-balance-concurrency-test",
        "spring.jpa.hibernate.ddl-auto=validate", "spring.jpa.open-in-view=false",
        "spring.flyway.enabled=true", "spring.flyway.locations=classpath:db/journal-migration",
        "spring.sql.init.mode=never", "spring.cloud.config.enabled=false",
        "spring.cloud.discovery.enabled=false", "spring.cloud.vault.enabled=false",
        "eureka.client.enabled=false"
})
class LedgerBalanceConcurrencyIntegrationTest {
    private static final PostingTestDatabase DATABASE = PostingTestDatabase.create();
    private static final LocalDate DATE = LocalDate.of(2026, 9, 24);
    private static final List<String> ACCOUNTS = List.of("10100", "40100");
    private static final AtomicLong FIXTURE_SEQUENCE = new AtomicLong();
    enum Mode { JPA, JDBC }
    record Dimensions(String partner, String department) { }

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", DATABASE::url);
        registry.add("spring.datasource.username", DATABASE::username);
        registry.add("spring.datasource.password", () -> "");
        registry.add("spring.datasource.driver-class-name", DATABASE::driver);
        registry.add("spring.flyway.schemas", DATABASE::schema);
        registry.add("spring.jpa.properties.hibernate.default_schema", DATABASE::schema);
    }

    @Autowired @Qualifier("jpaPosting") private PostingService jpaPosting;
    @Autowired @Qualifier("jdbcPosting") private PostingService jdbcPosting;
    @Autowired private JournalEntryRepository journals;
    @Autowired private GlBalanceRepository glBalances;
    @Autowired private SlBalanceRepository slBalances;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;
    @Autowired private BalanceReadGate gate;
    private TransactionTemplate transactions;

    @BeforeEach
    void resetSyntheticData() {
        transactions = new TransactionTemplate(transactionManager);
        gate.reset();
        transactions.executeWithoutResult(status -> {
            for (String table : List.of("gl_entries", "sl_entries", "gl_balances", "sl_balances",
                    "journal_details", "journal_entries")) {
                jdbc.update("DELETE FROM " + table);
            }
        });
    }

    static Stream<Arguments> overlappingCases() {
        return Stream.of(Mode.values()).flatMap(mode -> Stream.of(false, true).flatMap(existing ->
                Stream.of(new Dimensions(null, null), new Dimensions(null, "D-761"),
                        new Dimensions("BP-761", null), new Dimensions("BP-761", "D-761"))
                        .map(dimensions -> Arguments.of(mode, existing, dimensions))));
    }

    static Stream<Arguments> backdatedCases() {
        List<LocalDate> boundaryDates = List.of(
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 12, 31));
        List<Dimensions> dimensions = List.of(
                new Dimensions(null, null),
                new Dimensions("BP-766", null),
                new Dimensions(null, "D-766"),
                new Dimensions("BP-766", "D-766"),
                new Dimensions("NULL", "NULL"));
        return Stream.of(Mode.values()).flatMap(mode -> boundaryDates.stream().flatMap(date ->
                dimensions.stream().map(dimension -> Arguments.of(mode, date, dimension))));
    }

    @ParameterizedTest(name = "{0}, postingDate={1}, dimensions={2}")
    @MethodSource("backdatedCases")
    void backdatedPostingShiftsEverySparseSuccessorAcrossMonthAndYearBoundaries(
            Mode mode, LocalDate postingDate, Dimensions dimensions) {
        LocalDate firstSuccessor = postingDate.plusDays(1);
        LocalDate sparseSuccessor = postingDate.plusDays(40);
        posting(mode).postJournalEntry(seedJournal("BD-OPEN-" + mode + "-" + postingDate + "-" + dimensions,
                postingDate, "100.00", false, dimensions, ACCOUNTS), "initial-poster");
        posting(mode).postJournalEntry(seedJournal("BD-NEXT-" + mode + "-" + postingDate + "-" + dimensions,
                firstSuccessor, "20.00", false, dimensions, ACCOUNTS), "next-poster");
        posting(mode).postJournalEntry(seedJournal("BD-SPARSE-" + mode + "-" + postingDate + "-" + dimensions,
                sparseSuccessor, "5.00", false, dimensions, ACCOUNTS), "sparse-poster");

        posting(mode).postJournalEntry(seedJournal("BD-LATE-" + mode + "-" + postingDate + "-" + dimensions,
                postingDate, "10.00", false, dimensions, ACCOUNTS), "late-poster");

        assertSuccessor("gl_balances", "10100", firstSuccessor, null, "110.00", "20.00", "0.00", "130.00");
        assertSuccessor("gl_balances", "10100", sparseSuccessor, null, "130.00", "5.00", "0.00", "135.00");
        assertSuccessor("gl_balances", "40100", firstSuccessor, null, "-110.00", "0.00", "20.00", "-130.00");
        assertSuccessor("gl_balances", "40100", sparseSuccessor, null, "-130.00", "0.00", "5.00", "-135.00");
        assertSuccessor("sl_balances", "10100", firstSuccessor, dimensions,
                "110.00", "20.00", "0.00", "130.00");
        assertSuccessor("sl_balances", "10100", sparseSuccessor, dimensions,
                "130.00", "5.00", "0.00", "135.00");
        assertSuccessor("sl_balances", "40100", firstSuccessor, dimensions,
                "-110.00", "0.00", "20.00", "-130.00");
        assertSuccessor("sl_balances", "40100", sparseSuccessor, dimensions,
                "-130.00", "0.00", "5.00", "-135.00");
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void netZeroBackdatedMovementDoesNotShiftSuccessors(Mode mode) {
        LocalDate postingDate = LocalDate.of(2026, 5, 31);
        LocalDate successor = postingDate.plusDays(1);
        Dimensions dimensions = new Dimensions("BP-ZERO", "D-ZERO");
        List<String> sameAccount = List.of("10100", "10100");
        posting(mode).postJournalEntry(seedJournal("ZERO-OPEN-" + mode, postingDate,
                "100.00", false, dimensions, sameAccount), "initial-poster");
        posting(mode).postJournalEntry(seedJournal("ZERO-NEXT-" + mode, successor,
                "20.00", false, dimensions, sameAccount), "next-poster");

        posting(mode).postJournalEntry(seedJournal("ZERO-LATE-" + mode, postingDate,
                "10.00", false, dimensions, sameAccount), "late-poster");

        assertSuccessor("gl_balances", "10100", successor, null,
                "0.00", "20.00", "20.00", "0.00");
        assertSuccessor("sl_balances", "10100", successor, dimensions,
                "0.00", "20.00", "20.00", "0.00");
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void literalNullSlCodesRemainDistinctFromAbsentDimensionsDuringRepair(Mode mode) {
        LocalDate postingDate = LocalDate.of(2026, 6, 30);
        LocalDate successor = postingDate.plusDays(1);
        Dimensions absent = new Dimensions(null, null);
        Dimensions literal = new Dimensions("NULL", "NULL");
        for (Dimensions dimensions : List.of(absent, literal)) {
            posting(mode).postJournalEntry(seedJournal("NULL-OPEN-" + mode + "-" + dimensions,
                    postingDate, "100.00", false, dimensions, ACCOUNTS), "initial-poster");
            posting(mode).postJournalEntry(seedJournal("NULL-NEXT-" + mode + "-" + dimensions,
                    successor, "20.00", false, dimensions, ACCOUNTS), "next-poster");
        }

        posting(mode).postJournalEntry(seedJournal("NULL-LATE-" + mode,
                postingDate, "10.00", false, literal, ACCOUNTS), "late-poster");

        assertSuccessor("sl_balances", "10100", successor, absent,
                "100.00", "20.00", "0.00", "120.00");
        assertSuccessor("sl_balances", "10100", successor, literal,
                "110.00", "20.00", "0.00", "130.00");
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void successorOverflowRollsBackJournalEntriesPostingDayAndEveryShift(Mode mode) {
        LocalDate postingDate = LocalDate.of(2026, 7, 31);
        LocalDate successor = postingDate.plusDays(1);
        Dimensions dimensions = new Dimensions(null, null);
        BigDecimal databaseMaximum = new BigDecimal("99999999999999999.99");
        seedBalances(successor, dimensions, databaseMaximum);
        Long journalId = seedJournal("OVERFLOW-" + mode, postingDate,
                "1.00", false, dimensions, ACCOUNTS);

        assertThatThrownBy(() -> posting(mode).postJournalEntry(journalId, "overflow-poster"))
                .isInstanceOf(RuntimeException.class);

        assertThat(jdbc.queryForObject("SELECT status FROM journal_entries WHERE id = ?",
                String.class, journalId)).isEqualTo("APPROVED");
        for (String table : List.of("gl_entries", "sl_entries")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isZero();
        }
        for (String table : List.of("gl_balances", "sl_balances")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE balance_date = ?",
                    Integer.class, postingDate)).isZero();
            assertThat(jdbc.queryForObject("SELECT beginning_balance FROM " + table
                            + " WHERE account_code = '10100' AND balance_date = ?",
                    BigDecimal.class, successor)).isEqualByComparingTo(databaseMaximum);
            assertThat(jdbc.queryForObject("SELECT ending_balance FROM " + table
                            + " WHERE account_code = '10100' AND balance_date = ?",
                    BigDecimal.class, successor)).isEqualByComparingTo(databaseMaximum);
        }
    }

    @ParameterizedTest(name = "{0}, existing={1}, dimensions={2}")
    @MethodSource("overlappingCases")
    void distinctJournalsPreserveEveryDebitAndCredit(Mode mode, boolean existing, Dimensions dimensions)
            throws Exception {
        if (existing) seedBalances(DATE, dimensions, BigDecimal.ZERO);
        runOverlappingPostings(mode, dimensions, false);
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void followingDayCarriesForwardTheCommittedPreviousDay(Mode mode) throws Exception {
        Dimensions dimensions = new Dimensions(null, null);
        seedBalances(DATE.minusDays(1), dimensions, new BigDecimal("500.00"));
        runOverlappingPostings(mode, dimensions, true);
    }

    private void runOverlappingPostings(Mode mode, Dimensions dimensions, boolean followingDay) throws Exception {
        Long firstId = seedJournal("FIRST", DATE, "100.00", false, dimensions, ACCOUNTS);
        Long secondId = seedJournal("SECOND", followingDay ? DATE.plusDays(1) : DATE,
                "40.00", true, dimensions, ACCOUNTS);
        CountDownLatch bothLoaded = new CountDownLatch(2);
        CountDownLatch secondStarted = new CountDownLatch(1);
        AtomicLong secondConnection = new AtomicLong();
        gate.pauseNextRead.set(true);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> first = executor.submit(() -> attempt(() -> {
                preloadBalances(bothLoaded);
                posting(mode).postJournalEntry(firstId, "first-poster");
            }));
            Future<Attempt> second = executor.submit(() -> attempt(() -> {
                preloadBalances(bothLoaded);
                await(gate.firstRead);
                secondConnection.set(connectionId());
                secondStarted.countDown();
                posting(mode).postJournalEntry(secondId, "second-poster");
            }));
            await(secondStarted);
            boolean lockObserved;
            try {
                lockObserved = awaitDatabaseLock(secondConnection.get(), second);
            } finally {
                // On the old implementation the second commit wins this race. Release even then,
                // so the regression fails on missing money or duplicate keys rather than a timeout.
                gate.release.countDown();
            }
            Attempt firstResult = first.get(20, TimeUnit.SECONDS);
            Attempt secondResult = second.get(20, TimeUnit.SECONDS);
            assertFinancialTotals(2, 4, "140.00");
            assertThat(firstResult.failure()).isNull();
            assertThat(secondResult.failure()).isNull();
            assertThat(firstResult.connectionId()).isNotEqualTo(secondResult.connectionId());
            assertThat(lockObserved).as("second connection waits for the shared balance lock").isTrue();
            assertDailyBalances(dimensions, followingDay);
        } finally {
            gate.release.countDown();
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    private void preloadBalances(CountDownLatch bothLoaded) {
        // Dirty-checking and first-level caches must not replace balances committed while waiting.
        glBalances.findAll();
        slBalances.findAll();
        bothLoaded.countDown();
        await(bothLoaded);
    }

    private boolean awaitDatabaseLock(long connection, Future<Attempt> second) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(3);
        while (System.nanoTime() < deadline && !second.isDone()) {
            if (postgres() && jdbc.queryForObject(
                    "SELECT COUNT(*) FROM pg_stat_activity WHERE pid = ? AND wait_event_type = 'Lock'",
                    Integer.class, connection) == 1) return true;
            if (!postgres() && System.nanoTime() > deadline - TimeUnit.MILLISECONDS.toNanos(2700)) return true;
            Thread.sleep(10);
        }
        return false;
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void databaseDeadlockRollsBackAllEffectsAndWholePostingRetriesInANewTransaction(Mode mode) throws Exception {
        Dimensions dimensions = new Dimensions(null, null);
        Long firstId = seedJournal("DEADLOCK-A", DATE, "100.00", false, dimensions, ACCOUNTS);
        Long secondId = seedJournal("DEADLOCK-B", DATE, "40.00", true, dimensions, List.of("10101", "40101"));
        // Separate test rows induce a genuine database cycle after ALL posting writes. Production
        // balance-lock ordering stays intact; unrelated locks can still deadlock in a larger caller.
        jdbc.execute("CREATE TABLE IF NOT EXISTS balance_deadlock_probe (id INTEGER PRIMARY KEY)");
        jdbc.update("DELETE FROM balance_deadlock_probe");
        jdbc.update("INSERT INTO balance_deadlock_probe (id) VALUES (1), (2)");
        CountDownLatch bothPosted = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Attempt> first = executor.submit(() -> deadlockAttempt(mode, firstId, 1, 2, bothPosted));
            Future<Attempt> second = executor.submit(() -> deadlockAttempt(mode, secondId, 2, 1, bothPosted));
            Attempt a = first.get(20, TimeUnit.SECONDS);
            Attempt b = second.get(20, TimeUnit.SECONDS);
            assertThat(a.connectionId()).isNotEqualTo(b.connectionId());
            assertThat(Stream.of(a, b).filter(result -> result.failure() != null).count()).isEqualTo(1);
            Attempt victim = a.failure() != null ? a : b;
            Long victimId = a.failure() != null ? firstId : secondId;
            assertThat(sqlState(victim.failure())).isEqualTo(postgres() ? "40P01" : "40001");
            assertThat(jdbc.queryForObject("SELECT status FROM journal_entries WHERE id = ?", String.class, victimId))
                    .isEqualTo("APPROVED");
            assertThat(jdbc.queryForObject("SELECT audit_user FROM journal_entries WHERE id = ?", String.class, victimId))
                    .isEqualTo("approver");
            for (String table : List.of("gl_entries", "sl_entries")) {
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table
                        + " e JOIN journal_details d ON d.id = e.journal_detail_id WHERE d.journal_entry_id = ?",
                        Integer.class, victimId)).isZero();
            }
            assertFinancialTotals(1, 2, a.failure() == null ? "100.00" : "40.00");
            for (String table : List.of("gl_balances", "sl_balances")) {
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(2);
            }
            Attempt retried = attempt(() -> posting(mode).postJournalEntry(victimId, "retry-poster"));
            assertThat(retried.failure()).isNull();
            if (postgres()) assertThat(retried.transactionId()).isNotEqualTo(victim.transactionId());
            assertFinancialTotals(2, 4, "140.00");
            for (String table : List.of("gl_balances", "sl_balances")) {
                assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(4);
            }
            assertThatThrownBy(() -> posting(mode).postJournalEntry(victimId, "duplicate-retry"))
                    .isInstanceOf(IllegalStateException.class);
            assertFinancialTotals(2, 4, "140.00");
        } finally {
            executor.shutdownNow();
            assertThat(executor.awaitTermination(20, TimeUnit.SECONDS)).isTrue();
        }
    }

    private Attempt deadlockAttempt(Mode mode, Long journalId, int ownLock, int otherLock, CountDownLatch bothPosted) {
        return attempt(() -> {
            jdbc.queryForObject("SELECT id FROM balance_deadlock_probe WHERE id = ? FOR UPDATE", Integer.class, ownLock);
            posting(mode).postJournalEntry(journalId, "deadlock-poster");
            entityManager.flush();
            bothPosted.countDown();
            await(bothPosted);
            jdbc.queryForObject("SELECT id FROM balance_deadlock_probe WHERE id = ? FOR UPDATE", Integer.class, otherLock);
        });
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void snapshotIsolationIsRejectedAndEarlierPostingWritesRollBack(Mode mode) {
        Long id = seedJournal("ISOLATION", DATE, "100.00", false, new Dimensions(null, null), ACCOUNTS);
        TransactionTemplate snapshot = new TransactionTemplate(transactionManager);
        snapshot.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
        assertThatThrownBy(() -> snapshot.executeWithoutResult(status -> posting(mode).postJournalEntry(id, "snapshot")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("READ_COMMITTED");
        assertThat(jdbc.queryForObject("SELECT status FROM journal_entries WHERE id = ?", String.class, id))
                .isEqualTo("APPROVED");
        for (String table : List.of("gl_entries", "sl_entries", "gl_balances", "sl_balances")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isZero();
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void missingLockStripeRejectsPostingAndRollsBackEarlierWrites(Mode mode) {
        Long id = seedJournal("MISSING-LOCK", DATE, "100.00", false, new Dimensions(null, null), ACCOUNTS);
        int stripe = Math.floorMod(31 * ACCOUNTS.get(0).hashCode() + "KRW".hashCode(), 256);
        assertThatThrownBy(() -> transactions.executeWithoutResult(status -> {
            // Removing the seed in this transaction simulates incomplete migration without
            // leaving the next test with broken lock infrastructure after the expected rollback.
            assertThat(jdbc.update("DELETE FROM ledger_balance_locks WHERE lock_id = ?", stripe)).isEqualTo(1);
            posting(mode).postJournalEntry(id, "missing-lock");
        })).isInstanceOf(IllegalStateException.class).hasMessageContaining("V14");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM ledger_balance_locks", Integer.class)).isEqualTo(256);
        assertThat(jdbc.queryForObject("SELECT status FROM journal_entries WHERE id = ?", String.class, id))
                .isEqualTo("APPROVED");
        for (String table : List.of("gl_entries", "sl_entries", "gl_balances", "sl_balances")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isZero();
        }
    }

    @ParameterizedTest
    @EnumSource(Mode.class)
    void repeatedPostingsInsideOneTransactionRetainEveryBalanceUpdate(Mode mode) {
        Dimensions dimensions = new Dimensions(null, null);
        seedBalances(DATE, dimensions, BigDecimal.ZERO);
        List<Long> ids = List.of(seedJournal("AMBIENT-A", DATE, "100.00", false, dimensions, ACCOUNTS),
                seedJournal("AMBIENT-B", DATE, "40.00", true, dimensions, ACCOUNTS),
                seedJournal("AMBIENT-C", DATE, "10.00", true, dimensions, ACCOUNTS));
        transactions.executeWithoutResult(status -> {
            for (Long id : ids) posting(mode).postJournalEntry(id, "ambient-poster");
        });
        assertFinancialTotals(3, 6, "150.00");
        for (String table : List.of("gl_balances", "sl_balances")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(2);
            assertThat(jdbc.queryForObject("SELECT ending_balance FROM " + table + " WHERE account_code = '10100'",
                    BigDecimal.class)).isEqualByComparingTo("50.00");
            assertThat(jdbc.queryForObject("SELECT ending_balance FROM " + table + " WHERE account_code = '40100'",
                    BigDecimal.class)).isEqualByComparingTo("-50.00");
        }
    }

    private void assertFinancialTotals(int postedJournals, int entries, String amount) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries WHERE status = 'POSTED'", Integer.class))
                .isEqualTo(postedJournals);
        for (String side : List.of("DEBIT", "CREDIT")) {
            assertThat(jdbc.queryForObject("SELECT SUM(d.base_amount) FROM journal_details d"
                    + " JOIN journal_entries j ON j.id = d.journal_entry_id WHERE j.status = 'POSTED' AND d.side = ?",
                    BigDecimal.class, side)).isEqualByComparingTo(amount);
        }
        for (String table : List.of("gl_entries", "sl_entries")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(entries);
            assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT journal_detail_id) FROM " + table, Integer.class))
                    .isEqualTo(entries);
            for (String column : List.of("base_dr_amount", "base_cr_amount")) {
                assertThat(jdbc.queryForObject("SELECT SUM(" + column + ") FROM " + table, BigDecimal.class))
                        .as(table + "." + column).isEqualByComparingTo(amount);
            }
        }
        for (String table : List.of("gl_balances", "sl_balances")) {
            for (String column : List.of("debit_amount", "credit_amount")) {
                assertThat(jdbc.queryForObject("SELECT SUM(" + column + ") FROM " + table
                        + " WHERE balance_date >= ?", BigDecimal.class, DATE)).as(table + "." + column)
                        .isEqualByComparingTo(amount);
            }
        }
    }

    private void assertDailyBalances(Dimensions dimensions, boolean followingDay) {
        for (String table : List.of("gl_balances", "sl_balances")) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE balance_date >= ?",
                    Integer.class, DATE)).isEqualTo(followingDay ? 4 : 2);
            for (int account = 0; account < ACCOUNTS.size(); account++) {
                for (int day = 0; day < (followingDay ? 2 : 1); day++) {
                    var row = jdbc.queryForMap("SELECT * FROM " + table + " WHERE account_code = ? AND balance_date = ?",
                            ACCOUNTS.get(account), DATE.plusDays(day));
                    BigDecimal beginning = followingDay
                            ? BigDecimal.valueOf((account == 0 ? 1 : -1) * (day == 0 ? 500 : 600)) : BigDecimal.ZERO;
                    BigDecimal debit = new BigDecimal(account == 0 ? (day == 0 ? "100.00" : "0.00")
                            : (!followingDay || day == 1 ? "40.00" : "0.00"));
                    BigDecimal credit = new BigDecimal(account == 1 ? (day == 0 ? "100.00" : "0.00")
                            : (!followingDay || day == 1 ? "40.00" : "0.00"));
                    assertThat((BigDecimal) row.get("beginning_balance")).isEqualByComparingTo(beginning);
                    assertThat((BigDecimal) row.get("debit_amount")).isEqualByComparingTo(debit);
                    assertThat((BigDecimal) row.get("credit_amount")).isEqualByComparingTo(credit);
                    assertThat((BigDecimal) row.get("ending_balance"))
                            .isEqualByComparingTo(beginning.add(debit).subtract(credit));
                    if (table.equals("sl_balances")) {
                        assertThat(row.get("bp_code")).isEqualTo(dimensions.partner());
                        assertThat(row.get("dept_code")).isEqualTo(dimensions.department());
                    }
                }
            }
        }
    }

    private void assertSuccessor(String table, String accountCode, LocalDate date, Dimensions dimensions,
                                 String beginning, String debit, String credit, String ending) {
        StringBuilder dimensionPredicate = new StringBuilder();
        List<Object> parameters = new java.util.ArrayList<>(List.of(accountCode, date));
        if (dimensions != null) {
            if (dimensions.partner() == null) dimensionPredicate.append(" AND bp_code IS NULL");
            else { dimensionPredicate.append(" AND bp_code = ?"); parameters.add(dimensions.partner()); }
            if (dimensions.department() == null) dimensionPredicate.append(" AND dept_code IS NULL");
            else { dimensionPredicate.append(" AND dept_code = ?"); parameters.add(dimensions.department()); }
        }
        var row = jdbc.queryForMap("SELECT * FROM " + table
                + " WHERE account_code = ? AND balance_date = ?" + dimensionPredicate, parameters.toArray());
        assertThat((BigDecimal) row.get("beginning_balance")).isEqualByComparingTo(beginning);
        assertThat((BigDecimal) row.get("debit_amount")).isEqualByComparingTo(debit);
        assertThat((BigDecimal) row.get("credit_amount")).isEqualByComparingTo(credit);
        assertThat((BigDecimal) row.get("ending_balance")).isEqualByComparingTo(ending);
    }

    private Long seedJournal(String suffix, LocalDate date, String amount, boolean reversed,
                             Dimensions dimensions, List<String> accounts) {
        return transactions.execute(status -> {
            String syntheticId = "G" + FIXTURE_SEQUENCE.incrementAndGet() + "-"
                    + Integer.toUnsignedString(suffix.hashCode(), 36);
            JournalEntry journal = new JournalEntry();
            journal.setSlipNo(syntheticId);
            journal.setSlipDate(date);
            journal.setAccountingDate(date);
            journal.setCurrencyCode("KRW");
            journal.setLineageSourceType("TEST");
            journal.setLineageSourceId(syntheticId);
            journal.setCreatedBy("maker");
            for (int index : reversed ? List.of(1, 0) : List.of(0, 1)) {
                JournalDetail detail = new JournalDetail();
                detail.setSide(index == (reversed ? 1 : 0) ? JournalSide.DEBIT : JournalSide.CREDIT);
                detail.setAccountCode(accounts.get(index));
                detail.setAmount(new BigDecimal(amount));
                detail.setBaseAmount(new BigDecimal(amount));
                detail.setBusinessPartnerCode(dimensions.partner());
                detail.setDepartmentCode(dimensions.department());
                journal.addDetail(detail);
            }
            journal.initializeDraft();
            journal.requestApproval("maker");
            journal.approve("approver");
            return journals.saveAndFlush(journal).getId();
        });
    }

    private void seedBalances(LocalDate date, Dimensions dimensions, BigDecimal debitBeginning) {
        transactions.executeWithoutResult(status -> {
            for (int index = 0; index < ACCOUNTS.size(); index++) {
                BigDecimal beginning = index == 0 ? debitBeginning : debitBeginning.negate();
                GlBalance gl = new GlBalance();
                gl.setAccountCode(ACCOUNTS.get(index));
                gl.setCurrencyCode("KRW");
                gl.setBalanceDate(date);
                gl.setPeriod(YearMonth.from(date));
                gl.setBeginningBalance(beginning);
                glBalances.save(gl);
                SlBalance sl = new SlBalance();
                sl.setAccountCode(ACCOUNTS.get(index));
                sl.setCurrencyCode("KRW");
                sl.setBusinessPartnerCode(dimensions.partner());
                sl.setDepartmentCode(dimensions.department());
                sl.setBalanceDate(date);
                sl.setPeriod(YearMonth.from(date));
                sl.setBeginningBalance(beginning);
                slBalances.save(sl);
            }
        });
    }

    private Attempt attempt(Runnable work) {
        long[] identity = {-1, -1};
        try {
            transactions.executeWithoutResult(status -> {
                identity[0] = connectionId();
                if (postgres()) identity[1] = jdbc.queryForObject("SELECT txid_current()", Long.class);
                work.run();
            });
            return new Attempt(identity[0], identity[1], null);
        } catch (RuntimeException failure) {
            return new Attempt(identity[0], identity[1], failure);
        }
    }

    private long connectionId() {
        return jdbc.queryForObject(postgres() ? "SELECT pg_backend_pid()" : "SELECT session_id()", Long.class);
    }
    private static boolean postgres() { return DATABASE.driver().equals("org.postgresql.Driver"); }
    private PostingService posting(Mode mode) { return mode == Mode.JPA ? jpaPosting : jdbcPosting; }
    private record Attempt(long connectionId, long transactionId, RuntimeException failure) { }

    private static String sqlState(Throwable failure) {
        for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sql && sql.getSQLState() != null) return sql.getSQLState();
        }
        return null;
    }

    private static void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("balance test coordination timed out");
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("balance test interrupted", interrupted);
        }
    }

    static class BalanceReadGate {
        final AtomicBoolean pauseNextRead = new AtomicBoolean();
        CountDownLatch firstRead;
        CountDownLatch release;
        void reset() {
            pauseNextRead.set(false);
            firstRead = new CountDownLatch(1);
            release = new CountDownLatch(1);
        }
        LedgerBalancePersistencePort wrap(LedgerBalancePersistencePort delegate) {
            return (LedgerBalancePersistencePort) Proxy.newProxyInstance(getClass().getClassLoader(),
                    new Class<?>[]{LedgerBalancePersistencePort.class}, (proxy, method, args) -> {
                        Object result;
                        try {
                            result = method.invoke(delegate, args);
                        } catch (InvocationTargetException failure) {
                            throw failure.getCause();
                        }
                        if (method.getName().equals("findGlBalance") && pauseNextRead.compareAndSet(true, false)) {
                            firstRead.countDown();
                            await(release);
                        }
                        return result;
                    });
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan("com.ho.account.journalledger.infrastructure.persistence")
    @EnableJpaRepositories("com.ho.account.journalledger.infrastructure.persistence.repository")
    @Import({JournalPersistenceAdapter.class, JournalReversalPersistenceAdapter.class})
    static class PostingApplication {
        @Bean BalanceReadGate balanceReadGate() { return new BalanceReadGate(); }
        @Bean PostingService jpaPosting(JournalPersistencePort journals, JournalReversalPersistencePort reversals,
                GlEntryRepository glEntries,
                SlEntryRepository slEntries, GlBalanceRepository gl, SlBalanceRepository sl,
                JournalDetailRepository details, EntityManager em, BalanceReadGate gate, JdbcTemplate jdbc) {
            return service(journals, reversals, new LedgerEntryPersistenceAdapter(glEntries, slEntries, em),
                    new LedgerBalancePersistenceAdapter(gl, sl, details), em, gate, jdbc);
        }
        @Bean PostingService jdbcPosting(JournalPersistencePort journals, JournalReversalPersistencePort reversals,
                JdbcTemplate jdbc,
                GlBalanceRepository gl, SlBalanceRepository sl, JournalDetailRepository details,
                EntityManager em, BalanceReadGate gate) {
            return service(journals, reversals, new JdbcLedgerEntryBulkPersistenceAdapter(jdbc),
                    new JdbcLedgerBalanceBulkPersistenceAdapter(jdbc, gl, sl, details), em, gate, jdbc);
        }
        private PostingService service(JournalPersistencePort journals, JournalReversalPersistencePort reversals,
                LedgerEntryPersistencePort entries,
                LedgerBalancePersistencePort balances, EntityManager em, BalanceReadGate gate, JdbcTemplate jdbc) {
            // Keep the same fixture runnable against the audited pre-fix implementation.
            if (ReflectionUtils.findField(balances.getClass(), "entityManager") != null) {
                ReflectionTestUtils.setField(balances, "entityManager", em);
            }
            return new PostingService(journals, reversals, entries,
                    new LedgerService(gate.wrap(balances), new JdbcBalanceReaggregationControlAdapter(jdbc)),
                    new ClosingLockValidationFilter(date -> false));
        }
    }
}
