package com.ho.account.asset.application.pipeline;

import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.domain.FixedAssetDepreciationResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DepreciationPipelineTest {

    private final DepreciationPipeline pipeline = new DepreciationPipeline();

    @Test
    void calculateBatchReturnsBulkResultsWithoutMutatingAssets() {
        FixedAsset active = activeAsset(1L, new BigDecimal("1000000"), BigDecimal.ZERO, new BigDecimal("100000"));
        FixedAsset inactive = activeAsset(2L, new BigDecimal("1000000"), BigDecimal.ZERO, new BigDecimal("100000"));
        inactive.setStatus("DISPOSED");

        List<FixedAssetDepreciationResult> results = pipeline.calculateBatch(
                List.of(active, inactive),
                LocalDate.of(2026, 4, 30));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).assetId()).isEqualTo(1L);
        assertThat(results.get(0).depreciationAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(results.get(0).currentBookValue()).isEqualByComparingTo(new BigDecimal("900000"));
        assertThat(active.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("1000000"));
        assertThat(active.getAccumulatedDepreciation()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void calculateBatchIncludesFullyDepreciatedStatusResult() {
        FixedAsset asset = activeAsset(1L, new BigDecimal("100000"), BigDecimal.ZERO, new BigDecimal("200000"));

        List<FixedAssetDepreciationResult> results = pipeline.calculateBatch(
                List.of(asset),
                LocalDate.of(2026, 4, 30));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).depreciationAmount()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(results.get(0).status()).isEqualTo("FULLY_DEPRECIATED");
        assertThat(asset.getStatus()).isEqualTo("ACTIVE");
    }

    private FixedAsset activeAsset(Long id, BigDecimal currentBookValue, BigDecimal residualValue, BigDecimal periodAmount) {
        FixedAsset asset = new FixedAsset();
        asset.setId(id);
        asset.setCurrentBookValue(currentBookValue);
        asset.setResidualValue(residualValue);
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setDepreciationAmountPerPeriod(periodAmount);
        asset.setStatus("ACTIVE");
        return asset;
    }
}