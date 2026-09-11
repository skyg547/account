package com.ho.account.loan.batch.config;

import com.ho.account.loan.service.ScheduledRepaymentService;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * Scheduled repayment orchestration. repaymentDate is the stable identifying parameter.
 * The reader pages the immutable due-date schedule set, so repaid loan status changes cannot
 * shift later pages. Freeze schedules for that date while a job is running/restarting.
 * Core durably reserves each loan/date before its separate remote journal transaction; successful
 * items are reusable after a chunk restart, but ambiguous pending writes fail until reconciled.
 * No skip/retry policy masks core failures. The default single worker uses 100-item checkpoints.
 */
@Configuration
@RequiredArgsConstructor
public class LoanScheduledRepaymentBatchConfig {
    public static final String JOB_NAME = "loanScheduledRepaymentJob";
    private static final int CHUNK_SIZE = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final ScheduledRepaymentService repaymentService;

    @Bean
    public Job loanScheduledRepaymentJob(Step loanScheduledRepaymentStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(loanScheduledRepaymentParametersValidator())
                .start(loanScheduledRepaymentStep)
                .build();
    }

    @Bean
    public JobParametersValidator loanScheduledRepaymentParametersValidator() {
        return parameters -> {
            new DefaultJobParametersValidator(new String[]{"repaymentDate"}, new String[]{}).validate(parameters);
            if (!parameters.getParameter("repaymentDate").isIdentifying()) {
                throw new JobParametersInvalidException("repaymentDate must identify the job instance");
            }
            try {
                LocalDate.parse(parameters.getString("repaymentDate"));
            } catch (DateTimeParseException | IllegalArgumentException exception) {
                throw new JobParametersInvalidException("repaymentDate must be an ISO date");
            }
        };
    }

    @Bean
    public Step loanScheduledRepaymentStep(
            JpaPagingItemReader<Long> scheduledRepaymentLoanReader,
            ItemWriter<Long> scheduledRepaymentWriter) {
        return new StepBuilder("loanScheduledRepaymentStep", jobRepository)
                .<Long, Long>chunk(CHUNK_SIZE, transactionManager)
                .reader(scheduledRepaymentLoanReader)
                .writer(scheduledRepaymentWriter)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Long> scheduledRepaymentLoanReader(
            @Value("#{jobParameters['repaymentDate']}") String repaymentDate) {
        return new JpaPagingItemReaderBuilder<Long>()
                .name("scheduledRepaymentLoanReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT s.loan.id FROM EIRAmortizationSchedule s "
                        + "WHERE s.scheduleDate = :repaymentDate ORDER BY s.loan.id")
                .parameterValues(Map.of("repaymentDate", LocalDate.parse(repaymentDate)))
                .pageSize(CHUNK_SIZE)
                .saveState(true)
                .build();
    }

    @Bean
    @StepScope
    public ItemWriter<Long> scheduledRepaymentWriter(
            @Value("#{jobParameters['repaymentDate']}") String repaymentDate) {
        LocalDate date = LocalDate.parse(repaymentDate);
        return chunk -> {
            for (Long loanId : chunk) {
                repaymentService.processIndividualRepayment(loanId, date);
            }
        };
    }
}
