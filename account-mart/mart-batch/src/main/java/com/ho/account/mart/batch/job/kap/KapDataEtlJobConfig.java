package com.ho.account.mart.batch.job.kap;

import com.ho.account.mart.batch.config.MartBatchExecutionConfig;
import com.ho.account.mart.core.infrastructure.persistence.entity.external.kap.KapExternalRatingEntity;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JpaItemWriter;
import org.springframework.batch.item.database.builder.JpaItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileParseException;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.support.SynchronizedItemStreamReader;
import org.springframework.batch.item.support.builder.SynchronizedItemStreamReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.task.TaskExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * [KAP] 한국자산평가(KAP) 데이터 수집 및 ETL 배치 설정 구현.
 * 💡 [금융 전문가 가이드] 외부 평가기관(KAP, NICE 등)의 데이터는 은행 내부 데이터와 포맷이 다르므로
 *    표준 규격(CDM)으로 정제하는 과정이 필수적입니다. 특히 부도율(PD) 산출 시 
 *    외부 등급과 내부 등급의 매핑 정합성을 보장해야 합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class KapDataEtlJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    @Qualifier("martBatchTaskExecutor")
    private final TaskExecutor martBatchTaskExecutor;

    @Bean
    public Job kapDataEtlJob(Step kapExternalRatingStep) {
        return new JobBuilder("kapDataEtlJob", jobRepository)
                .start(kapExternalRatingStep)
                .build();
    }

    /**
     * [Step] 외부 등급 데이터 적재 단계입니다.
     * 💡 [기술 팁] 대용량 CSV 파싱 시 발생할 수 있는 포맷 오류를 skip 처리하고, 
     *    멀티 스레드 환경에서 데이터 순서 보장을 위해 SynchronizedItemStreamReader를 사용합니다.
     */
    @Bean
    public Step kapExternalRatingStep(
            SynchronizedItemStreamReader<KapExternalRatingCsvRow> kapExternalRatingReader,
            KapExternalRatingProcessor kapExternalRatingProcessor,
            JpaItemWriter<KapExternalRatingEntity> kapExternalRatingWriter) {
        return new StepBuilder("kapExternalRatingStep", jobRepository)
                .<KapExternalRatingCsvRow, KapExternalRatingEntity>chunk(MartBatchExecutionConfig.DEFAULT_CHUNK_SIZE, transactionManager)
                .reader(kapExternalRatingReader)
                .processor(kapExternalRatingProcessor)
                .writer(kapExternalRatingWriter)
                .faultTolerant()
                .skip(FlatFileParseException.class) // 잘못된 포맷의 라인은 건너뜀
                .skip(DataAccessException.class)    // 일시적인 DB 오류 건너뜀
                .skipLimit(100)                    // 최대 100건까지 허용 (DQ 임계치)
                .taskExecutor(martBatchTaskExecutor)
                .build();
    }

    /**
     * [Reader] CSV 파일을 읽어와 객체로 매핑합니다.
     * 💡 [Clean Batch Lifecycle Guide]
     * @Bean(destroyMethod = "")을 명시하여 ApplicationContext 셧다운 시 unopened 또는
     * StepScope 범위 밖의 Reader 빈에 대해 automatic inferred close()가 유발되는 경고를 방지합니다.
     * @param filePath 파일 경로 (Job 파라미터로 주입)
     * @return CSV 읽기 객체
     */
    @Bean(destroyMethod = "")
    @StepScope
    public FlatFileItemReader<KapExternalRatingCsvRow> rawKapExternalRatingReader(
            @Value("#{jobParameters['filePath'] ?: 'data/kap/external_ratings.csv'}") String filePath) {

        return new FlatFileItemReaderBuilder<KapExternalRatingCsvRow>()
                .name("kapExternalRatingReader")
                .resource(new ClassPathResource(filePath))
                .delimited()
                .names("customerId", "evalAgency", "ratingGrade", "baseDate")
                .fieldSetMapper(fieldSet -> KapExternalRatingCsvRow.builder()
                        .customerId(fieldSet.readString("customerId"))
                        .evalAgency(fieldSet.readString("evalAgency"))
                        .ratingGrade(fieldSet.readString("ratingGrade"))
                        .baseDate(fieldSet.readString("baseDate"))
                        .build())
                .build();
    }

    @Bean(destroyMethod = "")
    public SynchronizedItemStreamReader<KapExternalRatingCsvRow> kapExternalRatingReader(
            FlatFileItemReader<KapExternalRatingCsvRow> rawKapExternalRatingReader) {
        return new SynchronizedItemStreamReaderBuilder<KapExternalRatingCsvRow>()
                .delegate(rawKapExternalRatingReader)
                .build();
    }

    @Bean
    public JpaItemWriter<KapExternalRatingEntity> kapExternalRatingWriter() {
        return new JpaItemWriterBuilder<KapExternalRatingEntity>()
                .entityManagerFactory(entityManagerFactory)
                .build();
    }
}

