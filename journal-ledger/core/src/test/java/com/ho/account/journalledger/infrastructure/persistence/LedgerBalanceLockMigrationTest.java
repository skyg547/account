package com.ho.account.journalledger.infrastructure.persistence;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class LedgerBalanceLockMigrationTest {
    @Test
    void upgradeSeedsEveryLockWithoutChangingExistingGlOrNullableSlBalances() {
        PostingTestDatabase database = PostingTestDatabase.create();
        migrateTo(database, "13");
        JdbcTemplate jdbc = new JdbcTemplate(database.dataSource());
        for (String table : List.of("gl_balances", "sl_balances")) {
            jdbc.update("INSERT INTO " + table
                    + " (account_code, currency_code, balance_date, period, beginning_balance,"
                    + " debit_amount, credit_amount, ending_balance)"
                    + " VALUES ('10100', 'KRW', '2026-09-24', '2026-09', 100, 10, 20, 90)");
        }
        List<Map<String, Object>> glBefore = jdbc.queryForList("SELECT * FROM gl_balances");
        List<Map<String, Object>> slBefore = jdbc.queryForList("SELECT * FROM sl_balances");

        migrateTo(database, "14");
        // Re-running Flyway must preserve the seeded identities and the financial history.
        migrateTo(database, "14");

        assertThat(jdbc.queryForList("SELECT lock_id FROM ledger_balance_locks ORDER BY lock_id", Integer.class))
                .containsExactlyElementsOf(IntStream.range(0, 256).boxed().toList());
        assertThat(jdbc.queryForList("SELECT * FROM gl_balances")).isEqualTo(glBefore);
        assertThat(jdbc.queryForList("SELECT * FROM sl_balances")).isEqualTo(slBefore);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM flyway_schema_history"
                + " WHERE version = '14' AND success = TRUE", Integer.class)).isOne();
    }

    private void migrateTo(PostingTestDatabase database, String version) {
        Flyway.configure().dataSource(database.dataSource())
                .locations("classpath:db/journal-migration").schemas(database.schema())
                .target(version).load().migrate();
    }
}
