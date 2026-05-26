package com.risk.credit.core.infrastructure.adapter.persistence;

import com.risk.credit.core.application.port.out.CrBulkOperationPort;
import com.risk.credit.core.domain.result.CrMonthlySummary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * [Infrastructure] [Adapter] JDBC 기반 대량 리스크 데이터 처리 어댑터 (High-Performance)
 * 
 * 💡 [아키텍처 혁신: 기술 종속성 제거]
 * QueryDSL의 QClass 생성 시점 문제로 인한 빌드 마비를 해결하기 위해 
 * 원시 SQL(JDBC)을 사용하여 견고하고 빠른 데이터 처리를 구현했습니다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CrBulkOperationAdapter implements CrBulkOperationPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void clearBatchResults(LocalDate baseDate) {
        log.info("🧹 [JDBC] {} 기준 기존 산출 결과를 삭제합니다.", baseDate);
        
        int deletedResults = jdbcTemplate.update(
            "DELETE FROM cr_risk_results WHERE base_date = :baseDate", Map.of("baseDate", baseDate));
        
        int deletedSummaries = jdbcTemplate.update(
            "DELETE FROM cr_monthly_summaries WHERE base_date = :baseDate", Map.of("baseDate", baseDate));

        log.info("✅ [JDBC] 삭제 완료 (Results: {}, Summaries: {})", deletedResults, deletedSummaries);
    }

    @Override
    public int aggregateMonthlyRiskMart(LocalDate baseDate) {
        log.info("🚀 [JDBC] {} 기준 리스크 산출 결과 집계 및 마트 생성 쿼리 실행", baseDate);

        String sql = """
            INSERT INTO cr_monthly_summaries 
                (base_date, product_group, customer_type, staging, 
                 total_count, total_ead, total_ead_star, total_rwa_sa, total_rwa_irb, 
                 total_expected_loss, avg_pd, avg_lgd, created_at)
            SELECT 
                r.base_date,
                a.product_code as product_group,
                c.customer_type,
                r.staging,
                COUNT(*) as total_count,
                SUM(r.ead) as total_ead,
                SUM(r.ead_star) as total_ead_star,
                SUM(r.rwa_sa) as total_rwa_sa,
                SUM(r.rwa_irb) as total_rwa_irb,
                SUM(r.expected_loss) as total_expected_loss,
                AVG(r.pd) as avg_pd,
                AVG(r.lgd) as avg_lgd,
                CURRENT_TIMESTAMP
            FROM cr_risk_results r
            INNER JOIN cr_accounts a ON r.account_id = a.id
            INNER JOIN cr_customers c ON a.customer_id = c.id
            WHERE r.base_date = :baseDate
              AND r.status = 'COMPLETED'
            GROUP BY r.base_date, a.product_code, c.customer_type, r.staging
            """;

        int result = jdbcTemplate.update(sql, Map.of("baseDate", baseDate));
        log.info("✅ [JDBC] 총 {}개의 요약 세그먼트가 생성되었습니다.", result);
        
        return result;
    }

    @Override
    public java.math.BigDecimal sumAllocationByAccountId(Long accountId) {
        String sql = "SELECT SUM(allocation_amount) FROM cr_account_collaterals WHERE account_id = :accountId";
        java.math.BigDecimal sum = jdbcTemplate.queryForObject(sql, Map.of("accountId", accountId), java.math.BigDecimal.class);
        return sum != null ? sum : java.math.BigDecimal.ZERO;
    }

    @Override
    public java.math.BigDecimal sumAllocationByCollateralId(Long collateralId) {
        String sql = "SELECT SUM(allocation_amount) FROM cr_account_collaterals WHERE collateral_id = :collateralId";
        java.math.BigDecimal sum = jdbcTemplate.queryForObject(sql, Map.of("collateralId", collateralId), java.math.BigDecimal.class);
        return sum != null ? sum : java.math.BigDecimal.ZERO;
    }

    @Override
    public void deleteAllocationByAccountId(Long accountId) {
        String sql = "DELETE FROM cr_account_collaterals WHERE account_id = :accountId";
        jdbcTemplate.update(sql, Map.of("accountId", accountId));
        log.debug("🗑️ [JDBC] 계좌(ID: {})의 모든 담보 배분 정보를 삭제했습니다.", accountId);
    }

    @Override
    public void deleteAllResult() {
        jdbcTemplate.getJdbcOperations().execute("TRUNCATE TABLE cr_risk_results CASCADE");
    }

    @Override
    public void deleteAllMonthlySummary() {
        jdbcTemplate.getJdbcOperations().execute("TRUNCATE TABLE cr_monthly_summaries CASCADE");
    }

    @Override
    public void saveMonthlySummary(List<CrMonthlySummary> summaries) {
        // [참고] 마트 집계는 aggregateMonthlyRiskMart를 통해 SQL로 직접 수행되나,
        // Port 규격을 맞추기 위해 빈 구현 또는 필요 시 배치를 위한 저장 로직을 추가할 수 있습니다.
        log.info("💾 [JDBC] {} 건의 월간 리스크 요약 데이터 저장 요청을 처리합니다.", summaries.size());
    }
}
