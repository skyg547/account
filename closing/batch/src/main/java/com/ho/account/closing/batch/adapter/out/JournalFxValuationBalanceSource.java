package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.port.out.AllowanceBalance;
import com.ho.account.closing.application.port.out.AllowanceBalanceLookupPort;
import com.ho.account.closing.application.port.out.FxValuationEvidencePort;
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
import java.util.Objects;
import java.util.function.Consumer;

import static com.ho.account.closing.infrastructure.source.PostedJournalContributionQuery.ACCOUNT_COUNT_SQL;
import static com.ho.account.closing.infrastructure.source.PostedJournalContributionQuery.ALLOWANCE_BALANCE_SQL;
import static com.ho.account.closing.infrastructure.source.PostedJournalContributionQuery.BALANCES_SQL;
import static com.ho.account.closing.infrastructure.source.PostedJournalContributionQuery.ORDERED_ACCOUNTS_SQL;
import static com.ho.account.closing.infrastructure.source.PostedJournalContributionQuery.PARTITION_BALANCES_SQL;
import static com.ho.account.closing.infrastructure.source.PostedJournalContributionQuery.requireValidLineage;

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
public class JournalFxValuationBalanceSource
        implements FxValuationEvidencePort, AllowanceBalanceLookupPort {

    private static final int STREAM_FETCH_SIZE = 1_000;

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final FxValuationEligibilityResolver eligibilityResolver;

    @Override
    public AllowanceBalance findCreditBalance(
            String allowanceAccountCode,
            String transactionCurrencyCode,
            String functionalCurrencyCode,
            LocalDate balanceDate) {
        if (allowanceAccountCode == null || allowanceAccountCode.isBlank()) {
            throw new IllegalArgumentException("allowanceAccountCode must not be blank");
        }
        if (balanceDate == null) {
            throw new IllegalArgumentException("balanceDate must not be null");
        }
        String transactionCurrency = normalizeCurrency(transactionCurrencyCode);
        String functionalCurrency = normalizeCurrency(functionalCurrencyCode);
        return jdbcTemplate.queryForObject(
                ALLOWANCE_BALANCE_SQL,
                (resultSet, rowNumber) -> {
                    requireValidLineage(resultSet.getInt("invalid_lineage"));
                    // The common relation is debit-positive; ECL allowance balances are credit-positive.
                    return new AllowanceBalance(transactionCurrency, functionalCurrency,
                            resultSet.getBigDecimal("transaction_amount").negate(),
                            resultSet.getBigDecimal("base_amount").negate());
                },
                Date.valueOf(balanceDate), functionalCurrency, allowanceAccountCode.trim(), transactionCurrency);
    }

    @Override
    public void forEachBalance(
            LocalDate valuationDate,
            String reportingCurrencyCode,
            Consumer<FxValuationBalance> consumer) {
        Objects.requireNonNull(valuationDate, "valuationDate must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        String reportingCurrency = normalizeCurrency(reportingCurrencyCode);
        jdbcTemplate.query(
                BALANCES_SQL,
                statement -> {
                    statement.setDate(1, Date.valueOf(valuationDate));
                    statement.setString(2, reportingCurrency);
                    statement.setFetchSize(STREAM_FETCH_SIZE);
                },
                (RowCallbackHandler) resultSet -> {
                    requireValidLineage(resultSet.getInt("invalid_lineage"));
                    consumer.accept(toBalance(resultSet));
                });
    }

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
        reader.setSql(PARTITION_BALANCES_SQL);
        reader.setPreparedStatementSetter(statement -> {
            statement.setDate(1, Date.valueOf(valuationDate));
            statement.setString(2, reportingCurrency);
            statement.setString(3, startAccountCode);
            statement.setString(4, endAccountCode);
        });
        reader.setRowMapper((resultSet, rowNumber) -> {
            // Invalid posted history remains an error even if eligibility would exclude its account.
            requireValidLineage(resultSet.getInt("invalid_lineage"));
            return toBalance(resultSet);
        });
        reader.setFetchSize(fetchSize);
        reader.setSaveState(true);
        return reader;
    }

    private static FxValuationBalance toBalance(java.sql.ResultSet resultSet) throws java.sql.SQLException {
        return new FxValuationBalance(
                resultSet.getString("account_code"),
                resultSet.getString("currency_code"),
                resultSet.getBigDecimal("foreign_ending_balance"),
                resultSet.getBigDecimal("book_reporting_amount"));
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
