package com.ho.account.ecl.batch.config;

import com.ho.account.ecl.batch.processor.EclProcessor;
import com.ho.account.ecl.batch.job.tasklet.AllowanceSummaryTasklet;
import com.ho.account.ecl.batch.support.CacheWarmingTasklet;
import com.ho.account.ecl.batch.support.ColumnRangePartitioner;
import com.ho.account.ecl.batch.support.QuerydslPagingItemReader;
import com.ho.account.ecl.core.domain.result.AllowanceEclResult;
import com.ho.account.ecl.core.application.port.out.AllowanceEclResultRepository;
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
 * [Phase 5] IFRS 9 ECL 본산출 및 회계 summary 생성 배치.
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 단계는 앞에서 확정한 PD, EAD, LGD를 미래전망 시나리오와 결합해
 * 결산에 필요한 기대신용손실(ECL)을 산출하고 `allowance_summary`를 재생성합니다.
 */
@Configuration
@RequiredArgsConstructor
public class MainReportingBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final EclProcessor eclProcessor;
    private final AllowanceEclResultRepository allowanceResultRepository;
    private final CacheWarmingTasklet cacheWarmingTasklet;
    private final AllowanceSummaryTasklet allowanceSummaryTasklet;

    // Infrastructure Beans (병렬 처리 인프라)
    private final TaskExecutor allowanceTaskExecutor;
    private final ColumnRangePartitioner partitioner;
    private final QuerydslPagingItemReader<AllowanceEclResult> pagingResultReader;

    /**
     * Phase 5 Job: 미래전망 ECL 본산출을 수행합니다.
     */
    @Bean
    public Job mainReportingJob() {
        return new JobBuilder("mainReportingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(reportingWarmingStep())         // 0. 캐시 워밍업
                .next(eclManagerStep())               // 1. 기대손실(ECL) 산출
                .next(allowanceSummaryStep())          // 2. 회계 대손충당금 summary 생성
                .build();
    }

    /**
     * [Reporting Warming Step] 본산출 전 기저 데이터 캐시 로드
     */
    @Bean
    public Step reportingWarmingStep() {
        return new StepBuilder("reportingWarmingStep", jobRepository)
                .tasklet(cacheWarmingTasklet, transactionManager)
                .build();
    }

    /**
     * [ECL Manager Step] 기대손실(Expected Credit Loss) 산출 단계
     * 
     * 💡 [초보자를 위한 개념 설명]
     * 공식: ECL = PD(망할 확률) * EAD(망했을 때 빌린돈) * LGD(망했을 때 못받는 비율)
     * 이 단계에서는 수백만 건의 계좌를 4개의 채널(Step)로 나누어 동시에 계산기를 두드립니다 (병렬 처리).
     */
    @Bean
    public Step eclManagerStep() {
        return new StepBuilder("eclManagerStep", jobRepository)
                .partitioner("eclWorkerStep", partitioner) // ID 범위별로 구역 나누기
                .step(eclWorkerStep())
                .gridSize(4)
                .taskExecutor(allowanceTaskExecutor)       // 비동기 스레드 풀 사용
                .build();
    }

    /**
     * [ECL Worker Step] 실제 ECL 연산 워커
     */
    @Bean
    public Step eclWorkerStep() {
        return new StepBuilder("eclWorkerStep", jobRepository)
                .<AllowanceEclResult, AllowanceEclResult>chunk(200, transactionManager) // 200건마다 DB에 커밋
                .reader(pagingResultReader)
                .processor(eclProcessor)
                .writer(chunk -> allowanceResultRepository.saveAll(new ArrayList<AllowanceEclResult>(chunk.getItems())))
                .build();
    }

    @Bean
    public Step allowanceSummaryStep() {
        return new StepBuilder("allowanceSummaryStep", jobRepository)
                .tasklet(allowanceSummaryTasklet, transactionManager)
                .build();
    }
}

