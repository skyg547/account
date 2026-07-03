package com.ho.account.ecl.batch.config;

import com.ho.account.ecl.batch.processor.StagingProcessor;
import com.ho.account.ecl.batch.support.BatchParameterUtils;
import com.ho.account.ecl.batch.support.CacheWarmingTasklet;
import com.ho.account.ecl.batch.support.ColumnRangePartitioner;
import com.ho.account.ecl.batch.support.QuerydslPagingItemReader;
import com.ho.account.ecl.core.application.port.out.AllowanceEclResultRepository;
import com.ho.account.ecl.core.application.service.allowance.AllowanceCalculationService;
import com.ho.account.ecl.core.domain.exposure.CrAccount;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
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

import java.time.LocalDate;
import java.util.ArrayList;

/**
 * [Phase 3] IFRS 9 스테이징 및 PD(부도율) 산출 배치.
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 대출 건별로 '건전성 등급(Stage)'을 매기는 단계입니다. 
 * 연체일수나 차주의 신용등급 변화를 보고 STAGE 1(정상), 2(자산건전성 하락), 3(부도)으로 분류하며,
 * 이 분류에 따라 나중에 대손충당금(ECL)이 달라집니다.
 * 이 과정에서 수백만 건의 대용량 데이터를 빠르게 처리하기 위해 '파티셔닝(Partitioning)' 기술을 사용합니다.
 */
@Configuration
@RequiredArgsConstructor
public class AllowanceStagingBatchConfig {

    /** 💡 [초보자 가이드] 배치의 전반적인 메타데이터를 관리하는 저장소입니다. */
    private final JobRepository jobRepository;
    
    /** 💡 [초보자 가이드] 데이터 작업의 일관성을 보장하는 관리자입니다. */
    private final PlatformTransactionManager transactionManager;

    /** 💡 [초보자 가이드] 대손충당금(IFRS9) 산출의 전반적인 흐름을 제어하는 비즈니스 서비스입니다. */
    private final AllowanceCalculationService allowanceCalculationService;
    
    /** 💡 [초보자 가이드] Spring Batch ItemProcessor 어댑터입니다. 실제 Stage/PD 판단은 core pipeline이 수행합니다. */
    private final StagingProcessor stagingProcessor;
    
    /** 💡 [초보자 가이드] 산출된 대손충당금 결과를 DB에 저장하는 통로입니다. */
    private final AllowanceEclResultRepository allowanceResultRepository;
    
    /** 💡 [초보자 가이드] 계산을 시작하기 전, 필요한 데이터를 미리 메모리에 올려두는(워밍업) 작업자입니다. */
    private final CacheWarmingTasklet cacheWarmingTasklet;

    // Infrastructure Beans (병렬 처리를 위한 인프라 빈 주입)
    /** 💡 [초보자 가이드] 여러 작업을 동시에 수행할 스레드 풀(일꾼들)입니다. */
    private final TaskExecutor allowanceTaskExecutor;
    
    /** 💡 [초보자 가이드] 수백만 건의 데이터를 어떻게 구역별로 쪼갤지 결정하는 분할기입니다. */
    private final ColumnRangePartitioner partitioner;
    
    /** 💡 [초보자 가이드] DB에서 계좌 데이터를 한 페이지씩 효율적으로 읽어오는 도구입니다. */
    private final QuerydslPagingItemReader<CrAccount> pagingAccountReader;

    /**
     * Phase 3 마스터 Job: 결과 적재 공간을 준비하고, 대량의 계좌를 병렬로 읽어 스테이징을 수행합니다.
     */
    @Bean
    public Job allowanceStagingJob() {
        return new JobBuilder("allowanceStagingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(stagingWarmingStep())           // 0. 캐시 워밍업 (DB 조회를 줄이기 위해 사전 준비)
                .next(resultPreparationStep())        // 1. 산출 결과 테이블(Result) 초기화
                .next(stagingManagerStep())            // 2. 계좌별 스테이징 산출 (병렬 처리 시작)
                .build();
    }

    /**
     * [Staging Warming Step] 스테이징 판정 전 기저 데이터 캐시 로드
     * 💡 [초보자 가이드] "미리 공부하기"와 같습니다. 등급별 PD 값 등을 미리 메모리에 올려두어 계산 속도를 올립니다.
     */
    @Bean
    public Step stagingWarmingStep() {
        return new StepBuilder("stagingWarmingStep", jobRepository)
                .tasklet(cacheWarmingTasklet, transactionManager)
                .build();
    }

    /**
     * [Preparation Step] 산출 결과 적재 공간 준비
     * 💡 [초보자 가이드] "책상 정리"와 같습니다. 이번에 새로 계산할 자리를 만들기 위해 기존 데이터를 치웁니다.
     */
    @Bean
    public Step resultPreparationStep() {
        return new StepBuilder("resultPreparationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());
                    // 💡 테이블 정리(Clean-up)를 확실히 합니다.
                    allowanceCalculationService.clearPreviousResults(baseDate);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * [Staging Manager Step] 스테이징 산출 파티셔너 (Master Step)
     * 💡 [초보자 가이드] "반장"과 같습니다. 수백만 건의 데이터를 일꾼(Worker)들에게 "너는 1~10만 번, 너는 11~20만 번" 식으로 일을 나눠줍니다.
     */
    @Bean
    public Step stagingManagerStep() {
        return new StepBuilder("stagingManagerStep", jobRepository)
                .partitioner("stagingWorkerStep", partitioner) // ID 범위 기반으로 구역 나누기
                .step(stagingWorkerStep())                     // 실제 일은 Worker가 수행
                .gridSize(4)                                   // 4개 구역으로 분할 처리 (일꾼 4명)
                .taskExecutor(allowanceTaskExecutor)           // 병렬 스레드풀 사용
                .build();
    }

    /**
     * [Staging Worker Step] 계좌 chunk를 core Stage/PD pipeline으로 전달하는 Worker Step
     * 💡 [초보자 가이드] "실제 일꾼"입니다. 200건씩 끊어서 읽고, batch adapter가 core pipeline에 계산을 맡긴 뒤 저장합니다.
     */
    @Bean
    public Step stagingWorkerStep() {
        return new StepBuilder("stagingWorkerStep", jobRepository)
                .<CrAccount, AllowanceEclResult>chunk(200, transactionManager) // 200건 단위 처리
                .reader(pagingAccountReader)                             // QueryDSL로 똑똑하게 읽기
                .processor(stagingProcessor)                             // batch adapter -> core StagingCalculationPipeline 위임
                .writer(chunk -> allowanceResultRepository.saveAll(new ArrayList<AllowanceEclResult>(chunk.getItems()))) // 계산 결과 저장
                .build();
    }
}

