package com.ho.account.mart.batch.job.ods;

import com.ho.account.mart.core.application.port.out.OdsBehavioralHistoryRepository;
import com.ho.account.mart.core.domain.ods.loan.OdsBehavioralHistory;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.Objects;

/**
 * [ODS] 행동 모델링용 과거 이력 데이터 적재 배치 설정.
 * 
 * 💡 [금융 전문가 가이드] 
 * 이 배치는 NMD(핵심예금) 및 CPR(조기상환) 모델링의 '기초 체력'인 과거 데이터를 마트에 쌓습니다.
 * 과거 잔액 추이와 이벤트(상환/해지) 데이터를 축적해야만 통계적으로 유의미한 행동 모형을 산출할 수 있습니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class BehavioralHistoryLoadJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;

    @Bean
    public Job behavioralHistoryLoadJob() {
        return new JobBuilder("behavioralHistoryLoadJob", Objects.requireNonNull(jobRepository))
                .incrementer(new RunIdIncrementer())
                .start(behavioralHistoryLoadStep())
                .build();
    }

    @Bean
    public Step behavioralHistoryLoadStep() {
        return new StepBuilder("behavioralHistoryLoadStep", jobRepository)
                .<OdsBehavioralHistory, OdsBehavioralHistory>chunk(500, transactionManager)
                .reader(behavioralHistoryReader())
                .writer(behavioralHistoryWriter())
                .build();
    }

    /**
     * [Clean Batch Lifecycle Guide]
     * Spring Batch의 JpaPagingItemReader는 ApplicationContext 셧다운 시 Spring의 기본 destroyMethod 추론에 의해
     * unopened 상태에서 close()가 호출되는 예외(WARN)를 발생시킵니다.
     * @Bean(destroyMethod = "")으로 지정하여 Spring Container 차원의 자동 close 호출을 예방합니다.
     */
    @Bean(destroyMethod = "")
    public JpaPagingItemReader<OdsBehavioralHistory> behavioralHistoryReader() {
        return new JpaPagingItemReaderBuilder<OdsBehavioralHistory>()
                .name("behavioralHistoryReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT h FROM OdsLoanBehavioralHistory h")
                .pageSize(500)
                .build();
    }

    @Bean
    public JpaItemWriter<OdsBehavioralHistory> behavioralHistoryWriter() {
        return new JpaItemWriterBuilder<OdsBehavioralHistory>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}
