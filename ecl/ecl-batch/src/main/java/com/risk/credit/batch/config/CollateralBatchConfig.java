package com.risk.credit.batch.config;

import com.risk.credit.batch.support.CacheWarmingTasklet;
import com.risk.credit.batch.support.ColumnRangePartitioner;
import com.risk.credit.batch.support.QuerydslPagingItemReader;
import com.risk.credit.core.application.service.crm.ApartmentCollateralService;
import com.risk.credit.core.application.service.crm.CollateralAllocationService;
import com.risk.credit.core.domain.exposure.CrCustomer;
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
import org.springframework.core.task.TaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * [Phase 2] 담보 배분 최적화 및 가치평가 배치 (CRM Phase 1)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 단계는 수백만 명의 고객에 대해 각자의 대출과 담보를 최적으로 매칭합니다.
 * 대규모 데이터를 빠르게 처리하기 위해 '파티셔닝(Partitioning)' 기술을 사용하여
 * 여러 명의 일꾼이 동시에 담보 배분 연산을 수행하도록 설계되었습니다.
 * 
 * 💡 [담보 평가 및 LGD 개념]
 * 담보 평가(Collateral Evaluation)는 담보물의 현재 가치를 산정하는 과정이며,
 * LGD(Loss Given Default)는 부도 발생 시 담보를 통해 회수하지 못하는 손실률을 의미합니다.
 * 
 * 💡 [Chunk Processing]
 * 데이터를 한 번에 처리하지 않고 100건씩 묶어서(Chunk) 처리함으로써,
 * 메모리 부담을 줄이고 트랜잭션 단위로 안정적인 데이터 저장을 보장합니다.
 */
@Configuration
@RequiredArgsConstructor
public class CollateralBatchConfig {

    /** 💡 [초보자 가이드] 배치의 실행 상태와 이력을 관리하는 저장소입니다. */
    private final JobRepository jobRepository;
    
    /** 💡 [초보자 가이드] 데이터 처리 중 오류 발생 시 롤백 등을 관리하는 트랜잭션 매니저입니다. */
    private final PlatformTransactionManager transactionManager;

    /** 💡 [초보자 가이드] 고객별 담보 배분 로직을 수행하는 서비스입니다. */
    private final CollateralAllocationService collateralAllocationService;
    
    /** 💡 [초보자 가이드] 아파트 담보 가치 평가 로직을 수행하는 서비스입니다. */
    private final ApartmentCollateralService apartmentCollateralService;
    
    /** 💡 [초보자 가이드] 배치 시작 전 필요한 데이터를 미리 메모리에 올려두는 작업입니다. */
    private final CacheWarmingTasklet cacheWarmingTasklet;

    // Infrastructure Beans (인프라 설정에서 주입받음)
    /** 💡 [초보자 가이드] 병렬 처리를 위해 여러 스레드를 관리하는 실행기입니다. */
    private final TaskExecutor creditRiskTaskExecutor;
    
    /** 💡 [초보자 가이드] 전체 데이터를 특정 범위로 나누어 워커들에게 분배하는 파티셔너입니다. */
    private final ColumnRangePartitioner partitioner;
    
    /** 💡 [초보자 가이드] DB에서 고객 데이터를 페이징 방식으로 읽어오는 도구입니다. */
    private final QuerydslPagingItemReader<CrCustomer> pagingCustomerReader;

    /**
     * Phase 2 마스터 Job: 병렬화된 담보 배분 단계와 후속 가치평가 단계를 실행합니다.
     */
    @Bean
    public Job collateralOptimizationJob() {
        return new JobBuilder("collateralOptimizationJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(collateralWarmingStep())           // 0. 캐시 워밍업 (고도화 추가)
                .next(collateralAllocationManagerStep()) // 1. 병렬 담보 배분 (Master)
                .next(apartmentCollateralManagerStep())   // 2. 병렬 아파트/부동산 특화 평가 (Master)
                .build();
    }

    /**
     * [Collateral Warming Step] 담보 산산 전 기저 데이터 캐시 로드
     */
    @Bean
    public Step collateralWarmingStep() {
        return new StepBuilder("collateralWarmingStep", jobRepository)
                .tasklet(cacheWarmingTasklet, transactionManager)
                .build();
    }

    /**
     * [Collateral Allocation Manager Step] 담보 배분 마스터 단계
     * 전체 고객 명단을 여러 구역으로 나누어 각 워커(Worker)들에게 할당합니다.
     */
    @Bean
    public Step collateralAllocationManagerStep() {
        return new StepBuilder("collateralAllocationManagerStep", jobRepository)
                .partitioner("collateralAllocationWorkerStep", partitioner)
                .step(collateralAllocationWorkerStep())
                .gridSize(4) // 4개 스레드로 병렬 처리
                .taskExecutor(creditRiskTaskExecutor)
                .build();
    }

    /**
     * [Collateral Allocation Worker Step] 담보 배분 워커 단계 (Chunk 방식)
     * 실제 최적화 연산을 수행합니다. 고객 단위로 읽어와서 연산을 실행한 뒤 커밋합니다.
     */
    @Bean
    public Step collateralAllocationWorkerStep() {
        return new StepBuilder("collateralAllocationWorkerStep", jobRepository)
                .<CrCustomer, CrCustomer>chunk(100, transactionManager) // 100명 단위로 트랜잭션 커밋
                .reader(pagingCustomerReader)
                .writer(chunk -> {
                    // ItemProcessor 대신 Writer에서 직접 서비스 호출 (간결함 유지)
                    chunk.getItems().forEach(customer -> 
                        collateralAllocationService.allocateCollateralsForCustomer(customer.getId())
                    );
                })
                .build();
    }

    /**
     * [Apartment Evaluation Manager Step] 아파트 담보 최적화 마스터 단계
     * 부동산 담보 가치를 재산정하는 스텝을 병렬로 분할하여 실행합니다.
     */
    @Bean
    public Step apartmentCollateralManagerStep() {
        return new StepBuilder("apartmentCollateralManagerStep", jobRepository)
                .partitioner("apartmentCollateralWorkerStep", partitioner)
                .step(apartmentCollateralWorkerStep())
                .gridSize(4)
                .taskExecutor(creditRiskTaskExecutor)
                .build();
    }

    /**
     * [Apartment Evaluation Worker Step] 아파트 담보 최적화 워커 단계 (Chunk 방식)
     */
    @Bean
    public Step apartmentCollateralWorkerStep() {
        return new StepBuilder("apartmentCollateralWorkerStep", jobRepository)
                .<CrCustomer, CrCustomer>chunk(100, transactionManager)
                .reader(pagingCustomerReader)
                .writer(chunk -> {
                    chunk.getItems().forEach(customer -> 
                        apartmentCollateralService.processApartmentCollateralsForCustomer(customer.getId())
                    );
                })
                .build();
    }
}
