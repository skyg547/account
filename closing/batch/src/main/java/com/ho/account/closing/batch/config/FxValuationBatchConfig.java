package com.ho.account.closing.batch.config;

import com.ho.account.closing.application.pipeline.FxValuationPipeline;
import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.closing.application.service.FxValuationBalance;
import com.ho.account.closing.batch.adapter.out.JournalFxValuationBalanceSource;
import com.ho.account.closing.batch.support.ClosingJobParameters;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

/**
 * FX valuation Batch adapter.
 *
 * <p>Batch owns stable parameters, fixed-range partitioning, chunk/checkpoint and concurrency.
 * Signed balance calculation and journal construction remain in Closing core.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FxValuationBatchConfig {

    public static final String JOB_NAME = "fxValuationJob";
    private static final String STEP_NAME = "fxValuationStep";
    private static final String WORKER_STEP_NAME = "fxValuationWorkerStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final JournalFxValuationBalanceSource balanceSource;
    private final FxValuationPipeline fxValuationPipeline;
    private final ClosingAccountingProperties accountingProperties;

    @Value("${account.closing.batch.fx.chunk-size:1000}")
    private int chunkSize;

    @Value("${account.closing.batch.fx.grid-size:4}")
    private int gridSize;

    @Bean
    public JobParametersValidator fxValuationJobParametersValidator() {
        return ClosingJobParameters.requiredDateAndPositiveId("valuationDate", "valuationBatchId");
    }

    @Bean
    public Job fxValuationJob(Step fxValuationStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(fxValuationJobParametersValidator())
                .start(fxValuationStep)
                .build();
    }

    @Bean
    public Step fxValuationStep(Partitioner fxValuationPartitioner, Step fxValuationWorkerStep) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .partitioner(WORKER_STEP_NAME, fxValuationPartitioner)
                .step(fxValuationWorkerStep)
                .gridSize(requirePositive(gridSize, "gridSize"))
                .taskExecutor(fxValuationTaskExecutor())
                .build();
    }

    @Bean
    public Step fxValuationWorkerStep(
            ItemReader<FxValuationBalance> fxValuationItemReader,
            ItemWriter<FxValuationBalance> fxValuationItemWriter) {
        return new StepBuilder(WORKER_STEP_NAME, jobRepository)
                .<FxValuationBalance, FxValuationBalance>chunk(
                        requirePositive(chunkSize, "chunkSize"),
                        transactionManager)
                .reader(fxValuationItemReader)
                .writer(fxValuationItemWriter)
                .build();
    }

    @Bean
    @JobScope
    public Partitioner fxValuationPartitioner(
            @Value("#{jobParameters['valuationDate']}") String valuationDateValue) {
        return requestedGridSize -> {
            LocalDate valuationDate = requiredDate(valuationDateValue, "valuationDate");
            String reportingCurrency = accountingProperties.requireFxValuationReportingCurrencyCode();
            int boundedGridSize = requirePositive(requestedGridSize, "requestedGridSize");
            var partitions = balanceSource.createPartitions(
                    valuationDate,
                    reportingCurrency,
                    boundedGridSize);
            log.info("Created {} fixed FX account ranges for {}", partitions.size(), valuationDate);
            return partitions;
        };
    }

    @Bean
    @StepScope
    public ItemReader<FxValuationBalance> fxValuationItemReader(
            @Value("#{jobParameters['valuationDate']}") String valuationDateValue,
            @Value("#{stepExecutionContext['startAccountCode']}") String startAccountCode,
            @Value("#{stepExecutionContext['endAccountCode']}") String endAccountCode) {
        return balanceSource.createReader(
                requiredDate(valuationDateValue, "valuationDate"),
                accountingProperties.requireFxValuationReportingCurrencyCode(),
                startAccountCode,
                endAccountCode,
                requirePositive(chunkSize, "chunkSize"));
    }

    @Bean
    @StepScope
    public ItemWriter<FxValuationBalance> fxValuationItemWriter(
            @Value("#{jobParameters['valuationDate']}") String valuationDateValue,
            @Value("#{jobParameters['valuationBatchId']}") Long valuationBatchId) {
        return chunk -> fxValuationPipeline.processChunk(
                chunk.getItems(),
                requiredDate(valuationDateValue, "valuationDate"),
                requiredPositiveLong(valuationBatchId, "valuationBatchId"));
    }

    @Bean
    public TaskExecutor fxValuationTaskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("fx-valuation-");
        taskExecutor.setConcurrencyLimit(requirePositive(gridSize, "gridSize"));
        return taskExecutor;
    }

    private LocalDate requiredDate(String value, String name) {
        try {
            return ClosingJobParameters.requireDate(value, name);
        } catch (JobParametersInvalidException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    private Long requiredPositiveLong(Long value, String name) {
        try {
            return ClosingJobParameters.requirePositiveLong(value, name);
        } catch (JobParametersInvalidException exception) {
            throw new IllegalArgumentException(exception.getMessage(), exception);
        }
    }

    private int requirePositive(int value, String name) {
        if (value <= 0) {
            throw new IllegalStateException(name + " must be positive");
        }
        return value;
    }
}
