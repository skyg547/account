package com.ho.account.journalledger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class ReversalOperationMigrationTest {

    @Test
    void cleanMigrationCreatesLifecycleAndRelationshipConstraints() {
        PostingTestDatabase database = PostingTestDatabase.create();

        migrateTo(database, "17");

        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        insertJournal(jdbc, 1L, "POSTED", "NORMAL", null, "100.00");
        insertJournal(jdbc, 2L, "DRAFT", "REVERSAL", "1", "100.00");
        jdbc.update("INSERT INTO journal_reversal_operations"
                + " (original_journal_entry_id, reversal_journal_entry_id, status, created_at, updated_at)"
                + " VALUES (1, 2, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)");

        assertThat(jdbc.queryForObject("SELECT status FROM journal_reversal_operations"
                + " WHERE original_journal_entry_id = 1", String.class)).isEqualTo("PENDING");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO journal_reversal_operations"
                        + " (original_journal_entry_id, reversal_journal_entry_id, status, created_at, updated_at)"
                        + " VALUES (2, 2, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE journal_reversal_operations SET status = 'UNKNOWN'"
                        + " WHERE original_journal_entry_id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE journal_reversal_operations"
                        + " SET cancelled_at = CURRENT_TIMESTAMP, cancelled_by = 'operator',"
                        + " cancellation_reason = 'invalid pending audit' WHERE original_journal_entry_id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE journal_reversal_operations"
                        + " SET status = 'POSTED' WHERE original_journal_entry_id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("UPDATE journal_reversal_operations"
                        + " SET status = 'CANCELLED' WHERE original_journal_entry_id = 1"))
                .isInstanceOf(DataIntegrityViolationException.class);
        insertJournal(jdbc, 3L, "DRAFT", "NORMAL", null, "50.00");
        insertJournal(jdbc, 4L, "DRAFT", "REVERSAL", "3", "50.00");
        assertThatThrownBy(() -> jdbc.update("INSERT INTO journal_reversal_operations"
                        + " (original_journal_entry_id, reversal_journal_entry_id, status, created_at, updated_at)"
                        + " VALUES (3, 4, 'PENDING', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history"
                + " WHERE version = '17' AND success = TRUE", Integer.class)).isOne();
    }

    @Test
    void upgradeBackfillsPostedAndPendingOperationsAndLeavesRejectedDraftReusable() {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrateTo(database, "16");
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        insertJournal(jdbc, 10L, "POSTED", "NORMAL", null, "125.25");
        insertJournal(jdbc, 11L, "POSTED", "REVERSAL", "10", "125.25");
        insertJournal(jdbc, 20L, "POSTED", "NORMAL", null, "80.40");
        insertJournal(jdbc, 21L, "APPROVED", "REVERSAL", "20", "80.40");
        insertJournal(jdbc, 30L, "POSTED", "NORMAL", null, "15.00");
        insertJournal(jdbc, 31L, "REJECTED", "REVERSAL", "30", "15.00");

        migrateTo(database, "17");

        Map<Long, Map<String, Object>> operations = jdbc.query(
                "SELECT original_journal_entry_id, reversal_journal_entry_id, status"
                        + " FROM journal_reversal_operations ORDER BY original_journal_entry_id",
                resultSet -> {
                    java.util.LinkedHashMap<Long, Map<String, Object>> rows = new java.util.LinkedHashMap<>();
                    while (resultSet.next()) {
                        rows.put(resultSet.getLong("original_journal_entry_id"), Map.of(
                                "reversal", resultSet.getLong("reversal_journal_entry_id"),
                                "status", resultSet.getString("status")));
                    }
                    return rows;
                });
        assertThat(operations).containsExactly(
                Map.entry(10L, Map.of("reversal", 11L, "status", "POSTED")),
                Map.entry(20L, Map.of("reversal", 21L, "status", "PENDING")));
        assertThat(operations).doesNotContainKey(30L);
        assertThat(totalJournalAmount(jdbc)).isEqualByComparingTo("441.30");
    }

    @Test
    void upgradeBackfillsTrimmedCaseInsensitiveReversalLineageButIgnoresOtherSources() {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrateTo(database, "16");
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        insertJournal(jdbc, 40L, "POSTED", "NORMAL", null, "25.00");
        insertJournal(jdbc, 41L, "DRAFT", "reversal", "40", "25.00");
        insertJournal(jdbc, 50L, "POSTED", "NORMAL", null, "35.00");
        insertJournal(jdbc, 51L, "APPROVED", " reversal ", "50", "35.00");
        insertJournal(jdbc, 61L, "DRAFT", " reversal ", null, "45.00");
        jdbc.update("UPDATE journal_entries SET lineage_source_type = 'journal_entry',"
                + " lineage_source_id = ' 40 ' WHERE id = 41");
        jdbc.update("UPDATE journal_entries SET lineage_source_type = ' JOURNAL_ENTRY '"
                + " WHERE id = 51");

        migrateTo(database, "17");

        assertThat(jdbc.queryForList("SELECT original_journal_entry_id"
                        + " FROM journal_reversal_operations ORDER BY original_journal_entry_id", Long.class))
                .containsExactly(40L, 50L);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_reversal_operations"
                + " WHERE reversal_journal_entry_id = 61", Integer.class)).isZero();
        assertThat(totalJournalAmount(jdbc)).isEqualByComparingTo("165.00");
    }

    @Test
    void duplicateActiveLegacyReversalsFailClosedWithoutDeletingJournalAmounts() {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrateTo(database, "16");
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        insertJournal(jdbc, 100L, "POSTED", "NORMAL", null, "250.00");
        insertJournal(jdbc, 101L, "DRAFT", "REVERSAL", "100", "250.00");
        insertJournal(jdbc, 102L, "APPROVED", "REVERSAL", "100", "250.00");
        BigDecimal amountBefore = totalJournalAmount(jdbc);

        assertThatThrownBy(() -> migrateTo(database, "17")).isInstanceOf(FlywayException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isEqualTo(3);
        assertThat(totalJournalAmount(jdbc)).isEqualByComparingTo(amountBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history"
                + " WHERE version = '17' AND success = TRUE", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"NOT-A-NUMBER", "999999"})
    void invalidOrOrphanLegacyLineageFailsClosedWithoutDeletingJournalAmounts(String originalId) {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrateTo(database, "16");
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        insertJournal(jdbc, 201L, "DRAFT", "REVERSAL", originalId, "321.09");

        assertThatThrownBy(() -> migrateTo(database, "17")).isInstanceOf(FlywayException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isOne();
        assertThat(totalJournalAmount(jdbc)).isEqualByComparingTo("321.09");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history"
                + " WHERE version = '17' AND success = TRUE", Integer.class)).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"DRAFT", "APPROVED"})
    void activeLegacyReversalOfNonPostedOriginalFailsClosedAndPreservesAmounts(String originalStatus) {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrateTo(database, "16");
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        insertJournal(jdbc, 301L, originalStatus, "NORMAL", null, "440.25");
        insertJournal(jdbc, 302L, "DRAFT", "REVERSAL", "301", "440.25");
        BigDecimal amountBefore = totalJournalAmount(jdbc);

        assertThatThrownBy(() -> migrateTo(database, "17")).isInstanceOf(FlywayException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isEqualTo(2);
        assertThat(totalJournalAmount(jdbc)).isEqualByComparingTo(amountBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history"
                + " WHERE version = '17' AND success = TRUE", Integer.class)).isZero();
    }

    private static void migrateTo(PostingTestDatabase database, String version) {
        Flyway.configure().dataSource(database.dataSource())
                .locations("classpath:db/journal-migration").schemas(database.schema())
                .target(version).load().migrate();
    }

    private static void insertJournal(
            JdbcTemplate jdbc,
            long id,
            String status,
            String entryType,
            String originalId,
            String amount) {
        jdbc.update("INSERT INTO journal_entries"
                        + " (id, slip_no, slip_date, accounting_date, status, entry_type, currency_code,"
                        + " created_by, audit_user, lineage_source_type, lineage_source_id)"
                        + " VALUES (?, ?, '2026-09-25', '2026-09-25', ?, ?, 'KRW', 'maker', 'actor', ?, ?)",
                id, "GL759-" + id, status, entryType,
                originalId == null ? "TEST" : "JOURNAL_ENTRY", originalId == null ? "ISSUE-759" : originalId);
        jdbc.update("INSERT INTO journal_details"
                        + " (id, journal_entry_id, side, account_code, amount, base_amount)"
                        + " VALUES (?, ?, 'DEBIT', '10100', ?, ?)",
                id * 10, id, new BigDecimal(amount), new BigDecimal(amount));
    }

    private static BigDecimal totalJournalAmount(JdbcTemplate jdbc) {
        return jdbc.queryForObject("SELECT SUM(amount) FROM journal_details", BigDecimal.class);
    }
}
