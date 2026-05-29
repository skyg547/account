package com.ho.account.ecl.batch.config;

import com.ho.account.ecl.batch.support.BatchParameterUtils;
import com.ho.account.ecl.core.application.pipeline.AllowanceDataQualityService;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

/**
 * [Phase 1] IFRS 9 대손충당금 입력 데이터 전처리 및 품질 검증 배치.
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 본격적인 대손충당금 산출을 시작하기 전, '재료(데이터)'가 신선하고 정확한지 확인하는 단계입니다.
 * - DQ 체크: 데이터에 빠진 것이나 틀린 것이 없는지 검사합니다.
 */
@Configuration
@RequiredArgsConstructor
public class PreProcessingBatchConfig {

    /** 💡 [초보자 가이드] 배치의 전반적인 메타데이터를 관리하는 저장소입니다. */
    private final JobRepository jobRepository;
    
    /** 💡 [초보자 가이드] 데이터 작업의 일관성을 보장하는 관리자입니다. */
    private final PlatformTransactionManager transactionManager;

    /** 💡 [초보자 가이드] 데이터가 정확한지(누락 등) 검사하는 비즈니스 서비스입니다. */
    private final AllowanceDataQualityService allowanceDataQualityService;
    
    /** 💡 [초보자 가이드] CDM(마트) 데이터를 엔진으로 가져오는 태스크릿입니다. */
    private final com.ho.account.ecl.batch.job.tasklet.CdmSyncTasklet cdmSyncTasklet;

    /**
     * Phase 1 마스터 Job: CDM 동기화와 데이터 품질 검토를 순차적으로 실행합니다.
     */
    @Bean
    public Job preProcessingJob() {
        return new JobBuilder("preProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cdmSyncStep())             // 0. CDM 동기화 (마트에서 데이터 가져오기)
                .next(dqStep())                  // 1. 데이터 품질 체크 (데이터가 깨졌는지 확인)
                .build();
    }

    /**
     * [CDM Sync Step] 통합 마트 데이터 동기화 단계
     * 💡 [초보자 가이드] 대손충당금(IFRS9) 산출의 '단일 진실 공급원'인 마트에서 최신 데이터를 가져옵니다.
     */
    @Bean
    public Step cdmSyncStep() {
        return new StepBuilder("cdmSyncStep", jobRepository)
                .tasklet(cdmSyncTasklet, transactionManager)
                .build();
    }

    /**
     * [DQ Step] 데이터 품질(Data Quality) 검증 단계
     * 💡 [초보자 가이드] 원천 데이터에서 필수값이 빠졌거나, 논리적으로 말이 안 되는 데이터를 찾아내어 경고를 보냅니다.
     */
    @Bean
    public Step dqStep() {
        return new StepBuilder("dqStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 💡 배치 파라미터에서 기준 일자를 알아냅니다.
                    LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());
                    // 💡 모든 데이터 품질 규칙을 실행합니다.
                    allowanceDataQualityService.runFullDataQualityCheck(baseDate);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
