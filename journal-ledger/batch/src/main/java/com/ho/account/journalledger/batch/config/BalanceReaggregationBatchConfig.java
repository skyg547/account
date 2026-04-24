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
 * <h3>?먯옣 ?붿븸 ?ъ쭛怨?諛곗튂 (Balance Re-aggregation Batch)</h3>
 * <p>
 * 怨쇨굅???꾪몴 ?곗씠?곌? ?섏젙?섍굅?? ?꾨씫???꾪몴媛 ?ы썑???낅젰?섏뿀????
 * ?뱀젙 湲곌컙???먯옣 ?붿븸??泥섏쓬遺???ㅼ떆 怨꾩궛?섏뿬 ?뺥빀?깆쓣 留욎땅?덈떎.
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BalanceReaggregationBatchConfig {

    private final LedgerService ledgerService;

    /**
     * ?꾩씪???붿븸???ъ쭛怨꾪븯??諛곗튂 ??
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
                    // 湲곕낯?곸쑝濡??댁젣 ?좎쭨???곗씠?곕? ?ъ쭛怨?
                    LocalDate yesterday = LocalDate.now().minusDays(1);
                    log.info("Starting balance re-aggregation for: {}", yesterday);
                    
                    ledgerService.reaggregateLedgerBalancesForPeriod(yesterday, yesterday);
                    
                    log.info("Balance re-aggregation completed successfully.");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
