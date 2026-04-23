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
 * <h3>?ì¥ ?”ì•¡ ?¬ì§‘ê³?ë°°ì¹˜ (Balance Re-aggregation Batch)</h3>
 * <p>
 * ê³¼ê±°???„í‘œ ?°ì´?°ê? ?˜ì •?˜ê±°?? ?„ë½???„í‘œê°€ ?¬í›„???…ë ¥?˜ì—ˆ????
 * ?¹ì • ê¸°ê°„???ì¥ ?”ì•¡??ì²˜ìŒë¶€???¤ì‹œ ê³„ì‚°?˜ì—¬ ?•í•©?±ì„ ë§ì¶¥?ˆë‹¤.
 * </p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BalanceReaggregationBatchConfig {

    private final LedgerService ledgerService;

    /**
     * ?„ì¼???”ì•¡???¬ì§‘ê³„í•˜??ë°°ì¹˜ ??
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
                    // ê¸°ë³¸?ìœ¼ë¡??´ì œ ? ì§œ???°ì´?°ë? ?¬ì§‘ê³?
                    LocalDate yesterday = LocalDate.now().minusDays(1);
                    log.info("Starting balance re-aggregation for: {}", yesterday);
                    
                    ledgerService.reaggregateLedgerBalancesForPeriod(yesterday, yesterday);
                    
                    log.info("Balance re-aggregation completed successfully.");
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
