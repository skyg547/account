package com.ho.account.asset.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class FixedAssetTest {

    @Test
    @DisplayName("정액법 감가상각이 정확하게 계산되어야 한다.")
    void testStraightLineDepreciation() {
        FixedAsset asset = new FixedAsset();
        asset.setAcquisitionCost(new BigDecimal("12000000"));
        asset.setUsefulLife(5);
        asset.setResidualValue(BigDecimal.ZERO);
        asset.setDepreciationMethod("STRAIGHT_LINE");
        asset.setCurrentBookValue(new BigDecimal("12000000"));
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setDepreciationAmountPerPeriod(new BigDecimal("200000"));
        asset.setStatus("ACTIVE");

        BigDecimal amount = asset.depreciate(LocalDate.of(2026, 4, 30));

        assertThat(amount).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("11800000"));
    }

    @Test
    @DisplayName("장부가액이 잔존가치에 도달하면 상각이 완료되어야 한다.")
    void testDepreciationCompletion() {
        FixedAsset asset = new FixedAsset();
        asset.setDepreciationMethod("STRAIGHT_LINE"); // 드디어 원인 해결!
        asset.setCurrentBookValue(new BigDecimal("100000"));
        asset.setResidualValue(BigDecimal.ZERO);
        asset.setDepreciationAmountPerPeriod(new BigDecimal("200000"));
        asset.setStatus("ACTIVE");
        asset.setAccumulatedDepreciation(new BigDecimal("11900000"));

        BigDecimal amount = asset.depreciate(LocalDate.now());

        assertThat(amount).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getStatus()).isEqualTo("FULLY_DEPRECIATED");
    }
}
