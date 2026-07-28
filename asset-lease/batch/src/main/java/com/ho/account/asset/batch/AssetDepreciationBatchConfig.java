package com.ho.account.asset.batch;

import com.ho.account.asset.application.pipeline.DepreciationPipeline;
import com.ho.account.asset.application.port.out.AssetPersistencePort;
import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.domain.FixedAssetDepreciationResult;
import com.ho.account.asset.repository.FixedAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * <h3>고속 감가상각 배치 오케스트레이터</h3>
 * Agents.md 규율에 따라 비즈니스 로직을 포함하지 않고 Flow 제어에만 집중합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class AssetDepreciationBatchConfig {

    private final FixedAssetRepository fixedAssetRepository;
    private final DepreciationPipeline depreciationPipeline;
    private final AssetPersistencePort assetPersistencePort;

    @Bean
    public Job assetDepreciationJob(JobRepository jobRepository, Step fixedAssetBulkStep) {
        return new JobBuilder("assetDepreciationJob", jobRepository)
                .start(fixedAssetBulkStep)
                .build();
    }

    @Bean
    public Step fixedAssetBulkStep(JobRepository jobRepository,
                                   PlatformTransactionManager transactionManager,
                                   ItemWriter<FixedAsset> assetBulkWriter) {
        return new StepBuilder("fixedAssetBulkStep", jobRepository)
                .<FixedAsset, FixedAsset>chunk(1000, transactionManager) // 1,000건 단위 Chunk 처리
                .reader(assetReader())
                .processor(assetProcessor())
                .writer(assetBulkWriter)
                .build();
    }

    @Bean
    public ItemReader<FixedAsset> assetReader() {
        return new RepositoryItemReaderBuilder<FixedAsset>()
                .name("assetReader")
                .repository(fixedAssetRepository)
                .methodName("findByStatus")
                .arguments(Collections.singletonList("ACTIVE"))
                .pageSize(1000)
                .sorts(Collections.singletonMap("id", Sort.Direction.ASC))
                .build();
    }

    @Bean
    public ItemProcessor<FixedAsset, FixedAsset> assetProcessor() {
        return asset -> asset;
    }

    @Bean
    @StepScope
    public ItemWriter<FixedAsset> assetBulkWriter(
            @Value("#{jobParameters['targetDate']}") String targetDateParameter) {
        LocalDate targetDate = requireTargetDate(targetDateParameter);
        return items -> {
            List<FixedAsset> assets = new ArrayList<>();
            for (FixedAsset item : items) {
                assets.add(item);
            }
            List<FixedAssetDepreciationResult> result = depreciationPipeline.calculateBatch(assets, targetDate);
            if (!result.isEmpty()) {
                assetPersistencePort.updateDepreciationBulk(result, targetDate);
            }
        };
    }

    private LocalDate requireTargetDate(String targetDateParameter) {
        if (targetDateParameter == null || targetDateParameter.isBlank()) {
            throw new IllegalArgumentException("JobParameter targetDate(yyyy-MM-dd) is required for assetDepreciationJob");
        }
        return LocalDate.parse(targetDateParameter);
    }
}