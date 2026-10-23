package com.ho.account.journalledger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

class JournalCreationOperationMigrationTest {

    private static final String FINGERPRINT_A = "a".repeat(64);
    private static final String FINGERPRINT_B = "b".repeat(64);

    @Test
    void cleanV18CreatesCompleteLockSetAndEnforcesOperationRelationships() {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrateTo(database, "18");
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());

        assertCompleteLockSet(jdbc);
        insertJournal(jdbc, 1L, "GL765-1", "100.00");
        insertJournal(jdbc, 2L, "GL765-2", "200.00");
        insertOperation(jdbc, "PAYABLE", "INV-765", "PRIMARY", FINGERPRINT_A, 1L);
        insertOperation(jdbc, "PAYABLE", "INV-765", "TAX", FINGERPRINT_B, 2L);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class))
                .isEqualTo(2);
        assertThat(jdbc.queryForList("SELECT operation_key FROM journal_creation_operations"
                + " WHERE source_type = 'PAYABLE' AND source_id = 'INV-765' ORDER BY operation_key", String.class))
                .containsExactly("PRIMARY", "TAX");
        assertThatThrownBy(() -> insertOperation(
                jdbc, "PAYABLE", "INV-765", "PRIMARY", FINGERPRINT_B, 2L))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertOperation(
                jdbc, "PAYABLE", "INV-OTHER", "PRIMARY", FINGERPRINT_B, 1L))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> insertOperation(
                jdbc, "PAYABLE", "INV-ORPHAN", "PRIMARY", FINGERPRINT_B, 999L))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO journal_creation_operations"
                        + " (source_type, source_id, operation_key, fingerprint_version,"
                        + " request_fingerprint, journal_entry_id, created_at)"
                        + " VALUES ('PAYABLE', 'INV-VERSION', 'PRIMARY', 0, ?, 2, CURRENT_TIMESTAMP)",
                FINGERPRINT_B)).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update("INSERT INTO journal_creation_operations"
                        + " (source_type, source_id, operation_key, fingerprint_version,"
                        + " request_fingerprint, journal_entry_id, created_at)"
                        + " VALUES ('PAYABLE', 'INV-DIGEST', 'PRIMARY', 1, ?, 2, CURRENT_TIMESTAMP)",
                "A".repeat(64))).isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbc.update(
                "INSERT INTO journal_creation_operation_locks(lock_id) VALUES (256)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history"
                + " WHERE version = '18' AND success = TRUE", Integer.class)).isOne();
    }

    @Test
    void v17ToV18PreservesLegacyJournalsWithoutInventingOperationIdentity() {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrateTo(database, "17");
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        insertJournal(jdbc, 10L, "GL765-LEGACY-1", "125.25");
        insertJournal(jdbc, 11L, "GL765-LEGACY-2", "80.40");
        BigDecimal amountBefore = totalJournalAmount(jdbc);

        migrateTo(database, "18");

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_entries", Integer.class)).isEqualTo(2);
        assertThat(totalJournalAmount(jdbc)).isEqualByComparingTo(amountBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operations", Integer.class)).isZero();
        assertCompleteLockSet(jdbc);
        insertOperation(jdbc, "ISSUE_765", "LEGACY-10", "ADOPTED", FINGERPRINT_A, 10L);
        assertThat(jdbc.queryForObject("SELECT journal_entry_id FROM journal_creation_operations"
                + " WHERE source_type = 'ISSUE_765' AND source_id = 'LEGACY-10'"
                + " AND operation_key = 'ADOPTED'", Long.class)).isEqualTo(10L);
        assertThat(totalJournalAmount(jdbc)).isEqualByComparingTo(amountBefore);
    }

    private static void assertCompleteLockSet(JdbcTemplate jdbc) {
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM journal_creation_operation_locks", Integer.class))
                .isEqualTo(256);
        assertThat(jdbc.queryForObject("SELECT COUNT(DISTINCT lock_id)"
                + " FROM journal_creation_operation_locks", Integer.class)).isEqualTo(256);
        assertThat(jdbc.queryForObject("SELECT MIN(lock_id) FROM journal_creation_operation_locks", Integer.class))
                .isZero();
        assertThat(jdbc.queryForObject("SELECT MAX(lock_id) FROM journal_creation_operation_locks", Integer.class))
                .isEqualTo(255);
    }

    private static void migrateTo(PostingTestDatabase database, String version) {
        Flyway.configure().dataSource(database.dataSource())
                .locations("classpath:db/journal-migration").schemas(database.schema())
                .target(version).load().migrate();
    }

    private static void insertJournal(JdbcTemplate jdbc, long id, String slipNo, String amount) {
        jdbc.update("INSERT INTO journal_entries"
                        + " (id, slip_no, slip_date, accounting_date, status, entry_type, currency_code,"
                        + " created_by, audit_user, lineage_source_type, lineage_source_id)"
                        + " VALUES (?, ?, '2026-09-25', '2026-09-25', 'DRAFT', 'NORMAL', 'KRW',"
                        + " 'maker', 'maker', 'ISSUE_765', ?)",
                id, slipNo, "SOURCE-" + id);
        jdbc.update("INSERT INTO journal_details"
                        + " (id, journal_entry_id, side, account_code, amount, base_amount)"
                        + " VALUES (?, ?, 'DEBIT', '10100', ?, ?)",
                id * 10, id, new BigDecimal(amount), new BigDecimal(amount));
    }

    private static void insertOperation(
            JdbcTemplate jdbc,
            String sourceType,
            String sourceId,
            String operationKey,
            String fingerprint,
            long journalId) {
        jdbc.update("INSERT INTO journal_creation_operations"
                        + " (source_type, source_id, operation_key, fingerprint_version,"
                        + " request_fingerprint, journal_entry_id, created_at)"
                        + " VALUES (?, ?, ?, 1, ?, ?, CURRENT_TIMESTAMP)",
                sourceType, sourceId, operationKey, fingerprint, journalId);
    }

    private static BigDecimal totalJournalAmount(JdbcTemplate jdbc) {
        return jdbc.queryForObject("SELECT SUM(amount) FROM journal_details", BigDecimal.class);
    }
}
