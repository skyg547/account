package com.ho.account.reporting.adapter.in.batch;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * [Spring Batch Job 설정] 재무제표 생성 배치
 * 초보자 가이드: Job은 배치 전체 이름표이고, Step은 그 안에서 실제로 수행하는 한 단위 작업입니다.
 * 이 설정은 "언제, 어떤 기준일, 누가 실행했는지"만 관리하고 보고서 산출 규칙은 core 유즈케이스에 맡깁니다.
 */
@Configuration
@RequiredArgsConstructor
public class ReportingStatementBatchConfig {

    public static final String JOB_NAME = "reportingStatementGenerationJob";
    public static final String STEP_NAME = "reportingStatementGenerationStep";

    private final ReportingBatchAdapter reportingBatchAdapter;

    @Bean
    public Job reportingStatementGenerationJob(
            JobRepository jobRepository,
            Step reportingStatementGenerationStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(reportingStatementGenerationStep)
                .build();
    }

    @Bean
    public Step reportingStatementGenerationStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            Tasklet reportingStatementGenerationTasklet) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(reportingStatementGenerationTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public Tasklet reportingStatementGenerationTasklet(
            @Value("#{jobParameters['baseDate']}") String baseDate,
            @Value("#{jobParameters['requester']}") String requester) {
        return (contribution, chunkContext) -> {
            reportingBatchAdapter.runStatementGenerationBatch(
                    parseBaseDate(baseDate),
                    requireText(requester, "requester"));
            return RepeatStatus.FINISHED;
        };
    }

    static LocalDate parseBaseDate(String value) {
        String text = requireText(value, "baseDate");
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("baseDate must use yyyy-MM-dd format.", ex);
        }
    }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " job parameter is required.");
        }
        return value.trim();
    }
}
