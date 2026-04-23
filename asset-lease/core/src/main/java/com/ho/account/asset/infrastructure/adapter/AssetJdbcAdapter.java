package com.ho.account.asset.infrastructure.adapter;

import com.ho.account.asset.application.port.out.AssetPersistencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * <h3>JDBC 기반 고성능 어댑터</h3>
 * 1억 건 이상의 대용량 처리를 위해 Batch Update 기능을 제공합니다.
 */
@Repository
@RequiredArgsConstructor
public class AssetJdbcAdapter implements AssetPersistencePort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void updateDepreciationBulk(Map<Long, BigDecimal> results, LocalDate lastDate) {
        String sql = "UPDATE fixed_assets SET " +
                     "accumulated_depreciation = accumulated_depreciation + ?, " +
                     "current_book_value = current_book_value - ?, " +
                     "last_depreciation_date = ?, " +
                     "updated_at = NOW() " +
                     "WHERE id = ?";

        List<Object[]> batchArgs = new ArrayList<>();
        results.forEach((id, amount) -> {
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                batchArgs.add(new Object[]{amount, amount, lastDate, id});
            }
        });

        // 1,000건 단위로 나누어 실행 (성능 최적화)
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }
}
