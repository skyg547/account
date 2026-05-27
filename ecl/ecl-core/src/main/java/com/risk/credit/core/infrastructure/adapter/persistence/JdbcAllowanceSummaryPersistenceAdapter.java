package com.risk.credit.core.infrastructure.adapter.persistence;

import com.risk.credit.core.application.port.out.AllowanceSummaryBuildPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class JdbcAllowanceSummaryPersistenceAdapter implements AllowanceSummaryBuildPort {

    private static final String ELIGIBLE_RESULT_PREDICATE = """
            FROM cr_risk_results r
            JOIN cr_accounts a ON a.id = r.account_id
           WHERE r.base_date = ?
             AND r.status = 'COMPLETED'
             AND COALESCE(r.weighted_ecl, r.expected_loss) IS NOT NULL
            """;

    private static final String COUNT_ELIGIBLE_RESULTS = "SELECT COUNT(*) " + ELIGIBLE_RESULT_PREDICATE;

    private static final String COUNT_MISSING_MAPPINGS = """
            SELECT COUNT(*)
              FROM cr_risk_results r
              JOIN cr_accounts a ON a.id = r.account_id
             WHERE r.base_date = ?
               AND r.status = 'COMPLETED'
               AND COALESCE(r.weighted_ecl, r.expected_loss) IS NOT NULL
               AND NOT EXISTS (
                   SELECT 1
                     FROM allowance_account_mappings m
                    WHERE m.active = TRUE
                      AND m.product_code = a.product_code
                      AND (m.biz_unit_code IS NULL OR m.biz_unit_code = a.biz_unit_cd)
                      AND (m.currency_code IS NULL OR m.currency_code = a.currency)
               )
            """;

    private static final String DELETE_SUMMARY = """
            DELETE FROM allowance_summary
             WHERE base_date = ?
            """;

    private static final String INSERT_SUMMARIES = """
            WITH eligible_results AS (
                SELECT r.id AS result_id,
                       r.base_date,
                       r.staging,
                       COALESCE(r.weighted_ecl, r.expected_loss, 0) AS ecl_amount,
                       COALESCE(r.ead_star, r.ead, a.outstanding_amt, 0) AS exposure_amount,
                       a.product_code,
                       a.biz_unit_cd AS biz_unit_code,
                       a.currency AS account_currency_code
                  FROM cr_risk_results r
                  JOIN cr_accounts a ON a.id = r.account_id
                 WHERE r.base_date = ?
                   AND r.status = 'COMPLETED'
                   AND COALESCE(r.weighted_ecl, r.expected_loss) IS NOT NULL
            ),
            mapped_results AS (
                SELECT e.base_date,
                       e.result_id,
                       e.staging,
                       e.ecl_amount,
                       e.exposure_amount,
                       m.legal_entity_code,
                       COALESCE(m.currency_code, e.account_currency_code) AS currency_code,
                       m.exposure_account_code,
                       m.allowance_account_code,
                       m.bad_debt_expense_account_code,
                       m.reversal_income_account_code,
                       ROW_NUMBER() OVER (
                           PARTITION BY e.base_date, e.result_id
                           ORDER BY
                               CASE WHEN m.biz_unit_code = e.biz_unit_code THEN 2
                                    WHEN m.biz_unit_code IS NULL THEN 1
                                    ELSE 0 END DESC,
                               CASE WHEN m.currency_code = e.account_currency_code THEN 2
                                    WHEN m.currency_code IS NULL THEN 1
                                    ELSE 0 END DESC,
                               m.id ASC
                       ) AS mapping_rank
                  FROM eligible_results e
                  JOIN allowance_account_mappings m
                    ON m.active = TRUE
                   AND m.product_code = e.product_code
                   AND (m.biz_unit_code IS NULL OR m.biz_unit_code = e.biz_unit_code)
                   AND (m.currency_code IS NULL OR m.currency_code = e.account_currency_code)
            )
            INSERT INTO allowance_summary (
                   base_date,
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
                   stage3_allowance_amount,
                   created_at,
                   updated_at
            )
            SELECT base_date,
                   ? AS run_id,
                   ? AS model_version,
                   legal_entity_code,
                   currency_code,
                   exposure_account_code,
                   allowance_account_code,
                   bad_debt_expense_account_code,
                   reversal_income_account_code,
                   SUM(ecl_amount) AS target_allowance_amount,
                   SUM(exposure_amount) AS source_exposure_amount,
                   SUM(CASE WHEN staging = 'STAGE1' THEN ecl_amount ELSE 0 END) AS stage1_allowance_amount,
                   SUM(CASE WHEN staging = 'STAGE2' THEN ecl_amount ELSE 0 END) AS stage2_allowance_amount,
                   SUM(CASE WHEN staging = 'STAGE3' THEN ecl_amount ELSE 0 END) AS stage3_allowance_amount,
                   CURRENT_TIMESTAMP AS created_at,
                   CURRENT_TIMESTAMP AS updated_at
              FROM mapped_results
             WHERE mapping_rank = 1
             GROUP BY base_date,
                      legal_entity_code,
                      currency_code,
                      exposure_account_code,
                      allowance_account_code,
                      bad_debt_expense_account_code,
                      reversal_income_account_code
            """;

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int countEligibleResults(LocalDate baseDate) {
        Integer count = jdbcTemplate.queryForObject(COUNT_ELIGIBLE_RESULTS, Integer.class, Date.valueOf(baseDate));
        return count == null ? 0 : count;
    }

    @Override
    public int countMissingAccountMappings(LocalDate baseDate) {
        Integer count = jdbcTemplate.queryForObject(COUNT_MISSING_MAPPINGS, Integer.class, Date.valueOf(baseDate));
        return count == null ? 0 : count;
    }

    @Override
    public void deleteByBaseDate(LocalDate baseDate) {
        jdbcTemplate.update(DELETE_SUMMARY, Date.valueOf(baseDate));
    }

    @Override
    public int insertSummariesFromRiskResults(LocalDate baseDate, String runId, String modelVersion) {
        return jdbcTemplate.update(INSERT_SUMMARIES, Date.valueOf(baseDate), runId, modelVersion);
    }
}
