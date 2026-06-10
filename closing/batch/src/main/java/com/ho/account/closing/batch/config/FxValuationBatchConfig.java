package com.ho.account.closing.batch.config;

import com.ho.account.closing.batch.service.FxValuationService;
import com.ho.account.journalledger.domain.ledger.domain.GlAccountBalance;
import com.ho.account.journalledger.domain.ledger.repository.GlAccountBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * [배치 처리 (Batch Processing) - 결산 외화 평가 (FX Valuation)]
 *
 * 🐣 [초보자를 위한 설명]
 * 결산 시점(보통 월말)에 실행되는 '외화 자산/부채 평가 자동화 공장'입니다.
 * 
 * 1. Reader (읽기): 원장(GL)에서 기준 통화(KRW)가 아닌 모든 '외화 잔액(USD, EUR 등)' 목록을 가져옵니다.
 * 2. Processor (가공): 대상 검증 및 로깅만 수행하고 실제 처리는 Writer에게 넘깁니다.
 * 3. Writer (쓰기): 기말 환율을 적용하여 기존 장부 금액과의 차액을 계산하고, "외화환산손익" 전표를 자동으로 생성합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class FxValuationBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final GlAccountBalanceRepository glAccountBalanceRepository;
    private final FxValuationService fxValuationService;

    public static final String JOB_NAME = "fxValuationJob";
    private static final String STEP_NAME = "fxValuationStep";
    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job fxValuationJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(fxValuationStep())
                .build();
    }

    @Bean
    @JobScope
    public Step fxValuationStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<GlAccountBalance, GlAccountBalance>chunk(CHUNK_SIZE, transactionManager)
                .reader(fxValuationItemReader(null))
                .processor(fxValuationItemProcessor())
                .writer(fxValuationItemWriter(null, null))
                .build();
    }

    /**
     * [Reader] 평가일 기준 최신 외화 잔액 조회
     * (현재는 통화별/계정별 집계 잔액을 읽기 때문에 ListItemReader를 사용합니다.)
     *
     * @todo 외화 잔액 계정 수가 대량으로 늘어나는 운영 환경에서는 Paging Reader와 Partition Step으로 전환해
     *       메모리 사용량과 재시작 지점을 명확히 분리해야 합니다.
     */
    @Bean
    @StepScope
    public ItemReader<GlAccountBalance> fxValuationItemReader(
            @Value("#{jobParameters['valuationDate']}") String valuationDateStr) {
        
        LocalDate valuationDate = (valuationDateStr != null) 
                ? LocalDate.parse(valuationDateStr, DateTimeFormatter.ISO_DATE) 
                : LocalDate.now();

        List<GlAccountBalance> foreignBalances = glAccountBalanceRepository.findLatestForeignCurrencyBalances("KRW", valuationDate);
        log.info("Found {} foreign currency balances to evaluate on {}", foreignBalances.size(), valuationDate);
        
        return new ListItemReader<>(foreignBalances);
    }

    @Bean
    @StepScope
    public ItemProcessor<GlAccountBalance, GlAccountBalance> fxValuationItemProcessor() {
        return balance -> {
            log.debug("Processing FX Valuation for Account: {}, Currency: {}, Balance: {}", 
                    balance.getAccountCode(), balance.getCurrencyCode(), balance.getEndingBalance());
            return balance;
        };
    }

    @Bean
    @StepScope
    public ItemWriter<GlAccountBalance> fxValuationItemWriter(
            @Value("#{jobParameters['valuationDate']}") String valuationDateStr,
            @Value("#{jobParameters['valuationBatchId']}") Long valuationBatchId) {
        return balances -> {
            LocalDate valuationDate = (valuationDateStr != null) 
                    ? LocalDate.parse(valuationDateStr, DateTimeFormatter.ISO_DATE) 
                    : LocalDate.now();
            
            Long batchId = resolveBatchId(valuationDate, valuationBatchId);

            for (GlAccountBalance balance : balances) {
                try {
                    fxValuationService.processFxValuationForAccount(balance, valuationDate, batchId);
                } catch (Exception e) {
                    log.error("Failed to process FX valuation for account {}: {}", balance.getAccountCode(), e.getMessage());
                    // 배치 특성상 에러 로깅 후 계속 진행 (Skip)
                }
            }
        };
    }

    private Long resolveBatchId(LocalDate valuationDate, Long valuationBatchId) {
        if (valuationBatchId != null) {
            return valuationBatchId;
        }
        return Long.parseLong(valuationDate.format(DateTimeFormatter.BASIC_ISO_DATE));
    }
}
