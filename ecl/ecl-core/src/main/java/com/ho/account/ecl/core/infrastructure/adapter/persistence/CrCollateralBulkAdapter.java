package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.ecl.core.application.port.out.CrCollateralBulkPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * [Adapter] 담보 대량 처리를 위한 JdbcTemplate 기반 어댑터
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class CrCollateralBulkAdapter implements CrCollateralBulkPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    @Override
    public void truncateAccountCollateralMappings() {
        // TRUNCATE는 DDL 성격이므로 JdbcOperations를 직접 꺼내서 실행합니다.
        jdbcTemplate.getJdbcOperations().execute(CrBatchQueryProvider.TRUNCATE_ACCOUNT_COLLATERAL_SQL);
    }

    @Override
    public int createMappingsByCustomerId() {
        // NamedParameterJdbcTemplate의 파라미터 맵 방식을 사용합니다.
        return jdbcTemplate.update(CrBatchQueryProvider.CREATE_ACCOUNT_COLLATERAL_MAPPING_SQL, java.util.Map.of());
    }
}
