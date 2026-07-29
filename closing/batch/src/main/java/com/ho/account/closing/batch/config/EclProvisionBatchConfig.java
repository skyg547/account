package com.ho.account.closing.batch.config;

import com.ho.account.closing.application.service.EclProvisionService;
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
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

/**
 * ECL provision Batch adapter.
 *
 * <p>The date and lineage ID are mandatory identifying parameters. Missing parameters never fall
 * back to the system clock, so a restarted JobInstance cannot silently process a different period.</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EclProvisionBatchConfig {

    public static final String JOB_NAME = "eclProvisionJob";
    private static final String STEP_NAME = "eclProvisionStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EclProvisionService eclProvisionService;

    @Bean
    public JobParametersValidator eclProvisionJobParametersValidator() {
        return ClosingJobParameters.requiredDateAndPositiveId("closingDate", "provisionBatchId");
    }

    @Bean
    public Job eclProvisionJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(eclProvisionJobParametersValidator())
                .start(eclProvisionStep())
                .build();
    }

    @Bean
    @JobScope
    public Step eclProvisionStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(eclProvisionTasklet(null, null), transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public org.springframework.batch.core.step.tasklet.Tasklet eclProvisionTasklet(
            @Value("#{jobParameters['closingDate']}") String closingDateValue,
            @Value("#{jobParameters['provisionBatchId']}") Long provisionBatchId) {
        return (contribution, chunkContext) -> {
            LocalDate closingDate = requiredDate(closingDateValue, "closingDate");
            Long batchId = requiredPositiveLong(provisionBatchId, "provisionBatchId");
            eclProvisionService.processEclProvision(closingDate, batchId);
            log.info("ECL Provision Job completed successfully for date: {}", closingDate);
            return RepeatStatus.FINISHED;
        };
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
}
