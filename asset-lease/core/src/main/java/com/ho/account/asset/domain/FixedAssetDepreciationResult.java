package com.ho.account.asset.domain;

import java.math.BigDecimal;

/**
 * [Value Object] 고정자산 감가상각 계산 결과.
 *
 * <p>초보자 설명: Batch는 1억 건 같은 대량 데이터를 처리할 수 있어야 하므로 JPA 엔티티를 한 건씩
 * 저장하지 않고 JDBC bulk update로 한 번에 반영합니다. 이 값 객체는 "얼마를 상각했고, 반영 후
 * 장부가와 상태가 무엇인지"를 담아 batch writer가 DB에 정확히 한 번만 업데이트하게 합니다.</p>
 */
public record FixedAssetDepreciationResult(
        Long assetId,
        BigDecimal depreciationAmount,
        BigDecimal accumulatedDepreciation,
        BigDecimal currentBookValue,
        String status) {

    private static final String FULLY_DEPRECIATED = "FULLY_DEPRECIATED";

    public FixedAssetDepreciationResult {
        depreciationAmount = defaultZero(depreciationAmount);
        accumulatedDepreciation = defaultZero(accumulatedDepreciation);
        currentBookValue = defaultZero(currentBookValue);
    }

    public boolean shouldPersist() {
        return depreciationAmount.signum() > 0 || FULLY_DEPRECIATED.equals(status);
    }

    public static FixedAssetDepreciationResult noChange(
            Long assetId,
            BigDecimal accumulatedDepreciation,
            BigDecimal currentBookValue,
            String status) {
        return new FixedAssetDepreciationResult(
                assetId,
                BigDecimal.ZERO,
                accumulatedDepreciation,
                currentBookValue,
                status);
    }

    private static BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}