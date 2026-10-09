package com.ho.account.journalledger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class SlipNumberSequenceMigrationTest {
    private static final LocalDate PEAK_DAY = LocalDate.of(2026, 10, 9);

    @Test
    void migrationSupportsFiveThousandPersistedSameDaySlips() {
        JdbcTemplate jdbc = migratedDatabase();
        List<String> numbers = new ArrayList<>(5_000);

        for (int i = 0; i < 5_000; i++) {
            String number = slip(jdbc);
            persist(jdbc, number);
            numbers.add(number);
        }

        assertThat(numbers).allSatisfy(number -> assertThat(number).hasSize(20));
        assertThat(new HashSet<>(numbers)).hasSize(5_000);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class))
                .isEqualTo(5_000);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT slip_no) FROM journal_entries", Integer.class))
                .isEqualTo(5_000);
    }

    @Test
    void concurrentAllocationAndPersistenceAreUnique() throws Exception {
        JdbcTemplate jdbc = migratedDatabase();
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Callable<String>> calls = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                calls.add(() -> {
                    String number = slip(jdbc);
                    persist(jdbc, number);
                    return number;
                });
            }
            List<Future<String>> futures = executor.invokeAll(calls, 30, TimeUnit.SECONDS);
            Set<String> numbers = new HashSet<>();
            for (Future<String> future : futures) {
                assertThat(future.isCancelled()).isFalse();
                numbers.add(future.get());
            }
            assertThat(numbers).hasSize(200);
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class))
                    .isEqualTo(200);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void exhaustedSequenceFailsWithoutCyclingOrInserting() {
        JdbcTemplate jdbc = migratedDatabase();
        jdbc.execute("ALTER SEQUENCE journal_slip_no_seq RESTART WITH 2821109907455");
        String last = slip(jdbc);
        persist(jdbc, last);

        assertThatThrownBy(() -> slip(jdbc)).isInstanceOf(RuntimeException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isOne();
    }

    @Test
    void upgradeRefusesExistingNumbersInNewNamespace() {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrate(database, "18");
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        persist(jdbc, "JE-20261009-00000001");

        assertThatThrownBy(() -> migrate(database, "19"))
                .isInstanceOf(FlywayException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isOne();
    }

    @Test
    void upgradePreservesLegacyFourCharacterNumbers() {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrate(database, "18");
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        persist(jdbc, "JE-20261009-0001");

        migrate(database, "19");

        assertThat(slip(jdbc)).isEqualTo("JE-20261009-00000001");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isOne();
    }

    private JdbcTemplate migratedDatabase() {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrate(database, "19");
        return new JdbcTemplate(database.dataSource());
    }

    private void migrate(PostingTestDatabase database, String target) {
        Flyway.configure().dataSource(database.dataSource())
                .locations("classpath:db/journal-migration")
                .schemas(database.schema())
                .target(target)
                .load().migrate();
    }

    private String slip(JdbcTemplate jdbc) {
        long value = jdbc.queryForObject("SELECT nextval('journal_slip_no_seq')", Long.class);
        String suffix = Long.toString(value, 36).toUpperCase(java.util.Locale.ROOT);
        return "JE-" + PEAK_DAY.format(DateTimeFormatter.BASIC_ISO_DATE)
                + "-" + "0".repeat(8 - suffix.length()) + suffix;
    }

    private void persist(JdbcTemplate jdbc, String number) {
        jdbc.update("""
                INSERT INTO journal_entries (accounting_date, slip_date, slip_no, status)
                VALUES (?, ?, ?, 'DRAFT')
                """, PEAK_DAY, PEAK_DAY, number);
    }
}
