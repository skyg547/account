package com.ho.account.deposit.batch;

import com.ho.account.deposit.application.port.in.DepositBatchUseCase;
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
public class DepositAccountIntegrityBatchConfig {

    @Bean
    Job depositAccountIntegrityJob(JobRepository jobRepository, Step depositAccountIntegrityStep) {
        return new JobBuilder("depositAccountIntegrityJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(depositAccountIntegrityStep)
                .build();
    }

    @Bean
    Step depositAccountIntegrityStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            DepositBatchUseCase depositBatchUseCase) {
        return new StepBuilder("depositAccountIntegrityStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobParameters parameters = contribution.getStepExecution().getJobParameters();
                    LocalDate asOfDate = localDate(parameters, "asOfDate", LocalDate.now());
                    depositBatchUseCase.validateActiveAccounts(asOfDate);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    private LocalDate localDate(JobParameters parameters, String key, LocalDate defaultValue) {
        String value = parameters.getString(key);
        return value == null || value.isBlank() ? defaultValue : LocalDate.parse(value.trim());
    }
}
