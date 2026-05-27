package com.risk.credit.batch.config;

import com.risk.credit.batch.processor.EclProcessor;
import com.risk.credit.batch.processor.RwaProcessor;
import com.risk.credit.batch.job.tasklet.AllowanceSummaryTasklet;
import com.risk.credit.batch.support.BatchParameterUtils;
import com.risk.credit.batch.support.CacheWarmingTasklet;
import com.risk.credit.batch.support.ColumnRangePartitioner;
import com.risk.credit.batch.support.QuerydslPagingItemReader;
import com.risk.credit.core.application.pipeline.MonthlyAssetConsolidationService;
import com.risk.credit.core.application.service.monitoring.ConcentrationRiskService;
import com.risk.credit.core.domain.result.CrRiskResult;
import com.risk.credit.core.application.port.out.CrRiskResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;

/**
 * [Phase 5] RWA/ECL 본산출 및 마감 리포팅 배치 (Final Calculation & Reporting)
 * 
 * 💡 [초보자를 위한 개념 설명]
 * 이 단계는 신용 리스크 산출의 '최종 결산' 과정입니다. 앞에서 구한 모든 파라미터(PD, EAD, LGD)를 
 * 하나로 합쳐서 은행의 운명을 결정짓는 두 가지 핵심 숫자를 뽑아냅니다.
 * 
 * 1. ECL (Expected Credit Loss): "우리가 앞으로 얼마나 손해를 볼까?"에 대한 대답입니다. 
 *    이 금액만큼을 '대손충당금'이라는 이름으로 통장에 따로 떼어놓아야 합니다.
 * 2. RWA (Risk Weighted Asset): "위험을 고려했을 때 우리 자산은 실제 얼마인가?"에 대한 대답입니다.
 *    이 숫자가 클수록 은행은 더 많은 '자기자본'을 보유해야 합니다 (BIS 비율의 분모가 됩니다).
 * 3. 마감 및 편중분석: 한 달치 성적표를 확정하고, 특정 업종이나 고객에게 위험이 너무 쏠려있지는 않은지 검사합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class MainReportingBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;

    private final EclProcessor eclProcessor;
    private final RwaProcessor rwaProcessor;
    private final MonthlyAssetConsolidationService consolidationService;
    private final ConcentrationRiskService concentrationRiskService;
    private final CrRiskResultRepository riskResultRepository;
    private final CacheWarmingTasklet cacheWarmingTasklet;
    private final AllowanceSummaryTasklet allowanceSummaryTasklet;

    // Infrastructure Beans (병렬 처리 인프라)
    private final TaskExecutor creditRiskTaskExecutor;
    private final ColumnRangePartitioner partitioner;
    private final QuerydslPagingItemReader<CrRiskResult> pagingResultReader;

    /**
     * Phase 5 마스터 Job: ECL/RWA 본산출 후 자산 마감 및 편중리스크 분석을 수행합니다.
     */
    @Bean
    public Job mainReportingJob() {
        return new JobBuilder("mainReportingJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(reportingWarmingStep())         // 0. 캐시 워밍업
                .next(eclManagerStep())               // 1. 기대손실(ECL) 산출
                .next(rwaManagerStep())                // 2. 위험가중자산(RWA) 산출
                .next(allowanceSummaryStep())          // 3. 회계 대손충당금 summary 생성
                .next(consolidationStep())             // 4. 월별 데이터 마감 처리 (RDM 적재)
                .next(concentrationAnalysisStep())     // 5. 리스크 편중도 분석
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
                .taskExecutor(creditRiskTaskExecutor)      // 비동기 스레드 풀 사용
                .build();
    }

    /**
     * [ECL Worker Step] 실제 ECL 연산 워커
     */
    @Bean
    public Step eclWorkerStep() {
        return new StepBuilder("eclWorkerStep", jobRepository)
                .<CrRiskResult, CrRiskResult>chunk(200, transactionManager) // 200건마다 DB에 커밋
                .reader(pagingResultReader)
                .processor(eclProcessor)
                .writer(chunk -> riskResultRepository.saveAll(new ArrayList<CrRiskResult>(chunk.getItems())))
                .build();
    }

    /**
     * [RWA Manager Step] 위험가중자산(Risk Weighted Asset) 산출 단계
     * 
     * 💡 [초보자를 위한 개념 설명]
     * 똑같은 1억 대출이라도 삼성전자에 빌려준 돈과 담보 없는 개인에게 빌려준 돈의 '위험 무게'는 다릅니다.
     * 바젤 III 규제 수식에 따라 이 '위험 가중치'를 적용하여 자산의 무게를 재조정하는 단계입니다.
     */
    @Bean
    public Step rwaManagerStep() {
        return new StepBuilder("rwaManagerStep", jobRepository)
                .partitioner("rwaWorkerStep", partitioner)
                .step(rwaWorkerStep())
                .gridSize(4)
                .taskExecutor(creditRiskTaskExecutor)
                .build();
    }

    /**
     * [RWA Worker Step] RWA 연산 워커
     */
    @Bean
    public Step rwaWorkerStep() {
        return new StepBuilder("rwaWorkerStep", jobRepository)
                .<CrRiskResult, CrRiskResult>chunk(200, transactionManager)
                .reader(pagingResultReader)
                .processor(rwaProcessor)
                .writer(chunk -> riskResultRepository.saveAll(new ArrayList<CrRiskResult>(chunk.getItems())))
                .build();
    }

    @Bean
    public Step allowanceSummaryStep() {
        return new StepBuilder("allowanceSummaryStep", jobRepository)
                .tasklet(allowanceSummaryTasklet, transactionManager)
                .build();
    }

    /**
     * [Consolidation Step] 월별 통합 마감 단계
     * 
     * 💡 [초보자를 위한 개념 설명]
     * 한 달 동안 고생해서 계산한 결과물들을 '박스에 담아 창고(RDM: Risk Data Mart)에 넣는' 과정입니다.
     * 이 데이터는 나중에 금융감독원 보고서나 경영진 보고용 지표로 활용됩니다.
     */
    @Bean
    public Step consolidationStep() {
        return new StepBuilder("consolidationStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    LocalDate baseDate = BatchParameterUtils.resolveBaseDate(contribution.getStepExecution());
                    consolidationService.consolidateMonthlyAssets(baseDate);
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    /**
     * [Concentration Analysis Step] 리스크 편중도 분석 단계
     * 
     * 💡 [초보자를 위한 개념 설명]
     * 계란을 한 바구니에 담았는지 검사하는 단계입니다. 
     * **HHI(Herfindahl-Hirschman Index)** 지수를 사용하는데, 이 숫자가 0에 가까우면 골고루 분산된 것이고, 
     * 1에 가까우면 특정 산업(예: 부동산)에 위험이 몰려있다는 뜻입니다.
     */
    @Bean
    public Step concentrationAnalysisStep() {
        return new StepBuilder("concentrationAnalysisStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    BigDecimal industryHhi = concentrationRiskService.calculateIndustryHhi();
                    BigDecimal counterpartyHhi = concentrationRiskService.calculateCounterpartyHhi();

                    log.info("✅ [리스크 편중도 분석 결과] 산업별 HHI 지수={} ({})",
                            industryHhi,
                            concentrationRiskService.getIndustryConcentrationStatus(industryHhi));
                    log.info("✅ [리스크 편중도 분석 결과] 거래상대방별 HHI 지수={} ({})",
                            counterpartyHhi,
                            concentrationRiskService.getCounterpartyConcentrationStatus(counterpartyHhi));
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }
}
