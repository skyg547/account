package com.ho.account.ecl.batch.config;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * [Master] 전사 대손충당금(IFRS9) 산출 통합 마스터 Job 설정.
 * 
 * 💡 [아키텍처 가이드]
 * 분리된 5개의 전문 Job들을 순차적으로 오케스트레이션하여 
 * 전체 대손충당금(IFRS9) 파이프라인(End-to-End)을 완성합니다.
 * 대손충당금(IFRS9) 산출은 거대한 컨베이어 벨트에 비유할 수 있으며, 이 클래스는 그 벨트의 전원을 관리하고 흐름을 조정합니다.
 */
@Configuration
@RequiredArgsConstructor
public class CreditRiskMasterJobConfig {

    /** 💡 [초보자 가이드] 배치의 상태(성공, 실패 등)를 저장하고 관리하는 DB 리포지토리입니다. */
    private final JobRepository jobRepository;

    /** 💡 [초보자 가이드] 1단계: 원천 데이터(차주, 대출 등)를 불러오고 데이터가 싱싱한지 확인(DQ)하는 사전 처리 작업입니다. */
    private final Job preProcessingJob;

    /** 💡 [초보자 가이드] 2단계: 은행이 가진 담보(아파트, 주식 등)를 어떤 대출에 나눠줄지 가장 효율적인 조합을 찾습니다. */
    private final Job collateralOptimizationJob;

    /** 💡 [초보자 가이드] 3단계: 고객이 정상인지 아픈지(Staging) 진단하고, 망할 확률(PD)을 최초로 판정합니다. */
    private final Job riskStagingJob;

    /** 💡 [초보자 가이드] 4단계: 부도났을 때의 노출금액(EAD)과 손실률(LGD)이라는 정밀 리스크 파라미터를 최종 산출합니다. */
    private final Job exposureLgdJob;

    /** 💡 [초보자 가이드] 5단계: 위 단계들의 결과를 합쳐 '위험가중자산(RWA)'과 '충당금(ECL)'을 최종 계산하고 보고서를 만듭니다. */
    private final Job mainReportingJob;

    /**
     * 전사 대손충당금(IFRS9) 산출의 '심장'인 메인 Job을 생성합니다.
     * 컨베이어 벨트(Pipeline)의 순서를 1번부터 5번까지 차례대로 연결합니다.
     */
    @Bean
    public Job creditRiskMasterJob() {
        return new JobBuilder("creditRiskMasterJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(preProcessingStep())        // Step 1: 재료 준비 (전처리)
                .next(collateralOptimizationStep()) // Step 2: 담보 가치 분분 (최적화)
                .next(riskStagingStep())            // Step 3: 환자 진단 (스테이징)
                .next(exposureLgdStep())            // Step 4: 정밀 수치 계산 (EAD/LGD)
                .next(mainCalculationStep())        // Step 5: 최종 정산 (RWA/ECL)
                .build();
    }

    /** 💡 전처리 Job을 마스터 Job에서 실행 가능한 하나의 단계(Step)로 감쌉니다. */
    @Bean public Step preProcessingStep() { return new StepBuilder("preProcessingStep", jobRepository).job(preProcessingJob).build(); }

    /** 💡 담보 최적화 Job을 마스터 Job에서 실행 가능한 하나의 단계(Step)로 감쌉니다. */
    @Bean public Step collateralOptimizationStep() { return new StepBuilder("collateralOptimizationStep", jobRepository).job(collateralOptimizationJob).build(); }

    /** 💡 리스크 스테이징 Job을 마스터 Job에서 실행 가능한 하나의 단계(Step)로 감쌉니다. */
    @Bean public Step riskStagingStep() { return new StepBuilder("riskStagingStep", jobRepository).job(riskStagingJob).build(); }

    /** 💡 EAD/LGD 산출 Job을 마스터 Job에서 실행 가능한 하나의 단계(Step)로 감쌉니다. */
    @Bean public Step exposureLgdStep() { return new StepBuilder("exposureLgdStep", jobRepository).job(exposureLgdJob).build(); }

    /** 💡 최종 산출 및 리포팅 Job을 마스터 Job에서 실행 가능한 하나의 단계(Step)로 감쌉니다. */
    @Bean public Step mainCalculationStep() { return new StepBuilder("mainCalculationStep", jobRepository).job(mainReportingJob).build(); }

}
