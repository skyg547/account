package com.ho.account.asset.application.port.out;

import com.ho.account.asset.domain.FixedAssetDepreciationResult;

import java.time.LocalDate;
import java.util.List;

/**
 * 고정자산 영속성 포트 (Bulk 지원)
 */
public interface AssetPersistencePort {
    /**
     * 자산들의 감가상각 결과를 일괄 업데이트합니다. (JDBC Bulk 지원)
     */
    void updateDepreciationBulk(List<FixedAssetDepreciationResult> depreciationResults, LocalDate lastDate);
}