package com.ho.account.tax.batch;

import com.ho.account.tax.application.port.in.TaxInvoiceBatchUseCase;
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
public class TaxInvoiceValidationBatchConfig {

    @Bean
    Job taxInvoiceValidationJob(JobRepository jobRepository, Step taxInvoiceValidationStep) {
        return new JobBuilder("taxInvoiceValidationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(taxInvoiceValidationStep)
                .build();
    }

    @Bean
    Step taxInvoiceValidationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            TaxInvoiceBatchUseCase taxInvoiceBatchUseCase) {
        return new StepBuilder("taxInvoiceValidationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobParameters parameters = contribution.getStepExecution().getJobParameters();
                    LocalDate endDate = localDate(parameters, "endDate", LocalDate.now());
                    LocalDate startDate = localDate(parameters, "startDate", endDate);
                    taxInvoiceBatchUseCase.validatePurchaseInvoices(startDate, endDate);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    private LocalDate localDate(JobParameters parameters, String key, LocalDate defaultValue) {
        String value = parameters.getString(key);
        return value == null || value.isBlank() ? defaultValue : LocalDate.parse(value.trim());
    }
}
