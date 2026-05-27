package com.ho.account.mart.batch.job;

import com.ho.account.shared.finance.entity.IntegratedRiskPosition;
import com.ho.account.mart.batch.config.MartBatchExecutionConfig;
import com.ho.account.mart.batch.tasklet.BatchPreProcessTasklet;
import com.ho.account.mart.batch.tasklet.BatchReconcileTasklet;
import com.ho.account.mart.core.domain.mart.processor.IntegratedPositionProcessor;
import com.ho.account.mart.core.domain.ods.loan.OdsAccountLedger;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Objects;

/**
 * [배치 설정] 통합 리스크 데이터 마트 ETL(Extract, Transform, Load) 배치 설정
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final IntegratedPositionProcessor processor;
    private final BatchPreProcessTasklet preProcessTasklet;
    private final BatchReconcileTasklet reconcileTasklet;
    @Qualifier("martBatchTaskExecutor")
    private final TaskExecutor martBatchTaskExecutor;

    /**
     * [CDM] 통합 재무 마트 ETL 전체 공정 (Job)
     * 💡 [초보자를 위한 개념 설명]
     * 이 전체 공정은 원천 데이터를 가져와서(Extract), 정해진 리스크 포맷으로 바꾸고(Transform),
     * 최종적으로 우리 재무 마트에 넣는(Load) 전 과정을 관리합니다.
     */
    @Bean
    public Job integratedRiskEtlJob() {
        return new JobBuilder("integratedRiskEtlJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(Objects.requireNonNull(martPreProcessStep()))
                .next(Objects.requireNonNull(martEtlStep()))
                .next(Objects.requireNonNull(martReconcileStep()))
                .build();
    }

    // --- 개별 단계별 재수행용 단독 잡(Job) 정의 ---

    @Bean
    public Job martPreProcessJob() {
        return new JobBuilder("martPreProcessJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(Objects.requireNonNull(martPreProcessStep()))
                .build();
    }

    @Bean
    public Job martEtlJob() {
        return new JobBuilder("martEtlJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(Objects.requireNonNull(martEtlStep()))
                .build();
    }

    @Bean
    public Job martReconcileJob() {
        return new JobBuilder("martReconcileJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(Objects.requireNonNull(martReconcileStep()))
                .build();
    }

    @Bean
    public Step martEtlStep() {
        StepBuilder builder = new StepBuilder("martEtlStep", Objects.requireNonNull(jobRepository));
        return builder.<OdsAccountLedger, IntegratedRiskPosition>chunk(MartBatchExecutionConfig.DEFAULT_CHUNK_SIZE, Objects.requireNonNull(transactionManager))
                .reader(Objects.requireNonNull(odsReader()))
                .processor(Objects.requireNonNull(processor))
                .writer(Objects.requireNonNull(martWriter()))
                .faultTolerant()
                .skip(DataAccessException.class)
                .skip(IllegalArgumentException.class)
                .skipLimit(100)
                .taskExecutor(martBatchTaskExecutor)
                .build();
    }

    @Bean
    public SynchronizedItemStreamReader<OdsAccountLedger> odsReader() {
        return new SynchronizedItemStreamReaderBuilder<OdsAccountLedger>()
                .delegate(rawOdsReader())
                .build();
    }

    @Bean
    public JpaPagingItemReader<OdsAccountLedger> rawOdsReader() {
        JpaPagingItemReaderBuilder<OdsAccountLedger> builder = new JpaPagingItemReaderBuilder<>();
        return builder.name("rawOdsReader")
                .entityManagerFactory(Objects.requireNonNull(entityManagerFactory))
                .queryString("SELECT a FROM OdsAccountLedger a WHERE a.isActive = true ORDER BY a.accountNo")
                .pageSize(MartBatchExecutionConfig.DEFAULT_CHUNK_SIZE)
                .build();
    }

    @Bean
    public JpaItemWriter<IntegratedRiskPosition> martWriter() {
        JpaItemWriterBuilder<IntegratedRiskPosition> builder = new JpaItemWriterBuilder<>();
        return builder.entityManagerFactory(Objects.requireNonNull(entityManagerFactory))
                .build();
    }

    @Bean
    public Step martReconcileStep() {
        StepBuilder builder = new StepBuilder("martReconcileStep", Objects.requireNonNull(jobRepository));
        return builder.tasklet(Objects.requireNonNull(reconcileTasklet), Objects.requireNonNull(transactionManager))
                .build();
    }

    @Bean
    public Step martPreProcessStep() {
        StepBuilder builder = new StepBuilder("martPreProcessStep", Objects.requireNonNull(jobRepository));
        return builder.tasklet(Objects.requireNonNull(preProcessTasklet), Objects.requireNonNull(transactionManager))
                .build();
    }
}
