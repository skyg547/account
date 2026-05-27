package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.port.out.EclAllowanceResultPort;
import com.ho.account.closing.domain.EclAllowanceSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

/**
 * JDBC adapter for finalized ECL allowance summary rows.
 */
@Repository
@RequiredArgsConstructor
public class JdbcEclAllowanceResultAdapter implements EclAllowanceResultPort {

    private static final String SELECT_SUMMARIES = """
            SELECT base_date,
                   run_id,
                   model_version,
                   legal_entity_code,
                   currency_code,
                   exposure_account_code,
                   allowance_account_code,
                   bad_debt_expense_account_code,
                   reversal_income_account_code,
                   target_allowance_amount,
                   source_exposure_amount,
                   stage1_allowance_amount,
                   stage2_allowance_amount,
                   stage3_allowance_amount
              FROM allowance_summary
             WHERE base_date = ?
             ORDER BY legal_entity_code, currency_code, exposure_account_code, allowance_account_code
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<EclAllowanceSummary> loadSummaries(LocalDate baseDate) {
        return jdbcTemplate.query(
                SELECT_SUMMARIES,
                (rs, rowNum) -> new EclAllowanceSummary(
                        rs.getDate("base_date").toLocalDate(),
                        rs.getString("run_id"),
                        rs.getString("model_version"),
                        rs.getString("legal_entity_code"),
                        rs.getString("currency_code"),
                        rs.getString("exposure_account_code"),
                        rs.getString("allowance_account_code"),
                        rs.getString("bad_debt_expense_account_code"),
                        rs.getString("reversal_income_account_code"),
                        rs.getBigDecimal("target_allowance_amount"),
                        rs.getBigDecimal("source_exposure_amount"),
                        rs.getBigDecimal("stage1_allowance_amount"),
                        rs.getBigDecimal("stage2_allowance_amount"),
                        rs.getBigDecimal("stage3_allowance_amount")),
                Date.valueOf(baseDate));
    }
}
