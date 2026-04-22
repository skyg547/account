package com.ho.account.asset.batch;

import com.ho.account.asset.service.FixedAssetService;
import com.ho.account.asset.service.LeaseAccountingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

/**
 * <h3>자산/리스 감가상각 통합 배치</h3>
 * 매달 말일 실행되어 모든 고정자산 및 리스(ROU) 자산에 대한 상각 처리를 수행합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AssetDepreciationBatchConfig {

    private final FixedAssetService fixedAssetService;
    private final LeaseAccountingService leaseAccountingService;

    @Bean
    public Job assetDepreciationJob(JobRepository jobRepository, Step fixedAssetStep, Step leaseAssetStep) {
        return new JobBuilder("assetDepreciationJob", jobRepository)
                .start(fixedAssetStep)
                .next(leaseAssetStep)
                .build();
    }

    @Bean
    public Step fixedAssetStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("fixedAssetStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate lastDayOfLastMonth = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                    log.info("Starting Fixed Asset depreciation for: {}", lastDayOfLastMonth);
                    fixedAssetService.processMonthlyDepreciation(lastDayOfLastMonth);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    public Step leaseAssetStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("leaseAssetStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate lastDayOfLastMonth = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
                    log.info("Starting Lease (ROU) depreciation for: {}", lastDayOfLastMonth);
                    leaseAccountingService.processMonthlyLeaseAccounting(lastDayOfLastMonth);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
