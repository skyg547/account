package com.ho.account.asset.infrastructure.adapter;

import com.ho.account.asset.application.port.out.AssetPersistencePort;
import com.ho.account.asset.domain.FixedAssetDepreciationResult;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * <h3>JDBC 기반 고성능 어댑터</h3>
 * 1억 건 이상의 대용량 처리를 위해 Batch Update 기능을 제공합니다.
 */
@Repository
@RequiredArgsConstructor
public class AssetJdbcAdapter implements AssetPersistencePort {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void updateDepreciationBulk(List<FixedAssetDepreciationResult> results, LocalDate lastDate) {
        if (results == null || results.isEmpty() || lastDate == null) {
            return;
        }

        LocalDate periodStart = lastDate.withDayOfMonth(1);
        String sql = "UPDATE fixed_assets SET " +
                     "accumulated_depreciation = ?, " +
                     "current_book_value = ?, " +
                     "status = ?, " +
                     "last_depreciation_date = ?, " +
                     "updated_at = NOW() " +
                     "WHERE id = ? AND (last_depreciation_date IS NULL OR last_depreciation_date < ?)";

        List<Object[]> batchArgs = new ArrayList<>();
        for (FixedAssetDepreciationResult result : results) {
            if (result.shouldPersist()) {
                batchArgs.add(new Object[]{
                        result.accumulatedDepreciation(),
                        result.currentBookValue(),
                        result.status(),
                        lastDate,
                        result.assetId(),
                        periodStart
                });
            }
        }

        // 1,000건 단위로 나누어 실행 (성능 최적화)
        if (!batchArgs.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batchArgs);
        }
    }
}