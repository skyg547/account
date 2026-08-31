package com.ho.account.asset.batch;

import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.infrastructure.persistence.repository.FixedAssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(
        classes = AssetLeaseBatchApplication.class,
        properties = {
                "spring.datasource.url=jdbc:h2:mem:asset_lease_batch_idempotency;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE"
        }
)
@ActiveProfiles("local")
class AssetDepreciationBatchIdempotencyIntegrationTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job assetDepreciationJob;

    @Autowired
    private FixedAssetRepository fixedAssetRepository;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(assetDepreciationJob);
        fixedAssetRepository.deleteAll();
    }

    @Test
    @DisplayName("동일 회계기간에 감가상각 배치를 2회 연속 실행해도 이중 상각 없이 멱등하게 유지된다.")
    void repeatedBatchExecutionWithinSamePeriodIsIdempotent() throws Exception {
        // Given: 신규 자산 1건과 전월 상각된 자산 1건
        FixedAsset newAsset = createAsset("FA-001", "노트북 A", new BigDecimal("12000000.00"), new BigDecimal("200000.00"), null);
        FixedAsset priorAsset = createAsset("FA-002", "서버 B", new BigDecimal("24000000.00"), new BigDecimal("400000.00"), LocalDate.of(2026, 3, 31));
        priorAsset.setAccumulatedDepreciation(new BigDecimal("400000.00"));
        priorAsset.setCurrentBookValue(new BigDecimal("23600000.00"));

        fixedAssetRepository.saveAll(List.of(newAsset, priorAsset));

        JobParameters params1 = new JobParametersBuilder()
                .addString("targetDate", "2026-04-30")
                .addLong("runTime", System.currentTimeMillis())
                .toJobParameters();

        // When 1: 1차 배치 실행 (2026-04-30)
        JobExecution execution1 = jobLauncherTestUtils.launchJob(params1);
        assertThat(execution1.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Then 1: 두 자산 모두 당월(4월) 상각 반영 확인
        FixedAsset asset1AfterRun1 = fixedAssetRepository.findByAssetCode("FA-001").orElseThrow();
        assertThat(asset1AfterRun1.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("11800000.00"));
        assertThat(asset1AfterRun1.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(asset1AfterRun1.getLastDepreciationDate()).isEqualTo(LocalDate.of(2026, 4, 30));

        FixedAsset asset2AfterRun1 = fixedAssetRepository.findByAssetCode("FA-002").orElseThrow();
        assertThat(asset2AfterRun1.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("23200000.00"));
        assertThat(asset2AfterRun1.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("800000.00"));
        assertThat(asset2AfterRun1.getLastDepreciationDate()).isEqualTo(LocalDate.of(2026, 4, 30));

        // When 2: 동일 일자(2026-04-30) 2차 배치 재실행
        JobParameters params2 = new JobParametersBuilder()
                .addString("targetDate", "2026-04-30")
                .addLong("runTime", System.currentTimeMillis() + 1000)
                .toJobParameters();
        JobExecution execution2 = jobLauncherTestUtils.launchJob(params2);
        assertThat(execution2.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Then 2: 재실행 후에도 장부가액/누계액이 변하지 않고 멱등성 유지
        FixedAsset asset1AfterRun2 = fixedAssetRepository.findByAssetCode("FA-001").orElseThrow();
        assertThat(asset1AfterRun2.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("11800000.00"));
        assertThat(asset1AfterRun2.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("200000.00"));
        assertThat(asset1AfterRun2.getLastDepreciationDate()).isEqualTo(LocalDate.of(2026, 4, 30));

        FixedAsset asset2AfterRun2 = fixedAssetRepository.findByAssetCode("FA-002").orElseThrow();
        assertThat(asset2AfterRun2.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("23200000.00"));
        assertThat(asset2AfterRun2.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("800000.00"));
        assertThat(asset2AfterRun2.getLastDepreciationDate()).isEqualTo(LocalDate.of(2026, 4, 30));

        // When 3: 동일 월 내 다른 일자(2026-04-15) 실행
        JobParameters params3 = new JobParametersBuilder()
                .addString("targetDate", "2026-04-15")
                .addLong("runTime", System.currentTimeMillis() + 2000)
                .toJobParameters();
        JobExecution execution3 = jobLauncherTestUtils.launchJob(params3);
        assertThat(execution3.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Then 3: 동일 월 내 다른 일자 실행에도 장부가액/누계액 불변
        FixedAsset asset1AfterRun3 = fixedAssetRepository.findByAssetCode("FA-001").orElseThrow();
        assertThat(asset1AfterRun3.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("11800000.00"));
        assertThat(asset1AfterRun3.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("200000.00"));

        // When 4: 다음 회계기간(2026-05-31) 배치 실행
        JobParameters params4 = new JobParametersBuilder()
                .addString("targetDate", "2026-05-31")
                .addLong("runTime", System.currentTimeMillis() + 3000)
                .toJobParameters();
        JobExecution execution4 = jobLauncherTestUtils.launchJob(params4);
        assertThat(execution4.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Then 4: 5월 상각 정상 반영
        FixedAsset asset1AfterRun4 = fixedAssetRepository.findByAssetCode("FA-001").orElseThrow();
        assertThat(asset1AfterRun4.getCurrentBookValue()).isEqualByComparingTo(new BigDecimal("11600000.00"));
        assertThat(asset1AfterRun4.getAccumulatedDepreciation()).isEqualByComparingTo(new BigDecimal("400000.00"));
        assertThat(asset1AfterRun4.getLastDepreciationDate()).isEqualTo(LocalDate.of(2026, 5, 31));
    }

    private FixedAsset createAsset(String code, String name, BigDecimal cost, BigDecimal periodAmount, LocalDate lastDepreciationDate) {
        FixedAsset asset = new FixedAsset();
        asset.setAssetCode(code);
        asset.setAssetName(name);
        asset.setAccountCode("1201");
        asset.setAcquisitionDate(LocalDate.of(2026, 1, 1));
        asset.setAcquisitionCost(cost);
        asset.setCurrentBookValue(cost);
        asset.setResidualValue(BigDecimal.ZERO);
        asset.setUsefulLife(60);
        asset.setDepreciationMethod("STRAIGHT_LINE");
        asset.setDepreciationAmountPerPeriod(periodAmount);
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setStatus("ACTIVE");
        asset.setLastDepreciationDate(lastDepreciationDate);
        return asset;
    }
}
