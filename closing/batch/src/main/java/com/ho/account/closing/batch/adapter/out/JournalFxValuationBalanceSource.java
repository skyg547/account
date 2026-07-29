package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.service.FxValuationBalance;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.database.builder.JdbcCursorItemReaderBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads signed transaction/base balances from the posted journal source of truth.
 *
 * <p>The former {@code gl_account_balances} model had no production writer. This adapter therefore
 * aggregates the journal rows that posting actually persists. Fixed account ranges keep partition
 * metadata bounded by grid size instead of creating one partition per account.</p>
 *
 * <p>@todo Replace the journal aggregation with a maintained dual-currency balance read model
 * before 100M-row production rollout. Completion requires amount/baseAmount bulk updates during
 * posting, account/currency/date indexes, reconciliation against posted journals, and a PostgreSQL
 * execution-plan/load test proving bounded latency.</p>
 */
@Component
@RequiredArgsConstructor
public class JournalFxValuationBalanceSource {

    private static final String ACCOUNT_COUNT_SQL = """
            SELECT COUNT(*)
              FROM (
                    SELECT DISTINCT d.account_code
                      FROM journal_details d
                      JOIN journal_entries e ON e.id = d.journal_entry_id
                     WHERE e.status = 'POSTED'
                       AND e.accounting_date <= ?
                       AND UPPER(e.currency_code) <> ?
                   ) eligible_accounts
            """;

    private static final String ORDERED_ACCOUNTS_SQL = """
            SELECT DISTINCT d.account_code
              FROM journal_details d
              JOIN journal_entries e ON e.id = d.journal_entry_id
             WHERE e.status = 'POSTED'
               AND e.accounting_date <= ?
               AND UPPER(e.currency_code) <> ?
             ORDER BY d.account_code
            """;

    private static final String BALANCE_SQL = """
            SELECT d.account_code,
                   UPPER(e.currency_code) AS currency_code,
                   SUM(CASE WHEN d.side = 'DEBIT' THEN d.amount ELSE -d.amount END)
                       AS foreign_ending_balance,
                   SUM(CASE WHEN d.side = 'DEBIT' THEN d.base_amount ELSE -d.base_amount END)
                       AS book_reporting_amount
              FROM journal_details d
              JOIN journal_entries e ON e.id = d.journal_entry_id
             WHERE e.status = 'POSTED'
               AND e.accounting_date <= ?
               AND UPPER(e.currency_code) <> ?
               AND d.account_code >= ?
               AND d.account_code <= ?
             GROUP BY d.account_code, UPPER(e.currency_code)
            HAVING SUM(CASE WHEN d.side = 'DEBIT' THEN d.amount ELSE -d.amount END) <> 0
                OR SUM(CASE WHEN d.side = 'DEBIT' THEN d.base_amount ELSE -d.base_amount END) <> 0
             ORDER BY d.account_code, UPPER(e.currency_code)
            """;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public Map<String, ExecutionContext> createPartitions(
            LocalDate valuationDate,
            String reportingCurrencyCode,
            int gridSize) {
        if (gridSize <= 0) {
            throw new IllegalArgumentException("gridSize must be positive");
        }
        if (valuationDate == null) {
            throw new IllegalArgumentException("valuationDate must not be null");
        }
        String reportingCurrency = normalizeCurrency(reportingCurrencyCode);
        Date sqlDate = Date.valueOf(valuationDate);
        Long accountCount = jdbcTemplate.queryForObject(
                ACCOUNT_COUNT_SQL,
                Long.class,
                sqlDate,
                reportingCurrency);
        if (accountCount == null || accountCount == 0) {
            return Map.of();
        }

        long accountsPerPartition = Math.max(1L, (accountCount + gridSize - 1L) / gridSize);
        List<AccountRange> ranges = new java.util.ArrayList<>(gridSize);
        long[] rowIndex = {0L};
        jdbcTemplate.query(
                ORDERED_ACCOUNTS_SQL,
                statement -> {
                    statement.setDate(1, sqlDate);
                    statement.setString(2, reportingCurrency);
                },
                (RowCallbackHandler) resultSet -> {
                    String accountCode = resultSet.getString("account_code");
                    if (rowIndex[0] % accountsPerPartition == 0) {
                        ranges.add(new AccountRange(ranges.size() + 1, accountCode, accountCode));
                    } else {
                        int lastIndex = ranges.size() - 1;
                        AccountRange current = ranges.get(lastIndex);
                        ranges.set(lastIndex, new AccountRange(
                                current.bucket(), current.startAccountCode(), accountCode));
                    }
                    rowIndex[0]++;
                });

        Map<String, ExecutionContext> partitions = new LinkedHashMap<>();
        for (AccountRange range : ranges) {
            ExecutionContext context = new ExecutionContext();
            context.putString("startAccountCode", range.startAccountCode());
            context.putString("endAccountCode", range.endAccountCode());
            partitions.put("fx-range-" + range.bucket(), context);
        }
        return partitions;
    }

    public JdbcCursorItemReader<FxValuationBalance> createReader(
            LocalDate valuationDate,
            String reportingCurrencyCode,
            String startAccountCode,
            String endAccountCode,
            int fetchSize) {
        if (fetchSize <= 0) {
            throw new IllegalArgumentException("fetchSize must be positive");
        }
        String reportingCurrency = normalizeCurrency(reportingCurrencyCode);
        return new JdbcCursorItemReaderBuilder<FxValuationBalance>()
                .name("fxValuationBalanceReader-" + startAccountCode + "-" + endAccountCode)
                .dataSource(dataSource)
                .sql(BALANCE_SQL)
                .preparedStatementSetter(statement -> {
                    statement.setDate(1, Date.valueOf(valuationDate));
                    statement.setString(2, reportingCurrency);
                    statement.setString(3, startAccountCode);
                    statement.setString(4, endAccountCode);
                })
                .rowMapper((resultSet, rowNumber) -> new FxValuationBalance(
                        resultSet.getString("account_code"),
                        resultSet.getString("currency_code"),
                        resultSet.getBigDecimal("foreign_ending_balance"),
                        resultSet.getBigDecimal("book_reporting_amount")))
                .fetchSize(fetchSize)
                .saveState(true)
                .build();
    }

    private String normalizeCurrency(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("reportingCurrencyCode must not be blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!normalized.matches("[A-Z]{3}")) {
            throw new IllegalArgumentException("reportingCurrencyCode must be a 3-letter currency code");
        }
        return normalized;
    }

    private record AccountRange(int bucket, String startAccountCode, String endAccountCode) {
    }
}
