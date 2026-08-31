package com.ho.account.asset.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FixedAssetTest {

    @Test
    void acquisitionInitializationEstablishesNonNullFinancialBalances() {
        FixedAsset asset = new FixedAsset();
        asset.setAcquisitionCost(new BigDecimal("12000000.00"));
        asset.setResidualValue(new BigDecimal("1200000.00"));
        asset.setAccumulatedDepreciation(new BigDecimal("100.00"));

        asset.initializeAcquisitionBalances();

        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo("12000000.00");
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getResidualValue()).isEqualByComparingTo("1200000.00");
    }

    @Test
    void acquisitionInitializationRejectsResidualAboveCost() {
        FixedAsset asset = new FixedAsset();
        asset.setAcquisitionCost(new BigDecimal("100.00"));
        asset.setResidualValue(new BigDecimal("100.01"));

        assertThatThrownBy(asset::initializeAcquisitionBalances)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("residual value");
    }

    @Test
    void acquisitionInitializationRejectsNegativeResidual() {
        FixedAsset asset = new FixedAsset();
        asset.setAcquisitionCost(new BigDecimal("100.00"));
        asset.setResidualValue(new BigDecimal("-0.01"));

        assertThatThrownBy(asset::initializeAcquisitionBalances)
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not be negative");
    }

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

    @Test
    @DisplayName("calculateSafeDepreciationAmount는 잔존가치를 초과하는 상각 시 상각가능 남은 가액까지만 계산한다.")
    void calculateSafeDepreciationAmountCapsAtRemainingValueAboveResidual() {
        FixedAsset asset = createActiveAsset(new BigDecimal("500000"), new BigDecimal("100000"), new BigDecimal("1000000"));

        BigDecimal safeAmount = asset.calculateSafeDepreciationAmount(null);

        assertThat(safeAmount).isEqualByComparingTo(new BigDecimal("400000"));
    }

    @Test
    @DisplayName("동일 회계기간(동일 연-월)에 depreciate를 2회 호출하면 최초 1회만 상각되고 2번째는 0원을 반환하며 장부가액을 보존한다.")
    void depreciateCalledTwiceInSamePeriodIsIdempotent() {
        FixedAsset asset = createActiveAsset(new BigDecimal("12000000"), BigDecimal.ZERO, new BigDecimal("200000"));

        BigDecimal firstAmount = asset.depreciate(LocalDate.of(2026, 4, 30));
        assertThat(firstAmount).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("11800000"));
        assertThat(asset.getLastDepreciationDate()).isEqualTo(LocalDate.of(2026, 4, 30));

        // 동일 날짜 재실행
        BigDecimal secondAmount = asset.depreciate(LocalDate.of(2026, 4, 30));
        assertThat(secondAmount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("11800000"));
        assertThat(asset.getLastDepreciationDate()).isEqualTo(LocalDate.of(2026, 4, 30));

        // 동일 월 내 다른 날짜 재실행
        BigDecimal thirdAmount = asset.depreciate(LocalDate.of(2026, 4, 15));
        assertThat(thirdAmount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("11800000"));
        assertThat(asset.getLastDepreciationDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    @DisplayName("이미 당월 상각된 자산의 calculateDepreciation은 0원 및 상태 유지 결과를 반환한다.")
    void calculateDepreciationForAlreadyDepreciatedAssetReturnsNoChange() {
        FixedAsset asset = createActiveAsset(new BigDecimal("11800000"), BigDecimal.ZERO, new BigDecimal("200000"));
        asset.setAccumulatedDepreciation(new BigDecimal("200000"));
        asset.setLastDepreciationDate(LocalDate.of(2026, 4, 30));

        FixedAssetDepreciationResult result = asset.calculateDepreciation(LocalDate.of(2026, 4, 30));

        assertThat(result.depreciationAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.accumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(result.currentBookValue()).isEqualByComparingTo(new BigDecimal("11800000"));
        assertThat(result.status()).isEqualTo("ACTIVE");
        assertThat(result.shouldPersist()).isFalse();
    }

    @Test
    @DisplayName("과거 일자로 상각을 시도할 경우 상각되지 않고 0원을 반환한다.")
    void depreciateWithPastDateReturnsZero() {
        FixedAsset asset = createActiveAsset(new BigDecimal("11800000"), BigDecimal.ZERO, new BigDecimal("200000"));
        asset.setAccumulatedDepreciation(new BigDecimal("200000"));
        asset.setLastDepreciationDate(LocalDate.of(2026, 4, 30));

        BigDecimal amount = asset.depreciate(LocalDate.of(2026, 3, 31));

        assertThat(amount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("11800000"));
        assertThat(asset.getLastDepreciationDate()).isEqualTo(LocalDate.of(2026, 4, 30));
    }

    @Test
    @DisplayName("다음 회계기간으로 감가상각 실행 시 정상적으로 추가 상각이 수행된다.")
    void depreciateInSubsequentPeriodSucceeds() {
        FixedAsset asset = createActiveAsset(new BigDecimal("12000000"), BigDecimal.ZERO, new BigDecimal("200000"));

        BigDecimal aprilAmount = asset.depreciate(LocalDate.of(2026, 4, 30));
        assertThat(aprilAmount).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("11800000"));

        BigDecimal mayAmount = asset.depreciate(LocalDate.of(2026, 5, 31));
        assertThat(mayAmount).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("400000"));
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("11600000"));
        assertThat(asset.getLastDepreciationDate()).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    @Test
    @DisplayName("isDepreciatedForPeriod는 상각일자가 null이거나 기준일자가 null이면 false를 반환한다.")
    void isDepreciatedForPeriodHandlesNullDates() {
        FixedAsset asset = new FixedAsset();
        assertThat(asset.isDepreciatedForPeriod(LocalDate.of(2026, 4, 30))).isFalse();
        assertThat(asset.isDepreciatedForPeriod(null)).isFalse();

        asset.setLastDepreciationDate(LocalDate.of(2026, 4, 30));
        assertThat(asset.isDepreciatedForPeriod(null)).isFalse();
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
