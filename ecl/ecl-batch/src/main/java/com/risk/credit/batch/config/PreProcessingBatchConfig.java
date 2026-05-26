package com.risk.credit.batch.config;

import com.risk.credit.batch.job.tasklet.RegulatoryContractTasklet;
import com.risk.credit.batch.support.BatchParameterUtils;
import com.risk.credit.core.application.pipeline.RiskDataQualityService;
import com.risk.credit.core.application.service.monitoring.CreditMonitoringService;
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
 * [Phase 1] 데이터 전처리 및 품질 검증 배치 (Pre-Processing Phase)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 본격적인 리스크 계산을 시작하기 전, '재료(데이터)'가 신선하고 정확한지 확인하는 단계입니다.
 * - DQ 체크: 데이터에 빠진 것이나 틀린 것이 없는지 검사합니다.
 * - 모니터링: 오늘의 대출 현황을 미리 살펴봅니다.
 * - 매핑: 복잡한 대출 계약과 담보 정보를 하나로 묶어줍니다.
 */
@Configuration
@RequiredArgsConstructor
public class PreProcessingBatchConfig {

    /** 💡 [초보자 가이드] 배치의 전반적인 메타데이터를 관리하는 저장소입니다. */
    private final JobRepository jobRepository;
    
    /** 💡 [초보자 가이드] 데이터 작업의 일관성을 보장하는 관리자입니다. */
    private final PlatformTransactionManager transactionManager;

    /** 💡 [초보자 가이드] 데이터가 정확한지(누락 등) 검사하는 비즈니스 서비스입니다. */
    private final RiskDataQualityService riskDataQualityService;
    
    /** 💡 [초보자 가이드] 산출 전 상태를 기록하고 감시하는 서비스입니다. */
    private final CreditMonitoringService monitoringService;
    
    /** 💡 [초보자 가이드] 계약과 담보를 연결하는 복잡한 태스크를 담당하는 객체입니다. */
    private final RegulatoryContractTasklet regulatoryContractTasklet;
    
    /** 💡 [초보자 가이드] CDM(마트) 데이터를 엔진으로 가져오는 태스크릿입니다. */
    private final com.risk.credit.batch.job.tasklet.CdmSyncTasklet cdmSyncTasklet;

    /**
     * Phase 1 마스터 Job: 데이터 품질 검토와 기초 데이터 준비를 순차적으로 실행합니다.
     * 💡 [Step 0] CDM 싱크 -> [Step 1] DQ 체크 -> [Step 2] 모니터링 -> [Step 3] 규제 매핑 순서로 진행됩니다.
     */
    @Bean
    public Job preProcessingJob() {
        return new JobBuilder("preProcessingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(cdmSyncStep())             // 0. CDM 동기화 (마트에서 데이터 가져오기)
                .next(dqStep())                  // 1. 데이터 품질 체크 (데이터가 깨졌는지 확인)
                .next(monitoringStep())           // 2. 일회성 모니터링 수행 (현황 기록)
                .next(regulatoryContractStep())   // 3. 규제 매핑 데이터 준비 (계약-담보 연결)
                .build();
    }

    /**
     * [CDM Sync Step] 통합 마트 데이터 동기화 단계
     * 💡 [초보자 가이드] 리스크 산출의 '단일 진실 공급원'인 마트에서 최신 데이터를 가져옵니다.
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
                    riskDataQualityService.runFullDataQualityCheck(baseDate);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * [Monitoring Step] 산출 전 상태 점검 단계
     * 💡 [초보자 가이드] 오늘 리스크를 뽑기 전, 은행의 전체 자산 규모나 연체금액 등을 가볍게 훑어보고 기록합니다.
     */
    @Bean
    public Step monitoringStep() {
        return new StepBuilder("monitoringStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    // 💡 배치 파라미터에서 기준 일자를 알아냅니다.
                    LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());
                    // 💡 일일 모니터링 로직을 수행합니다.
                    monitoringService.runDailyMonitoring(baseDate);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * [Regulatory Mapping Step] 계약-담보 매핑 준비 단계
     * 💡 [초보자 가이드] 대출 하나에 담보가 여러 개일 수도 있고, 반대일 수도 있습니다. 
     *    이 단계에서는 이러한 복잡한 엉킴을 리스크 계산기가 이해하기 쉽게 미리 정리해둡니다.
     */
    @Bean
    public Step regulatoryContractStep() {
        return new StepBuilder("regulatoryContractStep", jobRepository)
                .tasklet(regulatoryContractTasklet, transactionManager)
                .build();
    }
}
