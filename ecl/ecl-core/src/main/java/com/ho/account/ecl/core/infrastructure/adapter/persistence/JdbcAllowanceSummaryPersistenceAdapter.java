package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.AllowanceSummaryBuildPort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Date;
import java.time.LocalDate;

@Repository
@RequiredArgsConstructor
public class JdbcAllowanceSummaryPersistenceAdapter implements AllowanceSummaryBuildPort {

    private static final String ELIGIBLE_RESULT_PREDICATE = """
            FROM allowance_ecl_results r
            JOIN cr_accounts a ON a.id = r.account_id
           WHERE r.base_date = ?
             AND r.status = 'COMPLETED'
             AND COALESCE(r.weighted_ecl, r.expected_loss) IS NOT NULL
            """;

    private static final String COUNT_ELIGIBLE_RESULTS = "SELECT COUNT(*) " + ELIGIBLE_RESULT_PREDICATE;

    private static final String COUNT_MISSING_MAPPINGS = """
            SELECT COUNT(*)
              FROM allowance_ecl_results r
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

    // @todo [DDD 위반 / 성능 트레이드오프] INSERT_SUMMARIES 쿼리 내부에 `ROW_NUMBER() OVER (...)`를 통해 매핑 우선순위(biz_unit_code, currency_code 일치 여부)를 결정하는 핵심 비즈니스 룰이 SQL로 하드코딩되어 있습니다. 대량 처리 성능을 위한 타협일 수 있으나, 비즈니스 정책이 인프라 계층에 숨겨지게 되므로 도메인 규칙으로 추출하거나 명시적인 문서화가 필요합니다.
    private static final String INSERT_SUMMARIES = """
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
            SELECT mapped.base_date,
                   ? AS run_id,
                   ? AS model_version,
                   mapped.legal_entity_code,
                   mapped.currency_code,
                   mapped.exposure_account_code,
                   mapped.allowance_account_code,
                   mapped.bad_debt_expense_account_code,
                   mapped.reversal_income_account_code,
                   SUM(mapped.ecl_amount) AS target_allowance_amount,
                   SUM(mapped.exposure_amount) AS source_exposure_amount,
                   SUM(CASE WHEN mapped.staging = 'STAGE1' THEN mapped.ecl_amount ELSE 0 END) AS stage1_allowance_amount,
                   SUM(CASE WHEN mapped.staging = 'STAGE2' THEN mapped.ecl_amount ELSE 0 END) AS stage2_allowance_amount,
                   SUM(CASE WHEN mapped.staging = 'STAGE3' THEN mapped.ecl_amount ELSE 0 END) AS stage3_allowance_amount,
                   CURRENT_TIMESTAMP AS created_at,
                   CURRENT_TIMESTAMP AS updated_at
              FROM (
                    SELECT eligible.base_date,
                           eligible.result_id,
                           eligible.staging,
                           eligible.ecl_amount,
                           eligible.exposure_amount,
                           m.legal_entity_code,
                           COALESCE(m.currency_code, eligible.account_currency_code) AS currency_code,
                           m.exposure_account_code,
                           m.allowance_account_code,
                           m.bad_debt_expense_account_code,
                           m.reversal_income_account_code,
                           ROW_NUMBER() OVER (
                               PARTITION BY eligible.base_date, eligible.result_id
                               ORDER BY
                                   CASE WHEN m.biz_unit_code = eligible.biz_unit_code THEN 2
                                        WHEN m.biz_unit_code IS NULL THEN 1
                                        ELSE 0 END DESC,
                                   CASE WHEN m.currency_code = eligible.account_currency_code THEN 2
                                        WHEN m.currency_code IS NULL THEN 1
                                        ELSE 0 END DESC,
                                   m.id ASC
                           ) AS mapping_rank
                      FROM (
                            SELECT r.id AS result_id,
                                   r.base_date,
                                   r.staging,
                                   COALESCE(r.weighted_ecl, r.expected_loss, 0) AS ecl_amount,
                                   COALESCE(r.ead_star, r.ead, a.outstanding_amt, 0) AS exposure_amount,
                                   a.product_code,
                                   a.biz_unit_cd AS biz_unit_code,
                                   a.currency AS account_currency_code
                              FROM allowance_ecl_results r
                              JOIN cr_accounts a ON a.id = r.account_id
                             WHERE r.base_date = ?
                               AND r.status = 'COMPLETED'
                               AND COALESCE(r.weighted_ecl, r.expected_loss) IS NOT NULL
                      ) eligible
                      JOIN allowance_account_mappings m
                        ON m.active = TRUE
                       AND m.product_code = eligible.product_code
                       AND (m.biz_unit_code IS NULL OR m.biz_unit_code = eligible.biz_unit_code)
                       AND (m.currency_code IS NULL OR m.currency_code = eligible.account_currency_code)
              ) mapped
             WHERE mapped.mapping_rank = 1
             GROUP BY mapped.base_date,
                      mapped.legal_entity_code,
                      mapped.currency_code,
                      mapped.exposure_account_code,
                      mapped.allowance_account_code,
                      mapped.bad_debt_expense_account_code,
                      mapped.reversal_income_account_code
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
    public int insertSummariesFromAllowanceResults(LocalDate baseDate, String runId, String modelVersion) {
        return jdbcTemplate.update(INSERT_SUMMARIES, runId, modelVersion, Date.valueOf(baseDate));
    }
}

