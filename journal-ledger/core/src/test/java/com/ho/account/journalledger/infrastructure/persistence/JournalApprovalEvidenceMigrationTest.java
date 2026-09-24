package com.ho.account.journalledger.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class JournalApprovalEvidenceMigrationTest {

    @Test
    void backfillsOnlyDistinctRecoverableLegacyApprovalEvidence() {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrateTo(database, "15");
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        insert(jdbc, 1L, "APPROVED", "maker-one", " Checker.One ");
        insert(jdbc, 2L, "POSTED", "maker-one", "poster-one");
        insert(jdbc, 3L, "REQUESTED", "maker-one", "maker-one");
        insert(jdbc, 4L, "APPROVED", " Maker.Self ", "maker.self");
        insert(jdbc, 5L, "APPROVED", null, "checker-without-maker");
        insert(jdbc, 6L, "APPROVED", "   ", "checker-with-blank-maker");

        migrateTo(database, "16");

        Map<Long, String> evidence = jdbc.query(
                "SELECT id, approved_by FROM journal_entries ORDER BY id",
                resultSet -> {
                    java.util.LinkedHashMap<Long, String> values = new java.util.LinkedHashMap<>();
                    while (resultSet.next()) {
                        values.put(resultSet.getLong("id"), resultSet.getString("approved_by"));
                    }
                    return values;
                });
        assertThat(evidence).containsEntry(1L, "checker.one");
        assertThat(evidence.get(2L)).isNull();
        assertThat(evidence.get(3L)).isNull();
        assertThat(evidence.get(4L)).isNull();
        assertThat(evidence.get(5L)).isNull();
        assertThat(evidence.get(6L)).isNull();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history"
                + " WHERE version = '16' AND success = TRUE", Integer.class)).isOne();
    }

    private void migrateTo(PostingTestDatabase database, String version) {
        Flyway.configure().dataSource(database.dataSource())
                .locations("classpath:db/journal-migration").schemas(database.schema())
                .target(version).load().migrate();
    }

    private void insert(JdbcTemplate jdbc, long id, String status, String createdBy, String auditUser) {
        jdbc.update("INSERT INTO journal_entries"
                        + " (id, slip_no, slip_date, accounting_date, status, created_by, audit_user)"
                        + " VALUES (?, ?, '2026-09-25', '2026-09-25', ?, ?, ?)",
                id, "GL756-" + id, status, createdBy, auditUser);
    }
}
