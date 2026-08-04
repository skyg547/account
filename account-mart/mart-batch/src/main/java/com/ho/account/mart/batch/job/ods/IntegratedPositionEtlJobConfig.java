package com.ho.account.mart.batch.job.ods;

import com.ho.account.mart.batch.config.MartBatchExecutionConfig;
import com.ho.account.mart.batch.tasklet.AllowanceExposureSnapshotTasklet;
import com.ho.account.mart.batch.tasklet.CdmEventPublishTasklet;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaOdsAccountLedgerRepository;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaOdsCollateralMstRepository;
import com.ho.account.mart.batch.tasklet.OdsReconcileTasklet;
import com.ho.account.mart.batch.processor.CollateralDataQualityItemProcessor;
import com.ho.account.mart.batch.processor.IntegratedPositionItemProcessor;
import com.ho.account.mart.batch.processor.LedgerDataQualityItemProcessor;
import com.ho.account.mart.core.application.port.out.OdsDqAuditRepository;
import com.ho.account.mart.core.domain.mart.AllowanceInputPosition;
import com.ho.account.mart.core.domain.ods.audit.OdsDqAudit;
import com.ho.account.mart.core.domain.ods.loan.OdsAccountLedger;
import com.ho.account.mart.core.domain.ods.loan.OdsCollateralMst;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Objects;

/**
 * [CDM] 통합 대손충당금 입력 포지션 마트(CDM) 적재 배치 설정.
 * 💡 [금융 전문가 가이드] CDM(Common Data Model)은 신용, 금리, 유동성 등 각기 다른 결산 대손 엔진이 
 *    공통으로 사용할 수 있는 표준화된 데이터 구조입니다. 이 단계에서 데이터의 정합성을 
 *    확보하지 못하면 모든 대손충당금(IFRS9) 산출 결과가 왜곡될 수 있습니다.
 */
@Configuration
@RequiredArgsConstructor
public class IntegratedPositionEtlJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final OdsDqAuditRepository odsDqAuditRepository;
    private final com.ho.account.mart.batch.tasklet.BatchPreProcessTasklet preProcessTasklet;
    private final OdsReconcileTasklet reconcileTasklet;
    private final CdmEventPublishTasklet cdmEventPublishTasklet;
    private final AllowanceExposureSnapshotTasklet allowanceExposureSnapshotTasklet;
    private final IntegratedPositionItemProcessor integratedPositionItemProcessor;
    private final LedgerDataQualityItemProcessor ledgerDataQualityItemProcessor;
    private final CollateralDataQualityItemProcessor collateralDataQualityItemProcessor;

    @Qualifier("martBatchTaskExecutor")
    private final TaskExecutor martBatchTaskExecutor;
    @Value("${mart.batch.cdm-load.parallel-enabled:false}")
    private boolean parallelCdmLoadEnabled;

    @Bean
    public Job integratedPositionEtlJob() {
        return new JobBuilder("integratedPositionEtlJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(Objects.requireNonNull(preProcessStep()))
                .next(Objects.requireNonNull(ledgerDataQualityStep()))
                .next(Objects.requireNonNull(collateralDataQualityStep()))
                .next(Objects.requireNonNull(odsReconcileStep()))
                .next(Objects.requireNonNull(cdmLoadStep()))
                .next(Objects.requireNonNull(allowanceExposureSnapshotStep()))
                .next(cdmEventPublishStep()) // [v7.0 추가] EDA 알림 발행
                .build();
    }

    @Bean
    public Step cdmEventPublishStep() {
        return new StepBuilder("cdmEventPublishStep", jobRepository)
                .tasklet(cdmEventPublishTasklet, transactionManager)
                .build();
    }

    // --- 개별 단계별 재수행용 단독 잡(Job) 정의 ---

    @Bean
    public Job preProcessJob() {
        return new JobBuilder("preProcessJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(Objects.requireNonNull(preProcessStep()))
                .build();
    }

    @Bean
    public Job ledgerDqJob() {
        return new JobBuilder("ledgerDqJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(Objects.requireNonNull(ledgerDataQualityStep()))
                .build();
    }

    @Bean
    public Job collateralDqJob() {
        return new JobBuilder("collateralDqJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(Objects.requireNonNull(collateralDataQualityStep()))
                .build();
    }

    @Bean
    public Job odsReconcileJob() {
        return new JobBuilder("odsReconcileJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(Objects.requireNonNull(ledgerDataQualityStep()))
                .next(Objects.requireNonNull(collateralDataQualityStep()))
                .next(Objects.requireNonNull(odsReconcileStep()))
                .build();
    }

    @Bean
    public Job cdmLoadJob() {
        return new JobBuilder("cdmLoadJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(Objects.requireNonNull(cdmLoadStep()))
                .build();
    }

    @Bean
    public Job cdmEventPublishJob() {
        return new JobBuilder("cdmEventPublishJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(cdmEventPublishStep())
                .build();
    }

    @Bean
    public Job allowanceExposureSnapshotJob() {
        return new JobBuilder("allowanceExposureSnapshotJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(Objects.requireNonNull(allowanceExposureSnapshotStep()))
                .build();
    }

    @Bean
    public Step odsReconcileStep() {
        return new StepBuilder("odsReconcileStep", Objects.requireNonNull(jobRepository))
                .tasklet(Objects.requireNonNull(reconcileTasklet), Objects.requireNonNull(transactionManager))
                .build();
    }

    @Bean
    public Step ledgerDataQualityStep() {
        return new StepBuilder("ledgerDataQualityStep", Objects.requireNonNull(jobRepository))
                .<OdsAccountLedger, OdsDqAudit>chunk(MartBatchExecutionConfig.DQ_CHUNK_SIZE, Objects.requireNonNull(transactionManager))
                .reader(Objects.requireNonNull(ledgerDataQualityReader()))
                .processor(ledgerDataQualityItemProcessor)
                .writer(Objects.requireNonNull(dqAuditWriter()))
                .faultTolerant() // 데이터 1건의 오류가 전체 배치를 멈추지 않도록 설정
                .skip(DataAccessException.class)
                .skipLimit(100) // 최대 100건까지는 오류 데이터를 무시하고 다음으로 진행
                .build();
    }

    @Bean
    public Step collateralDataQualityStep() {
        return new StepBuilder("collateralDataQualityStep", Objects.requireNonNull(jobRepository))
                .<OdsCollateralMst, OdsDqAudit>chunk(MartBatchExecutionConfig.DQ_CHUNK_SIZE, Objects.requireNonNull(transactionManager))
                .reader(Objects.requireNonNull(collateralDataQualityReader()))
                .processor(collateralDataQualityItemProcessor)
                .writer(Objects.requireNonNull(dqAuditWriter()))
                .faultTolerant()
                .skip(DataAccessException.class)
                .skipLimit(100)
                .build();
    }

    @Bean
    public Step allowanceExposureSnapshotStep() {
        return new StepBuilder("allowanceExposureSnapshotStep", Objects.requireNonNull(jobRepository))
                .tasklet(Objects.requireNonNull(allowanceExposureSnapshotTasklet), Objects.requireNonNull(transactionManager))
                .build();
    }

    @Bean
    public Step preProcessStep() {
        return new StepBuilder("preProcessStep", Objects.requireNonNull(jobRepository))
                .tasklet(Objects.requireNonNull(preProcessTasklet), Objects.requireNonNull(transactionManager))
                .build();
    }

    /**
     * [고도화] CDM 로드 Step
     * 💡 [기술 팁] JpaPagingItemReader와 TaskExecutor를 결합하여 멀티스레드 기반의 
     *    초고속 적재를 수행합니다. 데이터 변환 중 발생하는 예외는 skip 처리하여 전체 배치의 안정성을 확보합니다.
     */
    @Bean
    public Step cdmLoadStep() {
        StepBuilder builder = new StepBuilder("cdmLoadStep", Objects.requireNonNull(jobRepository));
        var chunkStep = builder
                .<OdsAccountLedger, AllowanceInputPosition>chunk(MartBatchExecutionConfig.DEFAULT_CHUNK_SIZE, Objects.requireNonNull(transactionManager))
                .reader(Objects.requireNonNull(odsLedgerReader()))
                .processor(integratedPositionItemProcessor)
                .writer(Objects.requireNonNull(cdmWriter()))
                .faultTolerant()
                .skip(DataAccessException.class)      // DB 정합성 오류 시 건너뜀
                .skip(IllegalArgumentException.class) // 비즈니스 로직 불일치 시 건너뜀
                .skipLimit(100);                     // DQ 임계치 100건 설정

        if (parallelCdmLoadEnabled) {
            chunkStep.taskExecutor(martBatchTaskExecutor);
        }

        return chunkStep.build();
    }

    @Bean
    public SynchronizedItemStreamReader<OdsAccountLedger> odsLedgerReader() {
        return new SynchronizedItemStreamReaderBuilder<OdsAccountLedger>()
                .delegate(rawOdsLedgerReader())
                .build();
    }

    @Bean
    public JpaPagingItemReader<OdsAccountLedger> rawOdsLedgerReader() {
        return new JpaPagingItemReaderBuilder<OdsAccountLedger>()
                .name("rawOdsLedgerReader")
                .entityManagerFactory(Objects.requireNonNull(entityManagerFactory))
                .queryString(JpaOdsAccountLedgerRepository.QUERY_FOR_CDM_LOAD)
                .pageSize(MartBatchExecutionConfig.DEFAULT_CHUNK_SIZE)
                .saveState(false)
                .build();
    }

    @Bean
    public JpaItemWriter<AllowanceInputPosition> cdmWriter() {
        return new JpaItemWriterBuilder<AllowanceInputPosition>()
                .entityManagerFactory(Objects.requireNonNull(entityManagerFactory))
                .build();
    }

    @Bean
    public JpaPagingItemReader<OdsAccountLedger> ledgerDataQualityReader() {
        return new JpaPagingItemReaderBuilder<OdsAccountLedger>()
                .name("ledgerDataQualityReader")
                .entityManagerFactory(Objects.requireNonNull(entityManagerFactory))
                .queryString(JpaOdsAccountLedgerRepository.QUERY_FOR_DQ_CHECK)
                .pageSize(MartBatchExecutionConfig.DQ_CHUNK_SIZE)
                .build();
    }

    @Bean
    public JpaPagingItemReader<OdsCollateralMst> collateralDataQualityReader() {
        return new JpaPagingItemReaderBuilder<OdsCollateralMst>()
                .name("collateralDataQualityReader")
                .entityManagerFactory(Objects.requireNonNull(entityManagerFactory))
                .queryString(JpaOdsCollateralMstRepository.QUERY_FOR_DQ_CHECK)
                .pageSize(MartBatchExecutionConfig.DQ_CHUNK_SIZE)
                .build();
    }

    @Bean
    public ItemWriter<OdsDqAudit> dqAuditWriter() {
        return chunk -> odsDqAuditRepository.saveAll(new java.util.ArrayList<>(chunk.getItems()));
    }
}


