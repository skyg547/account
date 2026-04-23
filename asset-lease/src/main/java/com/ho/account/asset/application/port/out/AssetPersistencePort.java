package com.ho.account.asset.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * 고정자산 영속성 포트 (Bulk 지원)
 */
public interface AssetPersistencePort {
    /**
     * 자산들의 감가상각 내역을 일괄 업데이트합니다. (JDBC Bulk 지원)
     */
    void updateDepreciationBulk(Map<Long, BigDecimal> depreciationResults, LocalDate lastDate);
}
