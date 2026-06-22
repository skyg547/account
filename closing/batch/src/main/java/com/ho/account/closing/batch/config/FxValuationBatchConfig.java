package com.ho.account.closing.batch.config;

import com.ho.account.closing.batch.service.FxValuationService;
import com.ho.account.closing.application.service.ClosingAccountingProperties;
import com.ho.account.journalledger.domain.ledger.domain.GlAccountBalance;
import com.ho.account.journalledger.domain.ledger.repository.GlAccountBalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.partition.support.Partitioner;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.batch.item.data.builder.RepositoryItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private final ClosingAccountingProperties accountingProperties;

    public static final String JOB_NAME = "fxValuationJob";
    private static final String STEP_NAME = "fxValuationStep";
    private static final String WORKER_STEP_NAME = "fxValuationWorkerStep";
    private static final int CHUNK_SIZE = 100;
    private static final int GRID_SIZE = 4;

    @Bean
    public Job fxValuationJob(Step fxValuationStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(fxValuationStep)
                .build();
    }

    @Bean
    public Step fxValuationStep(Partitioner fxValuationPartitioner, Step fxValuationWorkerStep) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .partitioner(WORKER_STEP_NAME, fxValuationPartitioner)
                .step(fxValuationWorkerStep)
                .gridSize(GRID_SIZE)
                .taskExecutor(fxValuationTaskExecutor())
                .build();
    }

    @Bean
    public Step fxValuationWorkerStep() {
        return new StepBuilder(WORKER_STEP_NAME, jobRepository)
                .<GlAccountBalance, GlAccountBalance>chunk(CHUNK_SIZE, transactionManager)
                .reader(fxValuationItemReader(null, null))
                .processor(fxValuationItemProcessor())
                .writer(fxValuationItemWriter(null, null))
                .build();
    }

    /**
     * [Partitioner] 평가 대상 계정코드를 나눕니다.
     * 초보자 가이드: Partition은 큰 작업을 계정코드 단위의 작은 작업 여러 개로 쪼개는 방식입니다.
     */
    @Bean
    @JobScope
    public Partitioner fxValuationPartitioner(
            @Value("#{jobParameters['valuationDate']}") String valuationDateStr) {
        return gridSize -> {
            LocalDate valuationDate = resolveValuationDate(valuationDateStr);
            String reportingCurrencyCode = accountingProperties.requireFxValuationReportingCurrencyCode();
            List<String> accountCodes = glAccountBalanceRepository
                    .findDistinctForeignCurrencyAccountCodes(reportingCurrencyCode, valuationDate);
            log.info("Found {} foreign currency accounts to evaluate on {}", accountCodes.size(), valuationDate);

            Map<String, ExecutionContext> partitions = new HashMap<>();
            if (accountCodes.isEmpty()) {
                ExecutionContext context = new ExecutionContext();
                context.putString("accountCode", "<none>");
                context.putString("valuationDate", valuationDate.toString());
                context.putString("reportingCurrencyCode", reportingCurrencyCode);
                partitions.put("fx-empty", context);
                return partitions;
            }

            for (int index = 0; index < accountCodes.size(); index++) {
                ExecutionContext context = new ExecutionContext();
                context.putString("accountCode", accountCodes.get(index));
                context.putString("valuationDate", valuationDate.toString());
                context.putString("reportingCurrencyCode", reportingCurrencyCode);
                partitions.put("fx-account-" + index, context);
            }
            return partitions;
        };
    }

    /**
     * [Reader] 파티션으로 받은 계정코드의 외화 잔액을 페이지 단위로 읽습니다.
     * 초보자 가이드: Paging Reader는 한 번에 전부 읽지 않고 100건씩 가져와 메모리 사용량과 재시작 지점을 안정화합니다.
     */
    @Bean
    @StepScope
    public RepositoryItemReader<GlAccountBalance> fxValuationItemReader(
            @Value("#{stepExecutionContext['accountCode']}") String accountCode,
            @Value("#{stepExecutionContext['valuationDate']}") String valuationDateStr) {
        LocalDate valuationDate = resolveValuationDate(valuationDateStr);
        String reportingCurrencyCode = accountingProperties.requireFxValuationReportingCurrencyCode();

        return new RepositoryItemReaderBuilder<GlAccountBalance>()
                .name("fxValuationItemReader-" + accountCode)
                .repository(glAccountBalanceRepository)
                .methodName("findLatestForeignCurrencyBalancesForAccount")
                .arguments(List.of(accountCode, reportingCurrencyCode, valuationDate))
                .pageSize(CHUNK_SIZE)
                .sorts(Map.of("id", Sort.Direction.ASC))
                .build();
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
            LocalDate valuationDate = resolveValuationDate(valuationDateStr);
            
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

    @Bean
    public TaskExecutor fxValuationTaskExecutor() {
        SimpleAsyncTaskExecutor taskExecutor = new SimpleAsyncTaskExecutor("fx-valuation-");
        taskExecutor.setConcurrencyLimit(GRID_SIZE);
        return taskExecutor;
    }

    private LocalDate resolveValuationDate(String valuationDateStr) {
        return (valuationDateStr != null && !valuationDateStr.isBlank())
                ? LocalDate.parse(valuationDateStr, DateTimeFormatter.ISO_DATE)
                : LocalDate.now();
    }

    private Long resolveBatchId(LocalDate valuationDate, Long valuationBatchId) {
        if (valuationBatchId != null) {
            return valuationBatchId;
        }
        return Long.parseLong(valuationDate.format(DateTimeFormatter.BASIC_ISO_DATE));
    }
}
