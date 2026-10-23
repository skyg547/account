package com.ho.account.closing.infrastructure.source;

import com.ho.account.closing.application.port.out.EclAllowanceResultPort;
import com.ho.account.closing.domain.EclAllowanceSummary;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/** Checks each finalized ECL row, then aggregates to ledger posting groups before the core port. */
public final class JdbcEclAllowanceResultAdapter implements EclAllowanceResultPort {

    public static final int DEFAULT_QUERY_TIMEOUT_SECONDS = 60;

    private static final String SELECT_SUMMARIES = """
            SELECT base_date,
                   run_id,
                   model_version,
                   legal_entity_code,
                   currency_code,
                   MIN(exposure_account_code) AS exposure_account_code,
                   allowance_account_code,
                   bad_debt_expense_account_code,
                   reversal_income_account_code,
                   SUM(target_allowance_amount) AS target_allowance_amount,
                   SUM(source_exposure_amount) AS source_exposure_amount,
                   SUM(stage1_allowance_amount) AS stage1_allowance_amount,
                   SUM(stage2_allowance_amount) AS stage2_allowance_amount,
                   SUM(stage3_allowance_amount) AS stage3_allowance_amount,
                   SUM(CASE WHEN target_allowance_amount IS NULL
                              OR source_exposure_amount IS NULL
                              OR stage1_allowance_amount IS NULL
                              OR stage2_allowance_amount IS NULL
                              OR stage3_allowance_amount IS NULL
                              OR target_allowance_amount < 0
                              OR source_exposure_amount < 0
                              OR stage1_allowance_amount < 0
                              OR stage2_allowance_amount < 0
                              OR stage3_allowance_amount < 0
                              OR stage1_allowance_amount + stage2_allowance_amount
                                 + stage3_allowance_amount <> target_allowance_amount
                              OR (source_exposure_amount = 0 AND target_allowance_amount <> 0)
                            THEN 1 ELSE 0 END) AS invalid_source_rows
              FROM allowance_summary
             WHERE base_date = ?
             GROUP BY base_date,
                      run_id,
                      model_version,
                      legal_entity_code,
                      currency_code,
                      allowance_account_code,
                      bad_debt_expense_account_code,
                      reversal_income_account_code
             ORDER BY legal_entity_code, currency_code, allowance_account_code
            """;

    private final JdbcTemplate jdbc;
    private final int maxSummaryGroups;
    private final int queryTimeoutSeconds;

    public JdbcEclAllowanceResultAdapter(JdbcTemplate jdbc) {
        this(jdbc, Integer.MAX_VALUE, DEFAULT_QUERY_TIMEOUT_SECONDS);
    }

    /**
     * Creates a bounded API source. Batch uses the unbounded-row constructor and owns scale through
     * its job controls; the synchronous API requests one overflow row so it can fail closed instead
     * of silently processing a truncated ECL snapshot.
     */
    public JdbcEclAllowanceResultAdapter(
            JdbcTemplate jdbc,
            int maxSummaryGroups,
            int queryTimeoutSeconds) {
        this.jdbc = Objects.requireNonNull(jdbc, "jdbc must not be null");
        if (maxSummaryGroups <= 0) {
            throw new IllegalArgumentException("maxSummaryGroups must be positive");
        }
        if (queryTimeoutSeconds <= 0) {
            throw new IllegalArgumentException("queryTimeoutSeconds must be positive");
        }
        this.maxSummaryGroups = maxSummaryGroups;
        this.queryTimeoutSeconds = queryTimeoutSeconds;
    }

    @Override
    public List<EclAllowanceSummary> loadSummaries(LocalDate baseDate) {
        Objects.requireNonNull(baseDate, "baseDate must not be null");
        int jdbcMaxRows = maxSummaryGroups == Integer.MAX_VALUE
                ? 0
                : maxSummaryGroups + 1;
        List<EclAllowanceSummary> summaries = jdbc.query(
                connection -> {
                    var statement = connection.prepareStatement(SELECT_SUMMARIES);
                    statement.setDate(1, Date.valueOf(baseDate));
                    statement.setQueryTimeout(queryTimeoutSeconds);
                    if (jdbcMaxRows > 0) {
                        statement.setMaxRows(jdbcMaxRows);
                    }
                    return statement;
                },
                (resultSet, rowNumber) -> {
                    // A grouped sum can hide equal and opposite source-row errors. Keep this
                    // check inside the bounded grouped query, before constructing any summary.
                    if (resultSet.getLong("invalid_source_rows") != 0) {
                        throw new IllegalStateException("ECL source row reconciliation failed");
                    }
                    return new EclAllowanceSummary(
                        resultSet.getDate("base_date").toLocalDate(),
                        resultSet.getString("run_id"),
                        resultSet.getString("model_version"),
                        resultSet.getString("legal_entity_code"),
                        resultSet.getString("currency_code"),
                        resultSet.getString("exposure_account_code"),
                        resultSet.getString("allowance_account_code"),
                        resultSet.getString("bad_debt_expense_account_code"),
                        resultSet.getString("reversal_income_account_code"),
                        resultSet.getBigDecimal("target_allowance_amount"),
                        resultSet.getBigDecimal("source_exposure_amount"),
                        resultSet.getBigDecimal("stage1_allowance_amount"),
                        resultSet.getBigDecimal("stage2_allowance_amount"),
                        resultSet.getBigDecimal("stage3_allowance_amount"));
                });
        if (summaries.size() > maxSummaryGroups) {
            throw new IllegalStateException(
                    "ECL summary groups exceed API hard cap of " + maxSummaryGroups);
        }
        return summaries;
    }
}
