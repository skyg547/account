package com.risk.credit.batch.config;

import com.risk.credit.batch.processor.EadCrmProcessor;
import com.risk.credit.batch.support.CacheWarmingTasklet;
import com.risk.credit.batch.support.ColumnRangePartitioner;
import com.risk.credit.batch.support.QuerydslPagingItemReader;
import com.risk.credit.core.application.port.out.CrRiskResultRepository;
import com.risk.credit.core.domain.result.CrRiskResult;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;

/**
 * [Phase 4] 노출액(EAD) 및 리스크 완화(CRM/LGD) 산출 배치 (Exposure & Loss Phase)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 단계에서는 고객이 부도가 났을 때 실제로 은행이 얼마를 떼일지(위험액)를 계산합니다.
 * 1. EAD: 부도 시점에 고객이 빌려 쓰고 있을 총금액(노출액)을 추정합니다.
 * 2. CRM: 담보가 있다면 그만큼 위험이 줄어듭니다(리스크 완율).
 * 3. LGD: 부도가 나더라도 담보 처분 등을 통해 회수하지 못하고 최종적으로 손실을 볼 비율을 정합니다.
 * 
 * 💡 [멀티스레드 처리 가이드]
 * 데이터가 매우 많기 때문에, 전체 데이터를 여러 범위(Partition)로 나누어 
 * 여러 개의 스레드(Worker)가 동시에 계산을 수행하여 처리 속도를 극대화합니다.
 */
@Configuration
@RequiredArgsConstructor
public class ExposureLgdBatchConfig {

    /** 💡 [초보자 가이드] 배치의 실행 상태와 이력을 기록하는 핵심 저장소입니다. */
    private final JobRepository jobRepository;
    
    /** 💡 [초보자 가이드] 데이터베이스 작업 시 오류가 나면 원상복구(Rollback)를 담당하는 관리자입니다. */
    private final PlatformTransactionManager transactionManager;

    /** 💡 [초보자 가이드] EAD(노출액)와 CRM(담보효과)을 계산하는 핵심 비즈니스 로직입니다. */
    private final EadCrmProcessor eadCrmProcessor;
    
    /** 💡 [초보자 가이드] 최종 산출된 리스크 결과를 DB에 저장하는 저장소입니다. */
    private final CrRiskResultRepository riskResultRepository;
    
    /** 💡 [초보자 가이드] 배치 시작 전, 자주 쓰이는 데이터를 메모리에 미리 올려두는 작업입니다. */
    private final CacheWarmingTasklet cacheWarmingTasklet;

    // Infrastructure Beans (병렬 처리를 위한 동적 파티셔닝 빈)
    /** 💡 [초보자 가이드] 여러 작업을 동시에 실행하기 위한 스레드 풀(일꾼 그룹)입니다. */
    private final TaskExecutor creditRiskTaskExecutor;
    
    /** 💡 [초보자 가이드] 전체 데이터를 ID 범위별로 쪼개어 여러 워커에게 배분하는 분할기입니다. */
    private final ColumnRangePartitioner partitioner;
    
    /** 💡 [초보자 가이드] DB에서 리스크 산출 대상 데이터를 페이지 단위로 읽어오는 도구입니다. */
    private final QuerydslPagingItemReader<CrRiskResult> pagingResultReader;

    /**
     * Phase 4 마스터 Job: EAD와 LGD를 병렬로 산출합니다.
     * 💡 [초보자 가이드] 전체 배치의 흐름을 정의하며, 워밍업 후 메인 산출 단계를 실행합니다.
     */
    @Bean
    public Job exposureLgdJob() {
        return new JobBuilder("exposureLgdJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(exposureWarmingStep())          // 0. 캐시 워밍업 (고도화 추가)
                .next(eadCrmManagerStep())            // 1. EAD/LGD 병렬 산출 실행
                .build();
    }

    /**
     * [Exposure Warming Step] EAD/LGD 산출 전 기저 데이터 캐시 로드
     * 💡 [초보자 가이드] 계산 속도를 높이기 위해 필요한 기초 데이터를 메모리에 미리 로드합니다.
     */
    @Bean
    public Step exposureWarmingStep() {
        return new StepBuilder("exposureWarmingStep", jobRepository)
                .tasklet(cacheWarmingTasklet, transactionManager)
                .build();
    }

    /**
     * [EAD/CRM Manager Step] 산출 파티셔너 (Master Step)
     * 💡 [초보자 가이드] 데이터를 4개 구역으로 나누어 4개의 스레드가 동시에 일하도록 지시합니다.
     */
    @Bean
    public Step eadCrmManagerStep() {
        return new StepBuilder("eadCrmManagerStep", jobRepository)
                .partitioner("eadCrmWorkerStep", partitioner)
                .step(eadCrmWorkerStep())
                .gridSize(4)
                .taskExecutor(creditRiskTaskExecutor)
                .build();
    }

    /**
     * [EAD/CRM Worker Step] 실제 연산 로직 실행 (Worker Step)
     * 💡 [초보자 가이드] 200건씩 데이터를 가져와 EAD/LGD를 계산하고 DB에 저장합니다.
     */
    @Bean
    public Step eadCrmWorkerStep() {
        return new StepBuilder("eadCrmWorkerStep", jobRepository)
                .<CrRiskResult, CrRiskResult>chunk(200, transactionManager) // 200건 단위 처리
                .reader(pagingResultReader)                                  // 이전 단계 결과를 읽기
                .processor(eadCrmProcessor)                                  // 핵심 연산(EAD/LGD)
                .writer(chunk -> riskResultRepository.saveAll(new ArrayList<CrRiskResult>(chunk.getItems())))
                .build();
    }
}
