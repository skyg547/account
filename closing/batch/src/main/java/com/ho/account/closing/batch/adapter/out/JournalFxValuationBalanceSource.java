package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.service.FxValuationBalance;
import com.ho.account.closing.application.service.FxValuationEligibilityResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JdbcCursorItemReader;
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
 * metadata bounded by grid size instead of creating one partition per account. Prior reporting-currency
 * FX adjustments contribute only signed base amounts to their lineage's foreign account/currency.</p>
 *
 * <p>Reversal ancestry is resolved without a date/status filter; each contributing entry independently
 * satisfies POSTED and the valuation cutoff. The original stays POSTED, and each reversal already has
 * flipped detail sides. Recursive descendants cannot cycle back to an FX root because their lineage
 * type is JOURNAL_ENTRY, while roots have type FX_VALUATION. Unattributable orphan reversals require
 * source reconciliation: this reader does not guess an FX account or currency.</p>
 *
 * <p>@todo Replace the journal aggregation with a maintained dual-currency balance read model
 * before 100M-row production rollout. Completion requires amount/baseAmount bulk updates during
 * posting, account/currency/date indexes, reconciliation against posted journals, and a PostgreSQL
 * execution-plan/load test proving bounded latency.</p>
 */
@Component
@RequiredArgsConstructor
public class JournalFxValuationBalanceSource {

    // One contribution relation keeps adjustment-only residual accounts in partition discovery.
    // An FX root owns the account/currency attribution; reporting-currency P&L legs never enter it.
    private static final String CONTRIBUTIONS_SQL = """
            WITH RECURSIVE fx_lineage_tokens AS (
                SELECT e.id, e.entry_type, e.currency_code, e.lineage_source_id,
                       CASE WHEN POSITION('|' IN e.lineage_source_id) > 0
                            THEN SUBSTRING(e.lineage_source_id FROM 1
                                           FOR POSITION('|' IN e.lineage_source_id) - 1)
                            ELSE '' END AS batch_id,
                       CASE WHEN POSITION('|' IN e.lineage_source_id) > 0
                            THEN SUBSTRING(e.lineage_source_id
                                           FROM POSITION('|' IN e.lineage_source_id) + 1)
                            ELSE '' END AS account_and_currency
                  FROM journal_entries e
                 WHERE e.lineage_source_type = 'FX_VALUATION'
            ), fx_roots AS (
                SELECT t.*,
                       CASE WHEN POSITION('|' IN t.account_and_currency) > 0
                            THEN SUBSTRING(t.account_and_currency FROM 1
                                           FOR POSITION('|' IN t.account_and_currency) - 1)
                            ELSE '' END AS account_code,
                       UPPER(CASE WHEN POSITION('|' IN t.account_and_currency) > 0
                                  THEN SUBSTRING(t.account_and_currency
                                                 FROM POSITION('|' IN t.account_and_currency) + 1)
                                  ELSE '' END) AS source_currency
                  FROM fx_lineage_tokens t
            ), fx_entries (entry_id, account_code, source_currency, reporting_currency, invalid_lineage) AS (
                SELECT r.id, CAST(r.account_code AS VARCHAR(50)),
                       CAST(r.source_currency AS VARCHAR(3)), UPPER(r.currency_code),
                       CASE WHEN r.entry_type = 'CLOSING_ADJUSTMENT'
                                  AND REGEXP_REPLACE(r.lineage_source_id,
                                      '^[1-9][0-9]*[|][^|]+[|][A-Za-z]{3}$', '') = ''
                                  AND LENGTH(r.batch_id) <= 19
                                  AND (LENGTH(r.batch_id) < 19 OR r.batch_id <= '9223372036854775807')
                                  AND LENGTH(r.account_code) BETWEEN 1 AND 50
                                  AND r.account_code = TRIM(r.account_code)
                                  AND r.source_currency <> UPPER(r.currency_code)
                            THEN 0 ELSE 1 END
                  FROM fx_roots r
                UNION ALL
                SELECT reversal.id, parent.account_code, parent.source_currency, parent.reporting_currency,
                       CASE WHEN UPPER(reversal.currency_code) = parent.reporting_currency
                            THEN parent.invalid_lineage ELSE 1 END
                  FROM journal_entries reversal
                  JOIN fx_entries parent
                    ON reversal.lineage_source_id = CAST(parent.entry_id AS VARCHAR)
                 WHERE reversal.entry_type = 'REVERSAL'
                   AND reversal.lineage_source_type = 'JOURNAL_ENTRY'
            ), eligible_contributions AS (
                SELECT CASE WHEN fx.entry_id IS NULL THEN d.account_code ELSE fx.account_code END AS account_code,
                       CASE WHEN fx.entry_id IS NULL THEN UPPER(e.currency_code)
                            ELSE fx.source_currency END AS currency_code,
                       CASE WHEN fx.entry_id IS NOT NULL THEN CAST(0 AS NUMERIC(19, 2))
                            WHEN d.side = 'DEBIT' THEN d.amount ELSE -d.amount END AS foreign_amount,
                       CASE WHEN d.side = 'DEBIT' THEN d.base_amount ELSE -d.base_amount END AS base_amount,
                       CASE WHEN fx.entry_id IS NOT NULL
                                  AND (fx.invalid_lineage <> 0 OR d.id IS NULL)
                            THEN 1 ELSE 0 END AS invalid_lineage,
                       e.accounting_date, fx.entry_id AS fx_entry_id, fx.reporting_currency AS fx_reporting_currency
                  FROM journal_entries e
                  LEFT JOIN fx_entries fx ON fx.entry_id = e.id
                  LEFT JOIN journal_details d
                    ON d.journal_entry_id = e.id
                   AND (fx.entry_id IS NULL OR d.account_code = fx.account_code)
                 WHERE e.status = 'POSTED'
                   AND (fx.entry_id IS NOT NULL OR d.id IS NOT NULL)
            )
            """;

    // H2's recursive CTE binding loses parameters inside WITH; bind only in the final SELECT.
    private static final String ELIGIBLE_FROM_SQL = """
              FROM eligible_contributions c
              CROSS JOIN (SELECT CAST(? AS DATE) AS valuation_date,
                                 CAST(? AS VARCHAR(3)) AS reporting_currency) p
             WHERE c.accounting_date <= p.valuation_date
               AND (c.fx_entry_id IS NOT NULL OR c.currency_code <> p.reporting_currency)
            """;

    private static final String INVALID_LINEAGE_SQL = """
            CASE WHEN c.fx_entry_id IS NOT NULL AND c.fx_reporting_currency <> p.reporting_currency
                 THEN 1 ELSE c.invalid_lineage END
            """;

    private static final String ACCOUNT_COUNT_SQL = CONTRIBUTIONS_SQL + """
            SELECT COUNT(DISTINCT account_code) AS account_count,
                   COALESCE(MAX(%s), 0) AS invalid_lineage
            """.formatted(INVALID_LINEAGE_SQL) + ELIGIBLE_FROM_SQL;

    private static final String ORDERED_ACCOUNTS_SQL = CONTRIBUTIONS_SQL + """
            SELECT DISTINCT account_code
            """ + ELIGIBLE_FROM_SQL + " ORDER BY account_code";

    private static final String BALANCE_SQL = CONTRIBUTIONS_SQL + """
            SELECT account_code, currency_code,
                   SUM(foreign_amount) AS foreign_ending_balance,
                   SUM(base_amount) AS book_reporting_amount,
                   MAX(%s) AS invalid_lineage
            """.formatted(INVALID_LINEAGE_SQL) + ELIGIBLE_FROM_SQL + """
               AND (c.invalid_lineage <> 0 OR c.fx_reporting_currency <> p.reporting_currency
                    OR (c.account_code >= ? AND c.account_code <= ?))
             GROUP BY account_code, currency_code
            HAVING SUM(foreign_amount) <> 0 OR SUM(base_amount) <> 0 OR MAX(%s) <> 0
             ORDER BY account_code, currency_code
            """.formatted(INVALID_LINEAGE_SQL);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final FxValuationEligibilityResolver eligibilityResolver;

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
                (resultSet, rowNumber) -> {
                    requireValidLineage(resultSet.getInt("invalid_lineage"));
                    return resultSet.getLong("account_count");
                },
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
                    if (!eligibilityResolver.isEligible(accountCode, valuationDate)) {
                        return;
                    }
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
        if (valuationDate == null) {
            throw new IllegalArgumentException("valuationDate must not be null");
        }
        JdbcCursorItemReader<FxValuationBalance> reader = new JdbcCursorItemReader<>() {
            private String lastAccountCode;
            private boolean lastAccountEligible;

            @Override
            protected void doOpen() throws Exception {
                lastAccountCode = null;
                super.doOpen();
            }

            @Override
            public FxValuationBalance read() throws Exception {
                FxValuationBalance balance;
                // Count every physical row with super.read(): restart offsets must include exclusions.
                // Returning null from a row mapper would instead terminate at the first historical item.
                while ((balance = super.read()) != null) {
                    if (!balance.accountCode().equals(lastAccountCode)) {
                        lastAccountEligible = eligibilityResolver.isEligible(balance.accountCode(), valuationDate);
                        lastAccountCode = balance.accountCode();
                    }
                    if (lastAccountEligible) {
                        return balance;
                    }
                }
                return null;
            }
        };
        reader.setName("fxValuationBalanceReader-" + startAccountCode + "-" + endAccountCode);
        reader.setDataSource(dataSource);
        reader.setSql(BALANCE_SQL);
        reader.setPreparedStatementSetter(statement -> {
            statement.setDate(1, Date.valueOf(valuationDate));
            statement.setString(2, reportingCurrency);
            statement.setString(3, startAccountCode);
            statement.setString(4, endAccountCode);
        });
        reader.setRowMapper((resultSet, rowNumber) -> {
            // Invalid posted history remains an error even if eligibility would exclude its account.
            requireValidLineage(resultSet.getInt("invalid_lineage"));
            return new FxValuationBalance(
                    resultSet.getString("account_code"),
                    resultSet.getString("currency_code"),
                    resultSet.getBigDecimal("foreign_ending_balance"),
                    resultSet.getBigDecimal("book_reporting_amount"));
        });
        reader.setFetchSize(fetchSize);
        reader.setSaveState(true);
        return reader;
    }

    private static void requireValidLineage(int invalidLineage) {
        if (invalidLineage != 0) {
            throw new IllegalStateException("FX valuation lineage is invalid or its account leg is missing");
        }
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
