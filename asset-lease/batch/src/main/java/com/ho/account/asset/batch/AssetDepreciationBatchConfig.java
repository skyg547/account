package com.ho.account.asset.batch;

import com.ho.account.asset.application.pipeline.DepreciationPipeline;
import com.ho.account.asset.application.port.out.AssetPersistencePort;
import com.ho.account.asset.domain.FixedAsset;
import com.ho.account.asset.repository.FixedAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    public Step fixedAssetBulkStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("fixedAssetBulkStep", jobRepository)
                .<FixedAsset, Map<Long, BigDecimal>>chunk(1000, transactionManager) // 1,000건 단위 Chunk 처리
                .reader(assetReader())
                .processor(assetProcessor())
                .writer(assetBulkWriter())
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
    public ItemProcessor<FixedAsset, Map<Long, BigDecimal>> assetProcessor() {
        return asset -> {
            // @todo 배치 오케스트레이터가 날짜 결정과 도메인 계산을 직접 수행하지 않도록 JobParameter targetDate와 DepreciationPipeline 호출로 분리한다.
            LocalDate lastDay = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
            BigDecimal amount = asset.depreciate(lastDay);
            return amount.compareTo(BigDecimal.ZERO) > 0 ? Map.of(asset.getId(), amount) : null;
        };
    }

    @Bean
    public ItemWriter<Map<Long, BigDecimal>> assetBulkWriter() {
        return items -> {
            // @todo processor와 같은 targetDate JobParameter를 사용해 재실행 시점이 바뀌어도 같은 회계월을 갱신하도록 보강한다.
            LocalDate lastDay = LocalDate.now().minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
            for (Map<Long, BigDecimal> result : items) {
                assetPersistencePort.updateDepreciationBulk(result, lastDay);
                // 여기서 Kafka 전송 유즈케이스 호출 가능 (비동기)
            }
        };
    }
}
