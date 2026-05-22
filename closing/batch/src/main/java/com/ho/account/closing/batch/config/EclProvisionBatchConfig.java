package com.ho.account.closing.batch.config;

import com.ho.account.closing.batch.service.EclProvisionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * [배치 처리 (Batch Processing) - 기대신용손실(ECL) 충당금 산출]
 *
 * 🐣 [초보자를 위한 설명]
 * 결산 시점(월말)에 실행되는 '대손충당금 자동화 공장'입니다.
 * 
 * 대손충당금 산출은 데이터 건건이 처리하기보다, 기말 원장(GL) 잔액을 통째로 읽어와
 * 포트폴리오 수준에서 계산하는 것이 효율적이므로 ItemReader/Writer 구조 대신 단일 Tasklet으로 구성했습니다.
 * 
 * 실행 시 파라미터로 `closingDate=2026-05-31` 과 같이 처리 일자를 넘겨줄 수 있습니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class EclProvisionBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EclProvisionService eclProvisionService;

    public static final String JOB_NAME = "eclProvisionJob";
    private static final String STEP_NAME = "eclProvisionStep";

    @Bean
    public Job eclProvisionJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
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
    public Tasklet eclProvisionTasklet(
            @Value("#{jobParameters['closingDate']}") String closingDateStr,
            @Value("#{jobParameters['provisionBatchId']}") Long provisionBatchId) {
        return (contribution, chunkContext) -> {
            LocalDate closingDate = (closingDateStr != null) 
                    ? LocalDate.parse(closingDateStr, DateTimeFormatter.ISO_DATE) 
                    : LocalDate.now();
            
            Long batchId = resolveBatchId(closingDate, provisionBatchId);

            try {
                eclProvisionService.processEclProvision(closingDate, batchId);
                log.info("ECL Provision Job completed successfully for date: {}", closingDate);
            } catch (Exception e) {
                log.error("Failed to process ECL provision: {}", e.getMessage(), e);
                throw e; // 배치 실패 처리를 위해 예외 던짐
            }

            return RepeatStatus.FINISHED;
        };
    }

    private Long resolveBatchId(LocalDate closingDate, Long provisionBatchId) {
        if (provisionBatchId != null) {
            return provisionBatchId;
        }
        return Long.parseLong(closingDate.format(DateTimeFormatter.BASIC_ISO_DATE));
    }
}
