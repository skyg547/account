package com.ho.account.expenditure.resolution.batch;

import com.ho.account.expenditure.application.port.in.ExpenditureResolutionBatchUseCase;
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
public class ExpenditureResolutionApprovalBatchConfig {

    @Bean
    Job expenditureResolutionApprovalJob(
            JobRepository jobRepository,
            Step expenditureResolutionApprovalStep) {
        return new JobBuilder("expenditureResolutionApprovalJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(expenditureResolutionApprovalStep)
                .build();
    }

    @Bean
    Step expenditureResolutionApprovalStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ExpenditureResolutionBatchUseCase batchUseCase) {
        return new StepBuilder("expenditureResolutionApprovalStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobParameters parameters = contribution.getStepExecution().getJobParameters();
                    LocalDate endDate = localDate(parameters, "endDate", LocalDate.now());
                    LocalDate startDate = localDate(parameters, "startDate", endDate);
                    LocalDate paymentDueDate = localDate(parameters, "paymentDueDate", endDate);
                    batchUseCase.approveRequestedResolutions(startDate, endDate, paymentDueDate);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    private LocalDate localDate(JobParameters parameters, String key, LocalDate defaultValue) {
        String value = parameters.getString(key);
        return value == null || value.isBlank() ? defaultValue : LocalDate.parse(value.trim());
    }
}
