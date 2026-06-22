package com.ho.account.expenditure.payable.batch;

import com.ho.account.expenditure.application.port.in.PaymentUseCase;
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
public class PayablePaymentRunBatchConfig {

    @Bean
    Job payablePaymentRunJob(JobRepository jobRepository, Step payablePaymentRunStep) {
        return new JobBuilder("payablePaymentRunJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(payablePaymentRunStep)
                .build();
    }

    @Bean
    Step payablePaymentRunStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PaymentUseCase paymentUseCase) {
        return new StepBuilder("payablePaymentRunStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobParameters parameters = contribution.getStepExecution().getJobParameters();
                    LocalDate runDate = localDate(parameters, "runDate", LocalDate.now());
                    String createdBy = text(parameters, "createdBy", "PAYABLE_BATCH");
                    String description = text(parameters, "description", "Scheduled payable payment run");
                    paymentUseCase.initiatePaymentRun(runDate, description, createdBy);
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
