package com.ho.account.asset.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RightOfUseAssetTest {

    @Test
    @DisplayName("100.00을 3회 상각하면 마지막 회차가 잔여 33.34를 정리한다.")
    void finalInstallmentClearsRoundingRemainder() {
        RightOfUseAsset asset = new RightOfUseAsset();
        asset.setInitialValue(new BigDecimal("100.00"));
        asset.setCurrentBookValue(new BigDecimal("100.00"));
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setDepreciationAmountPerPeriod(new BigDecimal("33.33"));
        asset.setStatus(RightOfUseAsset.STATUS_ACTIVE);

        assertThat(asset.depreciate(false)).isEqualByComparingTo("33.33");
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo("66.67");
        assertThat(asset.getStatus()).isEqualTo(RightOfUseAsset.STATUS_ACTIVE);
        assertThat(asset.depreciate(false)).isEqualByComparingTo("33.33");
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo("33.34");
        assertThat(asset.getStatus()).isEqualTo(RightOfUseAsset.STATUS_ACTIVE);

        // The final installment absorbs only the rounding remainder left by prior periods.
        assertThat(asset.depreciate(true)).isEqualByComparingTo("33.34");
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo("100.00");
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo("0.00");
        assertThat(asset.getStatus()).isEqualTo(RightOfUseAsset.STATUS_FULLY_DEPRECIATED);
        assertThat(asset.depreciate(true)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("마지막 회차가 아니면 잔여 장부가액을 선상각하지 않는다.")
    void nonFinalInstallmentPreservesRemainingBookValue() {
        RightOfUseAsset asset = new RightOfUseAsset();
        asset.setInitialValue(new BigDecimal("100.00"));
        asset.setCurrentBookValue(new BigDecimal("33.34"));
        asset.setAccumulatedDepreciation(new BigDecimal("66.66"));
        asset.setDepreciationAmountPerPeriod(new BigDecimal("33.33"));
        asset.setStatus(RightOfUseAsset.STATUS_ACTIVE);

        assertThat(asset.depreciate(false)).isEqualByComparingTo("33.33");
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo("0.01");
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo("99.99");
        assertThat(asset.getStatus()).isEqualTo(RightOfUseAsset.STATUS_ACTIVE);
    }

    @Test
    @DisplayName("마지막 회차에도 장부가액을 넘겨 상각하거나 음수로 만들지 않는다.")
    void finalInstallmentClampsToRemainingBookValue() {
        RightOfUseAsset asset = new RightOfUseAsset();
        asset.setInitialValue(new BigDecimal("100.00"));
        asset.setCurrentBookValue(new BigDecimal("20.00"));
        asset.setAccumulatedDepreciation(new BigDecimal("80.00"));
        asset.setDepreciationAmountPerPeriod(new BigDecimal("33.33"));
        asset.setStatus(RightOfUseAsset.STATUS_ACTIVE);

        assertThat(asset.depreciate(true)).isEqualByComparingTo("20.00");
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo("100.00");
        assertThat(asset.getStatus()).isEqualTo(RightOfUseAsset.STATUS_FULLY_DEPRECIATED);
    }

    @Test
    @DisplayName("계획된 상각액이 남은 장부가액을 초과하는 경우 남은 장부가액만큼만 안전 상각액으로 계산된다.")
    void calculateSafeDepreciationAmountAdjustsToRemainingBookValue() {
        RightOfUseAsset asset = new RightOfUseAsset();
        asset.setInitialValue(new BigDecimal("10000.00"));
        asset.setCurrentBookValue(new BigDecimal("500.00"));
        asset.setAccumulatedDepreciation(new BigDecimal("9500.00"));
        asset.setDepreciationAmountPerPeriod(new BigDecimal("1000.00"));
        asset.setStatus(RightOfUseAsset.STATUS_ACTIVE);

        BigDecimal safeAmount = asset.calculateSafeDepreciationAmount(new BigDecimal("1000.00"));

        assertThat(safeAmount).isEqualByComparingTo("500.00");
    }

    @Test
    @DisplayName("장부가액이 이미 0원 이하이면 안전 상각액은 0원이다.")
    void calculateSafeDepreciationAmountReturnsZeroWhenBookValueIsZero() {
        RightOfUseAsset asset = new RightOfUseAsset();
        asset.setInitialValue(new BigDecimal("10000.00"));
        asset.setCurrentBookValue(BigDecimal.ZERO);
        asset.setAccumulatedDepreciation(new BigDecimal("10000.00"));
        asset.setDepreciationAmountPerPeriod(new BigDecimal("1000.00"));
        asset.setStatus(RightOfUseAsset.STATUS_ACTIVE);

        BigDecimal safeAmount = asset.calculateSafeDepreciationAmount(null);

        assertThat(safeAmount).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("정상 감가상각 시 누적상각액이 증가하고 장부가액이 감소한다.")
    void depreciateNormalCase() {
        RightOfUseAsset asset = new RightOfUseAsset();
        asset.setInitialValue(new BigDecimal("12000.00"));
        asset.setCurrentBookValue(new BigDecimal("12000.00"));
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setDepreciationAmountPerPeriod(new BigDecimal("1000.00"));
        asset.setStatus(RightOfUseAsset.STATUS_ACTIVE);

        BigDecimal actualAmount = asset.depreciate();

        assertThat(actualAmount).isEqualByComparingTo("1000.00");
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo("1000.00");
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo("11000.00");
        assertThat(asset.getStatus()).isEqualTo(RightOfUseAsset.STATUS_ACTIVE);
    }

    @Test
    @DisplayName("상각액이 남은 장부가액을 초과하여 상각 완료되면 장부가액이 0원이 되고 FULLY_DEPRECIATED 상태로 전환된다.")
    void depreciateClampsToZeroAndTransitionsToFullyDepreciated() {
        RightOfUseAsset asset = new RightOfUseAsset();
        asset.setInitialValue(new BigDecimal("12000.00"));
        asset.setCurrentBookValue(new BigDecimal("400.00"));
        asset.setAccumulatedDepreciation(new BigDecimal("11600.00"));
        asset.setDepreciationAmountPerPeriod(new BigDecimal("1000.00"));
        asset.setStatus(RightOfUseAsset.STATUS_ACTIVE);

        BigDecimal actualAmount = asset.depreciate();

        assertThat(actualAmount).isEqualByComparingTo("400.00");
        assertThat(asset.getAccumulatedDepreciation()).isEqualByComparingTo("12000.00");
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getStatus()).isEqualTo(RightOfUseAsset.STATUS_FULLY_DEPRECIATED);
    }

    @Test
    @DisplayName("ACTIVE 상태가 아닌 자산은 감가상각이 실행되지 않는다.")
    void depreciateInactiveAssetDoesNothing() {
        RightOfUseAsset asset = new RightOfUseAsset();
        asset.setInitialValue(new BigDecimal("12000.00"));
        asset.setCurrentBookValue(BigDecimal.ZERO);
        asset.setAccumulatedDepreciation(new BigDecimal("12000.00"));
        asset.setDepreciationAmountPerPeriod(new BigDecimal("1000.00"));
        asset.setStatus(RightOfUseAsset.STATUS_FULLY_DEPRECIATED);

        BigDecimal actualAmount = asset.depreciate();

        assertThat(actualAmount).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(asset.getCurrentBookValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
