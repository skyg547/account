package com.ho.account.journalledger.batch.config;

import com.ho.account.journalledger.batch.tasklet.BalanceReaggregationTasklet;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * GL/SL 잔액 재집계 Batch 설정.
 *
 * <p>초보자 설명: 재집계는 과거 기간의 POSTED 전표를 기준으로 잔액을 다시 만드는 작업이다.
 * 이 설정 클래스는 Job과 Step 연결만 담당하고, 실제 기간 해석은 Tasklet, 잔액 계산은 core LedgerService가 담당한다.
 */
@Configuration
@RequiredArgsConstructor
public class BalanceReaggregationBatchConfig {

    private final BalanceReaggregationTasklet balanceReaggregationTasklet;

    /**
     * 일별 또는 기간별 GL/SL 잔액을 재집계하는 Job.
     */
    @Bean
    public Job dailyBalanceReaggregationJob(JobRepository jobRepository, Step reaggregateStep) {
        return new JobBuilder("dailyBalanceReaggregationJob", jobRepository)
                .start(reaggregateStep)
                .build();
    }

    @Bean
    public Step reaggregateStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("reaggregateStep", jobRepository)
                .tasklet(balanceReaggregationTasklet, transactionManager)
                .build();
    }
}