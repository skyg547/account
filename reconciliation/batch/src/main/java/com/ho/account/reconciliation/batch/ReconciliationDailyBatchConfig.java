package com.ho.account.reconciliation.batch;

import com.ho.account.reconciliation.application.port.in.ReconciliationBatchUseCase;
import java.time.LocalDate;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class ReconciliationDailyBatchConfig {

    @Bean
    Job reconciliationDailyJob(JobRepository jobRepository, Step reconciliationDailyStep) {
        return new JobBuilder("reconciliationDailyJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(reconciliationDailyStep)
                .build();
    }

    @Bean
    Step reconciliationDailyStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ReconciliationBatchUseCase reconciliationBatchUseCase) {
        return new StepBuilder("reconciliationDailyStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobParameters parameters = contribution.getStepExecution().getJobParameters();
                    LocalDate reconciliationDate = localDate(parameters, "reconciliationDate", LocalDate.now());
                    String runBy = text(parameters, "runBy", "RECONCILIATION_BATCH");
                    boolean deepMode = Boolean.parseBoolean(text(parameters, "deepMode", "false"));
                    reconciliationBatchUseCase.runDailyReconciliation(reconciliationDate, runBy, deepMode);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    private LocalDate localDate(JobParameters parameters, String key, LocalDate defaultValue) {
        String value = parameters.getString(key);
        return value == null || value.isBlank() ? defaultValue : LocalDate.parse(value.trim());
    }

    private String text(JobParameters parameters, String key, String defaultValue) {
        String value = parameters.getString(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}
