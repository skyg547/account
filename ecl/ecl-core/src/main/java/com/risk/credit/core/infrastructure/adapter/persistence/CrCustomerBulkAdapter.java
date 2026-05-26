package com.risk.credit.core.infrastructure.adapter.persistence;

import com.risk.credit.core.application.port.out.CrCustomerBulkPort;
import com.risk.credit.core.domain.exposure.CrCustomer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * [Adapter] 고객 대량 처리를 위한 JDBC 기반 어댑터
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CrCustomerBulkAdapter implements CrCustomerBulkPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void bulkUpsert(List<CrCustomer> customers) {
        log.info("📊 [JDBC] 고객 대량 Upsert 실행 (건수: {})", customers.size());

        SqlParameterSource[] batch = customers.stream()
                .map(cust -> {
                    MapSqlParameterSource params = new MapSqlParameterSource();
                    params.addValue("customerCode", cust.getCustomerCode());
                    params.addValue("customerName", cust.getCustomerName());
                    params.addValue("customerType", cust.getCustomerType() != null ? cust.getCustomerType().name() : "CORPORATE");
                    params.addValue("internalRating", cust.getInternalRating());
                    params.addValue("externalRating", cust.getExternalRating());
                    params.addValue("industryCode", cust.getIndustryCode());
                    params.addValue("countryCode", cust.getCountryCode());
                    params.addValue("isSme", cust.getIsSme());
                    params.addValue("warningLevel", cust.getWarningLevel());
                    return params;
                })
                .toArray(SqlParameterSource[]::new);

        jdbcTemplate.batchUpdate(CrBatchQueryProvider.UPSERT_CUSTOMER_FROM_CDM_SQL, batch);
    }
}
