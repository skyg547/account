package com.ho.account.journalledger.infrastructure.persistence;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.FlywayException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PostingIdentityMigrationTest {
    @Test
    void upgradesExistingValidLedgerAndEnforcesBothIdentities() {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrateTo(database, "12");
        JdbcTemplate jdbc = seedJournal(database);
        insertEntry(jdbc, "gl_entries", 760L);
        insertEntry(jdbc, "sl_entries", 760L);

        migrateTo(database, "13");

        for (String table : new String[]{"gl_entries", "sl_entries"}) {
            assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(1);
            assertThatThrownBy(() -> insertEntry(jdbc, table, 760L))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> insertEntry(jdbc, table, null))
                    .isInstanceOf(DataIntegrityViolationException.class);
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '13' AND success = TRUE",
                Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @CsvSource({"gl_entries,false", "sl_entries,false", "gl_entries,true", "sl_entries,true"})
    void refusesInvalidLegacyRowsWithoutDeletingFinancialHistory(String table, boolean nullIdentity) {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrateTo(database, "12");
        JdbcTemplate jdbc = seedJournal(database);
        insertEntry(jdbc, table, nullIdentity ? null : 760L);
        insertEntry(jdbc, table, nullIdentity ? null : 760L);

        assertThatThrownBy(() -> migrateTo(database, "13")).isInstanceOf(FlywayException.class);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Integer.class)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT SUM(base_dr_amount) FROM " + table, java.math.BigDecimal.class))
                .isEqualByComparingTo("200.00");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE version = '13' AND success = TRUE",
                Integer.class)).isZero();
    }

    private void migrateTo(PostingTestDatabase database, String version) {
        Flyway.configure().dataSource(database.dataSource())
                .locations("classpath:db/journal-migration").schemas(database.schema())
                .target(version).load().migrate();
    }

    private JdbcTemplate seedJournal(PostingTestDatabase database) {
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        jdbc.update("""
                INSERT INTO journal_entries (id, slip_no, slip_date, accounting_date, status)
                VALUES (760, 'GL760-MIGRATION', '2026-09-24', '2026-09-24', 'POSTED')
                """);
        jdbc.update("""
                INSERT INTO journal_details (id, journal_entry_id, side, account_code, amount, base_amount)
                VALUES (760, 760, 'DEBIT', '10100', 100, 100)
                """);
        return jdbc;
    }

    private void insertEntry(JdbcTemplate jdbc, String table, Long detailId) {
        jdbc.update("INSERT INTO " + table
                + " (journal_detail_id, account_code, dr_amount, cr_amount, base_dr_amount, base_cr_amount)"
                + " VALUES (?, '10100', 100, 0, 100, 0)", detailId);
    }
}
