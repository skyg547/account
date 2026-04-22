package com.ho.account.journalledger.batch.config;

import com.ho.account.journalledger.application.service.ledger.LedgerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;

/**
 * <h3>원장 잔액 재집계 배치 (Balance Re-aggregation Batch)</h3>
 * <p>
 * 과거의 전표 데이터가 수정되거나, 누락된 전표가 사후에 입력되었을 때
 * 특정 기간의 원장 잔액을 처음부터 다시 계산하여 정합성을 맞춥니다.
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BalanceReaggregationBatchConfig {

    private final LedgerService ledgerService;

    /**
     * 전일자 잔액을 재집계하는 배치 잡
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
                .tasklet((contribution, chunkContext) -> {
                    // 기본적으로 어제 날짜의 데이터를 재집계
                    LocalDate yesterday = LocalDate.now().minusDays(1);
                    log.info("Starting balance re-aggregation for: {}", yesterday);
                    
                    ledgerService.reaggregateLedgerBalancesForPeriod(yesterday, yesterday);
                    
                    log.info("Balance re-aggregation completed successfully.");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
