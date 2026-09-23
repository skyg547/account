package com.ho.account.cashflow.batch;

import com.ho.account.cashflow.core.application.port.in.CashflowStatementUseCase;
import com.ho.account.cashflow.core.application.port.in.GenerateStatementCommand;
import com.ho.account.cashflow.core.domain.CashflowMethod;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;

@Configuration(proxyBeanMethods = false)
public class CashflowAggregationJobConfig {

    public static final String JOB_NAME = "cashflowAggregationJob";
    public static final String STEP_NAME = "cashflowAggregationStep";

    @Bean
    Job cashflowAggregationJob(JobRepository jobRepository, Step cashflowAggregationStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(cashflowAggregationStep)
                .build();
    }

    @Bean
    Step cashflowAggregationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            org.springframework.batch.core.step.tasklet.Tasklet cashflowAggregationTasklet) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(cashflowAggregationTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    org.springframework.batch.core.step.tasklet.Tasklet cashflowAggregationTasklet(
            CashflowStatementUseCase statementUseCase,
            @Value("#{jobParameters['statementId']}") String statementId,
            @Value("#{jobParameters['fiscalYear']}") Long fiscalYear,
            @Value("#{jobParameters['fiscalPeriod']}") Long fiscalPeriod,
            @Value("#{jobParameters['method']}") String method,
            @Value("#{jobParameters['currency']}") String currency,
            @Value("#{jobParameters['generatedAt']}") String generatedAt) {
        return (contribution, chunkContext) -> {
            // The initial job owns execution parameters only; future readers will supply classified lines to core.
            statementUseCase.generate(new GenerateStatementCommand(
                    requireText(statementId, "statementId"),
                    requireRange(fiscalYear, "fiscalYear", 1900, 9999),
                    requireRange(fiscalPeriod, "fiscalPeriod", 1, 12),
                    parseMethod(method),
                    requireText(currency, "currency"),
                    parseGeneratedAt(generatedAt),
                    List.of()));
            return RepeatStatus.FINISHED;
        };
    }

    private static CashflowMethod parseMethod(String value) {
        try {
            return CashflowMethod.valueOf(requireText(value, "method").toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("method must be DIRECT or INDIRECT", exception);
        }
    }

    private static LocalDateTime parseGeneratedAt(String value) {
        try {
            return LocalDateTime.parse(requireText(value, "generatedAt"));
        } catch (DateTimeParseException exception) {
            throw new IllegalArgumentException("generatedAt must use ISO-8601 local date-time format", exception);
        }
    }

    private static int requireRange(Long value, String fieldName, int minimum, int maximum) {
        if (value == null || value < minimum || value > maximum) {
            throw new IllegalArgumentException(fieldName + " must be between " + minimum + " and " + maximum);
        }
        return value.intValue();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " job parameter is required");
        }
        return value.trim();
    }
}
