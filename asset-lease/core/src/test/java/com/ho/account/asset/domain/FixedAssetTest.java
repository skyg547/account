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
        FixedAsset asset = createActiveAsset(new BigDecimal("12000000"), BigDecimal.ZERO, new BigDecimal("200000"));

        BigDecimal amount = asset.depreciate(LocalDate.of(2026, 4, 30));

        assertThat(amount).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("11800000"));
        assertThat(asset.getLastDepreciationDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    @DisplayName("감가상각 미리 계산은 엔티티 상태를 변경하지 않는다.")
    void calculateDepreciationShouldNotMutateEntity() {
        FixedAsset asset = createActiveAsset(new BigDecimal("12000000"), BigDecimal.ZERO, new BigDecimal("200000"));

        FixedAssetDepreciationResult result = asset.calculateDepreciation(LocalDate.of(2026, 4, 30));

        assertThat(result.depreciationAmount()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(result.accumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(result.currentBookValue()).isEqualByComparingTo(new BigDecimal("11800000"));
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("12000000"));
        assertThat(asset.getLastDepreciationDate()).isNull();
    }

    @Test
    @DisplayName("장부가액이 잔존가치에 도달하면 상각이 완료되어야 한다.")
    void testDepreciationCompletion() {
        FixedAsset asset = createActiveAsset(new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("200000"));
        asset.setAccumulatedDepreciation(new BigDecimal("11900000"));

        BigDecimal amount = asset.depreciate(LocalDate.now());

        assertThat(amount).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getStatus()).isEqualTo("FULLY_DEPRECIATED");
    }

    @Test
    @DisplayName("잔존가치 이하인 ACTIVE 자산은 음수 상각 없이 완료 상태로 전환된다.")
    void depreciateShouldNotCreateNegativeDepreciationWhenBookValueReachedResidualValue() {
        FixedAsset asset = createActiveAsset(new BigDecimal("100000"), new BigDecimal("100000"), new BigDecimal("200000"));

        BigDecimal amount = asset.depreciate(LocalDate.of(2026, 4, 30));

        assertThat(amount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(asset.getStatus()).isEqualTo("FULLY_DEPRECIATED");
    }

    private FixedAsset createActiveAsset(BigDecimal currentBookValue, BigDecimal residualValue, BigDecimal depreciationAmountPerPeriod) {
        FixedAsset asset = new FixedAsset();
        asset.setId(1L);
        asset.setAcquisitionCost(new BigDecimal("12000000"));
        asset.setUsefulLife(60);
        asset.setResidualValue(residualValue);
        asset.setDepreciationMethod("STRAIGHT_LINE");
        asset.setCurrentBookValue(currentBookValue);
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setDepreciationAmountPerPeriod(depreciationAmountPerPeriod);
        asset.setStatus("ACTIVE");
        return asset;
    }
}