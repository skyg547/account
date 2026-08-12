package com.ho.account.receivable.batch;

import com.ho.account.receivable.application.port.in.CollectionUseCase;
import com.ho.account.receivable.domain.CollectionStatus;
import com.ho.account.receivable.infrastructure.persistence.entity.CollectionJpaEntity;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Map;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * [Spring Batch 청크 지향 배치 구성 - 수납 자동 매칭 배치]
 *
 * 🎓 [교육적 주석: Spring Batch 청크 지향 아키텍처 (Chunk-oriented Architecture)와 트랜잭션 경계 분리]
 *
 * 1. 단일 트랜잭션 Tasklet 방식의 한계:
 *    기존에는 단일 Tasklet 내에서 수만 건 이상의 자동 매칭 대상 수납(Collection) 데이터를 한 번에 메모리에 적재하여
 *    단일 루프로 처리했습니다. 이는 다음과 같은 치명적인 문제를 유발합니다:
 *    - 힙 메모리 고갈 (Out Of Memory Error): 대량 엔티티 로딩으로 인한 GC 압박 및 OOM 위험.
 *    - DB 락 장기화 및 타임아웃: 단일 트랜잭션이 길어짐에 따라 테이블/행 락이 오래 유지되어 DB 데드락 및 Connection Timeout 발생.
 *    - Partial Failure 불가: 루프 중간에 1건만 예외가 발생해도 전체 수만 건의 자동 매칭 작업이 롤백됨.
 *
 * 2. 청크 지향 아키텍처(Chunk-Oriented Architecture)의 아키텍처적 이점:
 *    - 메모리 풋프린트 상한 제어 (Memory Footprint Management): Paging ItemReader를 통해 CHUNK_SIZE(100건) 단위로
 *      데이터를 페이징 조회하여 JVM 힙 메모리 사용량을 항상 상한(Constant Bound) 이내로 일정하게 유지합니다.
 *    - 트랜잭션 경계 분리 (Transaction Boundary Segregation): 100건 단위로 트랜잭션 Commit을 수행하므로,
 *      특정 청크에서 장애가 발생하더라도 이미 커밋된 이전 청크의 결과는 안전하게 보존되고 해당 청크 단위의 Rollback & Retry가 가능합니다.
 *    - DB 커넥션 및 락 최적화: 트랜잭션 유지 시간이 짧아져 DB 자원 점유 시간을 최소화합니다.
 */
@Configuration
public class ReceivableAutoMatchingBatchConfig {

    public static final String JOB_NAME = "receivableAutoMatchingJob";
    private static final String STEP_NAME = "receivableAutoMatchingStep";
    private static final int CHUNK_SIZE = 100;

    @Bean
    Job receivableAutoMatchingJob(JobRepository jobRepository, Step receivableAutoMatchingStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(receivableAutoMatchingStep)
                .build();
    }

    @Bean
    Step receivableAutoMatchingStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JpaPagingItemReader<CollectionJpaEntity> receivableAutoMatchingItemReader,
            ItemWriter<CollectionJpaEntity> receivableAutoMatchingItemWriter) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<CollectionJpaEntity, CollectionJpaEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(receivableAutoMatchingItemReader)
                .writer(receivableAutoMatchingItemWriter)
                .build();
    }

    /**
     * [Paging ItemReader] 자동 매칭 대상 수납(Collection) 엔티티를 100건 단위로 페이징 조회합니다.
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<CollectionJpaEntity> receivableAutoMatchingItemReader(
            EntityManagerFactory entityManagerFactory) {
        return new JpaPagingItemReaderBuilder<CollectionJpaEntity>()
                .name("receivableAutoMatchingItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT c FROM CollectionJpaEntity c WHERE c.status IN :statuses ORDER BY c.id")
                .parameterValues(Map.of("statuses", List.of(
                        CollectionStatus.RECEIVED,
                        CollectionStatus.UNMATCHED,
                        CollectionStatus.PARTIAL_MATCHED)))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    /**
     * [Bulk Chunk ItemWriter] 100건 청크 단위로 자동 매칭 서비스(CollectionUseCase)를 호출합니다.
     */
    @Bean
    @StepScope
    public ItemWriter<CollectionJpaEntity> receivableAutoMatchingItemWriter(CollectionUseCase collectionUseCase) {
        return items -> {
            for (CollectionJpaEntity entity : items) {
                if (entity.getId() != null) {
                    collectionUseCase.attemptAutoMatching(entity.getId());
                }
            }
        };
    }
}
