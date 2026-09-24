package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.service.FxValuationBalance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JournalFxValuationBalanceSourceTest {

    private JdbcTemplate jdbcTemplate;
    private JournalFxValuationBalanceSource source;

    @BeforeEach
    void setUp() {
        DataSource dataSource = new DriverManagerDataSource(
                "jdbc:h2:mem:closing-fx-" + System.nanoTime() + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "sa",
                "");
        jdbcTemplate = new JdbcTemplate(dataSource);
        source = new JournalFxValuationBalanceSource(dataSource, jdbcTemplate);
        jdbcTemplate.execute("""
                CREATE TABLE journal_entries (
                    id BIGINT PRIMARY KEY,
                    status VARCHAR(20) NOT NULL,
                    accounting_date DATE NOT NULL,
                    currency_code VARCHAR(3) NOT NULL,
                    entry_type VARCHAR(50) DEFAULT 'NORMAL' NOT NULL,
                    lineage_source_type VARCHAR(50),
                    lineage_source_id VARCHAR(100)
                )
                """);
        jdbcTemplate.execute("""
                CREATE TABLE journal_details (
                    id BIGINT PRIMARY KEY,
                    journal_entry_id BIGINT NOT NULL,
                    account_code VARCHAR(50) NOT NULL,
                    side VARCHAR(10) NOT NULL,
                    amount DECIMAL(19,2) NOT NULL,
                    base_amount DECIMAL(19,2) NOT NULL
                )
                """);
    }

    @Test
    void readsPostedSignedForeignAndReportingBalancesWithBoundedPartitions() throws Exception {
        jdbcTemplate.update(
                "INSERT INTO journal_entries (id, status, accounting_date, currency_code) "
                        + "VALUES (1, 'POSTED', DATE '2026-05-20', 'EUR')");
        jdbcTemplate.update(
                "INSERT INTO journal_entries (id, status, accounting_date, currency_code) "
                        + "VALUES (2, 'DRAFT', DATE '2026-05-21', 'EUR')");
        jdbcTemplate.update(
                "INSERT INTO journal_entries (id, status, accounting_date, currency_code) "
                        + "VALUES (3, 'POSTED', DATE '2026-05-20', 'KRW')");
        jdbcTemplate.update(
                "INSERT INTO journal_details (id, journal_entry_id, account_code, side, amount, base_amount) "
                        + "VALUES (1, 1, '11000', 'DEBIT', 100.00, 120.00)");
        jdbcTemplate.update(
                "INSERT INTO journal_details (id, journal_entry_id, account_code, side, amount, base_amount) "
                        + "VALUES (2, 1, '11000', 'CREDIT', 20.00, 24.00)");
        jdbcTemplate.update(
                "INSERT INTO journal_details (id, journal_entry_id, account_code, side, amount, base_amount) "
                        + "VALUES (3, 2, '11000', 'DEBIT', 999.00, 999.00)");
        jdbcTemplate.update(
                "INSERT INTO journal_details (id, journal_entry_id, account_code, side, amount, base_amount) "
                        + "VALUES (4, 3, '11000', 'DEBIT', 999.00, 999.00)");

        Integer eligibleAccountCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(DISTINCT d.account_code)
                  FROM journal_details d
                  JOIN journal_entries e ON e.id = d.journal_entry_id
                 WHERE e.status = 'POSTED'
                   AND e.accounting_date <= ?
                   AND UPPER(e.currency_code) <> ?
                """, Integer.class, Date.valueOf("2026-05-31"), "KRW");
        assertThat(eligibleAccountCount).isEqualTo(1);

        var partitions = source.createPartitions(LocalDate.of(2026, 5, 31), "KRW", 4);
        assertThat(partitions).hasSize(1);

        ExecutionContext range = partitions.values().iterator().next();
        JdbcCursorItemReader<FxValuationBalance> reader = source.createReader(
                LocalDate.of(2026, 5, 31),
                "KRW",
                range.getString("startAccountCode"),
                range.getString("endAccountCode"),
                100);
        List<FxValuationBalance> balances = new ArrayList<>();
        reader.open(new ExecutionContext());
        try {
            FxValuationBalance balance;
            while ((balance = reader.read()) != null) {
                balances.add(balance);
            }
        } finally {
            reader.close();
        }

        assertThat(balances).singleElement().satisfies(balance -> {
            assertThat(balance.accountCode()).isEqualTo("11000");
            assertThat(balance.currencyCode()).isEqualTo("EUR");
            assertThat(balance.foreignEndingBalance()).isEqualByComparingTo("80.00");
            assertThat(balance.bookReportingAmount()).isEqualByComparingTo("96.00");
        });
    }

    @Test
    void partitionMetadataNeverExceedsConfiguredGridSize() {
        jdbcTemplate.update(
                "INSERT INTO journal_entries (id, status, accounting_date, currency_code) "
                        + "VALUES (1, 'POSTED', DATE '2026-05-20', 'EUR')");
        for (int index = 0; index < 10; index++) {
            jdbcTemplate.update(
                    "INSERT INTO journal_details (id, journal_entry_id, account_code, side, amount, base_amount) "
                            + "VALUES (?, 1, ?, 'DEBIT', 1.00, 1.20)",
                    index + 1,
                    String.valueOf(10000 + index));
        }

        var partitions = source.createPartitions(LocalDate.of(2026, 5, 31), "KRW", 4);

        assertThat(partitions).hasSize(4);
        assertThat(partitions.get("fx-range-1").getString("startAccountCode")).isEqualTo("10000");
        assertThat(partitions.get("fx-range-1").getString("endAccountCode")).isEqualTo("10002");
        assertThat(partitions.get("fx-range-4").getString("startAccountCode")).isEqualTo("10009");
        assertThat(partitions.get("fx-range-4").getString("endAccountCode")).isEqualTo("10009");
    }
}
