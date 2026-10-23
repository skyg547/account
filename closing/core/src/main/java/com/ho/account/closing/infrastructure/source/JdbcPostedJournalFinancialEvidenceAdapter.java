package com.ho.account.closing.infrastructure.source;

import com.ho.account.closing.application.port.out.AllowanceBalance;
import com.ho.account.closing.application.port.out.AllowanceBalanceLookupPort;
import com.ho.account.closing.application.port.out.FxValuationEvidencePort;
import com.ho.account.closing.application.service.FxValuationBalance;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.Date;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;

import static com.ho.account.closing.infrastructure.source.PostedJournalContributionQuery.ALLOWANCE_BALANCE_SQL;
import static com.ho.account.closing.infrastructure.source.PostedJournalContributionQuery.BALANCES_SQL;
import static com.ho.account.closing.infrastructure.source.PostedJournalContributionQuery.requireValidLineage;

/**
 * Streams posted journal evidence for synchronous Closing runs and ECL allowance reconciliation.
 *
 * <p>The callback is invoked directly from JDBC row traversal; this adapter never materializes an
 * unbounded ledger result. Callers that need an immutable preflight plan must impose their own hard
 * command cap. A statement timeout also bounds database occupancy, while the shared SQL preserves the
 * same cutoff, dual-currency and FX reversal-lineage semantics used by Batch.</p>
 */
public final class JdbcPostedJournalFinancialEvidenceAdapter
        implements FxValuationEvidencePort, AllowanceBalanceLookupPort {

    public static final int DEFAULT_QUERY_TIMEOUT_SECONDS = 60;
    private static final int STREAM_FETCH_SIZE = 1_000;

    private final JdbcTemplate jdbc;
    private final int queryTimeoutSeconds;
    private final int maxFxEvidenceRows;

    public JdbcPostedJournalFinancialEvidenceAdapter(JdbcTemplate jdbc) {
        this(jdbc, DEFAULT_QUERY_TIMEOUT_SECONDS, Integer.MAX_VALUE);
    }

    public JdbcPostedJournalFinancialEvidenceAdapter(JdbcTemplate jdbc, int queryTimeoutSeconds) {
        this(jdbc, queryTimeoutSeconds, Integer.MAX_VALUE);
    }

    public JdbcPostedJournalFinancialEvidenceAdapter(
            JdbcTemplate jdbc,
            int queryTimeoutSeconds,
            int maxFxEvidenceRows) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
        if (queryTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("queryTimeoutSeconds must be positive");
        }
        if (maxFxEvidenceRows <= 0) {
            throw new IllegalArgumentException("maxFxEvidenceRows must be positive");
        }
        this.queryTimeoutSeconds = queryTimeoutSeconds;
        this.maxFxEvidenceRows = maxFxEvidenceRows;
    }

    @Override
    public void forEachBalance(
            LocalDate valuationDate,
            String reportingCurrencyCode,
            Consumer<FxValuationBalance> consumer) {
        Objects.requireNonNull(valuationDate, "valuationDate must not be null");
        Objects.requireNonNull(consumer, "consumer must not be null");
        String reportingCurrency = normalizeCurrency(reportingCurrencyCode, "reportingCurrencyCode");

        jdbc.query(
                BALANCES_SQL,
                statement -> {
                    statement.setDate(1, Date.valueOf(valuationDate));
                    statement.setString(2, reportingCurrency);
                    statement.setFetchSize(STREAM_FETCH_SIZE);
                    statement.setQueryTimeout(queryTimeoutSeconds);
                    if (maxFxEvidenceRows < Integer.MAX_VALUE) {
                        // One overflow row lets core reject the request without silently truncating.
                        statement.setMaxRows(maxFxEvidenceRows + 1);
                    }
                },
                (RowCallbackHandler) resultSet -> {
                    requireValidLineage(resultSet.getInt("invalid_lineage"));
                    consumer.accept(new FxValuationBalance(
                            resultSet.getString("account_code"),
                            resultSet.getString("currency_code"),
                            resultSet.getBigDecimal("foreign_ending_balance"),
                            resultSet.getBigDecimal("book_reporting_amount")));
                });
    }

    @Override
    public AllowanceBalance findCreditBalance(
            String allowanceAccountCode,
            String transactionCurrencyCode,
            String functionalCurrencyCode,
            LocalDate balanceDate) {
        if (allowanceAccountCode == null || allowanceAccountCode.isBlank()) {
            throw new IllegalArgumentException("allowanceAccountCode must not be blank");
        }
        Objects.requireNonNull(balanceDate, "balanceDate must not be null");
        String transactionCurrency = normalizeCurrency(transactionCurrencyCode, "transactionCurrencyCode");
        String functionalCurrency = normalizeCurrency(functionalCurrencyCode, "functionalCurrencyCode");

        return jdbc.query(
                ALLOWANCE_BALANCE_SQL,
                statement -> {
                    statement.setDate(1, Date.valueOf(balanceDate));
                    statement.setString(2, functionalCurrency);
                    statement.setString(3, allowanceAccountCode.trim());
                    statement.setString(4, transactionCurrency);
                    statement.setQueryTimeout(queryTimeoutSeconds);
                },
                resultSet -> {
                    if (!resultSet.next()) {
                        throw new IllegalStateException("Posted allowance balance query returned no aggregate row");
                    }
                    requireValidLineage(resultSet.getInt("invalid_lineage"));
                    AllowanceBalance balance = new AllowanceBalance(
                            transactionCurrency,
                            functionalCurrency,
                            resultSet.getBigDecimal("transaction_amount").negate(),
                            resultSet.getBigDecimal("base_amount").negate());
                    if (resultSet.next()) {
                        throw new IllegalStateException("Posted allowance balance query returned multiple aggregate rows");
                    }
                    return balance;
                });
    }

    private static String normalizeCurrency(String value, String name) {
        if (value == null || !value.trim().matches("[A-Za-z]{3}")) {
            throw new IllegalArgumentException(name + " must be a 3-letter currency code");
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }
}
