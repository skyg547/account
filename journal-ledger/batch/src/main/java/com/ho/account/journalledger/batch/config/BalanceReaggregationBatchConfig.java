package com.ho.account.journalledger.batch.config;

import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.batch.support.BatchDateRangeParameterUtils;
import com.ho.account.journalledger.batch.tasklet.BalanceCleanUpTasklet;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * GL/SL 원장 잔액 재집계 (Balance Re-aggregation) Batch 설정.
 *
 * <p><b>[아키텍처 및 상세 교육적 주석 (Pedagogical Comments)]</b></p>
 *
 * <h3>1. 왜 Tasklet에서 Chunk 기반 처리 구조로 리팩토링했는가?</h3>
 * <ul>
 *   <li><b>기존 Tasklet 방식의 문제점:</b> 단일 트랜잭션 내에서 지정 기간의 전체 전표 상세(JournalDetail)를 메모리에 한 번에 조회하고 집계했습니다.
 *       대용량 데이터(수십만~수백만 건 전표) 발생 시 <b>Out Of Memory (OOM)</b>, <b>DB Connection Timeout</b>,
 *       장애 발생 시 중단된 지점부터 <b>재시작(Restartability) 불가능</b>의 치명적 한계가 존재했습니다.</li>
 *   <li><b>Chunk 프로세싱의 이점:</b>
 *     <ul>
 *       <li><b>메모리 풋프린트(Memory Footprint) 관리:</b> Paging Reader({@link JpaPagingItemReader})를 사용해 설정된 Chunk Size(예: 100건)만큼만 메모리에 로딩하여 처리하므로 OOM 발생 위험을 원천적으로 차단합니다.</li>
 *       <li><b>트랜잭션 경계 (Transaction Boundaries):</b> Chunk 단위마다 독립된 트랜잭션을 commit하므로 DB 락 타임아웃을 방지하고 시스템 리소스를 안정적으로 유지합니다.</li>
 *       <li><b>재시작 가능성 (Restartability) & 멱등성 (Idempotency):</b> 사전 Clean-up Step을 통해 대상 기간의 잔액을 삭제/초기화한 후 Chunk 단위로 재집계를 수행하므로, 배치를 재실행해도 데이터 중복 누적(Side-effect) 없이 동일한 결과를 보장합니다.</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>2. 2-Step 파이프라인 구조 (Clean-up Step -> Reaggregation Chunk Step)</h3>
 * <ol>
 *   <li><b>Step 1: balanceCleanUpStep (Pre-processing Phase):</b>
 *       배치 재실행 및 중복 집계에 따른 멱등성 파괴를 막기 위해 지정된 대상 기간({@code startDate} ~ {@code endDate})의 기존 GL/SL 잔액을 먼저 지웁니다.</li>
 *   <li><b>Step 2: balanceReaggregationStep (Chunk Processing Phase):</b>
 *       대상 기간의 POSTED 상태 전표 상세 데이터를 Chunk 단위로 읽어서(Reader), 원장 서비스에 청크 단위로 집계/저장(Writer)을 위임합니다.</li>
 * </ol>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BalanceReaggregationBatchConfig {

    private static final int CHUNK_SIZE = 100;

    private final BalanceCleanUpTasklet balanceCleanUpTasklet;
    private final LedgerService ledgerService;
    private final EntityManagerFactory entityManagerFactory;

    /**
     * 일별 또는 기간별 GL/SL 잔액을 재집계하는 배치 Job.
     *
     * <p>Step 1 (Clean-up) 후 Step 2 (Chunk 기반 재집계)를 순차적으로 수행합니다.</p>
     */
    @Bean
    public Job dailyBalanceReaggregationJob(JobRepository jobRepository,
                                           Step balanceCleanUpStep,
                                           Step balanceReaggregationStep) {
        return new JobBuilder("dailyBalanceReaggregationJob", jobRepository)
                .start(balanceCleanUpStep)
                .next(balanceReaggregationStep)
                .build();
    }

    /**
     * Step 1: 멱등성 보장을 위한 대상 기간 사전 잔액 Clean-up Step.
     */
    @Bean
    public Step balanceCleanUpStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("balanceCleanUpStep", jobRepository)
                .tasklet(balanceCleanUpTasklet, transactionManager)
                .build();
    }

    /**
     * Step 2: 대용량 데이터 분할 처리를 위한 Chunk 기반 재집계 Step.
     */
    @Bean
    public Step balanceReaggregationStep(JobRepository jobRepository,
                                          PlatformTransactionManager transactionManager,
                                          JpaPagingItemReader<JournalDetail> balanceReaggregationItemReader,
                                          ItemProcessor<JournalDetail, JournalDetail> balanceReaggregationItemProcessor,
                                          ItemWriter<JournalDetail> balanceReaggregationItemWriter) {
        return new StepBuilder("balanceReaggregationStep", jobRepository)
                .<JournalDetail, JournalDetail>chunk(CHUNK_SIZE, transactionManager)
                .reader(balanceReaggregationItemReader)
                .processor(balanceReaggregationItemProcessor)
                .writer(balanceReaggregationItemWriter)
                .build();
    }

    /**
     * ItemReader: 지정 기간 내 승인 완료(POSTED)된 전표 상세 목록을 Chunk Size 단위로 Paging 조회.
     *
     * <p>정렬 순서: accountingDate ASC, je.id ASC, jd.id ASC<br>
     * 날짜 순서대로 읽어야 기말/기초 잔액(Beginning Balance) 연결이 정확하게 이루어집니다.</p>
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<JournalDetail> balanceReaggregationItemReader(
            @Value("#{stepExecution}") StepExecution stepExecution) {

        BatchDateRangeParameterUtils.DateRange range =
                BatchDateRangeParameterUtils.resolveDateRange(stepExecution);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("startDate", range.startDate());
        parameters.put("endDate", range.endDate());

        log.info("[Journal Ledger Batch Reader] Initializing JpaPagingItemReader. startDate={}, endDate={}, pageSize={}",
                range.startDate(), range.endDate(), CHUNK_SIZE);

        return new JpaPagingItemReaderBuilder<JournalDetail>()
                .name("balanceReaggregationItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT jd FROM JournalDetail jd " +
                             "JOIN FETCH jd.journalEntry je " +
                             "WHERE je.accountingDate BETWEEN :startDate AND :endDate " +
                             "AND je.status = 'POSTED' " +
                             "ORDER BY je.accountingDate ASC, je.id ASC, jd.id ASC")
                .parameterValues(parameters)
                .pageSize(CHUNK_SIZE)
                .build();
    }

    /**
     * ItemProcessor: 전표 상세 항목 투과 전달 (Pass-Through Processor).
     */
    @Bean
    public ItemProcessor<JournalDetail, JournalDetail> balanceReaggregationItemProcessor() {
        return item -> item;
    }

    /**
     * ItemWriter: Chunk 단위로 수집된 전표 상세 목록을 LedgerService에 전달하여 GL/SL 잔액에 반영.
     *
     * <p>각 Chunk 트랜잭션이 성공적으로 commit될 때마다 DB 잔액 상태가 업데이트되어 메모리를 효율적으로 사용합니다.</p>
     */
    @Bean
    public ItemWriter<JournalDetail> balanceReaggregationItemWriter() {
        return chunk -> {
            log.debug("[Journal Ledger Batch Writer] Writing chunk of {} details to ledger balances.", chunk.getItems().size());
            ledgerService.updateLedgerBalancesBulk(new ArrayList<>(chunk.getItems()));
        };
    }
}