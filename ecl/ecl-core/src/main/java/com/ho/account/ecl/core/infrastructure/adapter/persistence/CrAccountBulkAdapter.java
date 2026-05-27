package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.CrAccountBulkPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.Map;

/**
 * [Adapter] 계좌 대량 처리를 위한 JdbcTemplate 기반 고성능 어댑터
 * 
 * 💡 [아키텍처 혁신: 기술 종속성 제거]
 * QueryDSL QClass 의존성을 제거하고 원시 SQL(JDBC)을 사용하여 
 * 빌드 안정성을 확보하고 대용량(100M+) 처리 성능을 극대화했습니다.
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CrAccountBulkAdapter implements CrAccountBulkPort {
 
    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public int updateStagingByDelinquentDays(int lowerDays, Integer upperDays, String targetStage) {
        log.info("📊 [JDBC] 연체 일수 기반 스테이지({}) 업데이트 실행", targetStage);
        
        String sql = CrBatchQueryProvider.UPDATE_STAGING_BY_DAYS_SQL.formatted(
                upperDays != null ? "AND delinquent_days < :upperDays" : ""
        );

        Map<String, Object> params = Map.of(
            "targetStage", targetStage,
            "lowerDays", lowerDays,
            "upperDays", upperDays != null ? upperDays : 0
        );

        return jdbcTemplate.update(sql, params);
    }

    @Override
    public int updateStagingByWarningLevel(String warningLevel, String targetStage) {
        log.info("📊 [JDBC] 조기경보 레벨({}) 기반 스테이지({}) 업데이트 실행", warningLevel, targetStage);
        
        Map<String, Object> params = Map.of(
            "warningLevel", warningLevel,
            "targetStage", targetStage
        );

        return jdbcTemplate.update(CrBatchQueryProvider.UPDATE_STAGING_BY_WARNING_SQL, params);
    }

    @Override
    public int markInvalidIdentityAccounts() {
        return jdbcTemplate.update(CrBatchQueryProvider.MARK_INVALID_IDENTITY_SQL, Map.of());
    }

    @Override
    public int markInvalidRiskParamAccounts() {
        return jdbcTemplate.update(CrBatchQueryProvider.MARK_INVALID_RISK_PARAM_SQL, Map.of());
    }

    @Override
    public int markInvalidAmountAccounts() {
        return jdbcTemplate.update(CrBatchQueryProvider.MARK_INVALID_AMOUNT_SQL, Map.of());
    }

    @Override
    public void clearErrorMessages() {
        jdbcTemplate.update(CrBatchQueryProvider.CLEAR_ACCOUNT_ERROR_SQL, Map.of());
    }

    @Override
    public void bulkUpsert(java.util.List<com.ho.account.ecl.core.domain.exposure.CrAccount> accounts) {
        log.info("📊 [JDBC] 계좌 대량 Upsert 실행 (건수: {})", accounts.size());
        
        org.springframework.jdbc.core.namedparam.SqlParameterSource[] batch = accounts.stream()
                .map(acc -> {
                    org.springframework.jdbc.core.namedparam.MapSqlParameterSource params = new org.springframework.jdbc.core.namedparam.MapSqlParameterSource();
                    params.addValue("accountNo", acc.getAccountNo());
                    params.addValue("customerId", acc.getCustomer() != null ? acc.getCustomer().getId() : null);
                    params.addValue("productCode", acc.getProductCode());
                    params.addValue("currency", acc.getCurrency());
                    params.addValue("notionalAmount", acc.getNotionalAmount());
                    params.addValue("outstandingAmount", acc.getOutstandingAmount());
                    params.addValue("productCategory", acc.getProductCategory());
                    params.addValue("interestRate", acc.getInterestRate());
                    params.addValue("repaymentMethod", acc.getRepaymentMethod());
                    params.addValue("gracePeriod", acc.getGracePeriod());
                    params.addValue("repaymentFreq", acc.getRepaymentFreq());
                    params.addValue("branchCode", acc.getBranchCode());
                    params.addValue("bizUnitCode", acc.getBizUnitCode());
                    params.addValue("staging", acc.getStaging() != null ? acc.getStaging().name() : "STAGE1");
                    params.addValue("delinquentDays", acc.getDelinquentDays());
                    params.addValue("openDate", acc.getOpenDate());
                    params.addValue("maturityDate", acc.getMaturityDate());
                    params.addValue("internalRating", acc.getInternalRating());
                    params.addValue("isDebtRestructured", acc.getIsDebtRestructured());
                    return params;
                })
                .toArray(org.springframework.jdbc.core.namedparam.SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(CrBatchQueryProvider.UPSERT_ACCOUNT_FROM_CDM_SQL, batch);
    }
}
