package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.CrBulkOperationPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Map;

/**
 * [Infrastructure] [Adapter] JDBC 기반 대량 대손충당금 데이터 처리 어댑터.
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
            "DELETE FROM allowance_ecl_results WHERE base_date = :baseDate", Map.of("baseDate", baseDate));
        log.info("✅ [JDBC] 삭제 완료 (Results: {})", deletedResults);
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
        jdbcTemplate.getJdbcOperations().execute("TRUNCATE TABLE allowance_ecl_results CASCADE");
    }

}

