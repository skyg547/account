package com.ho.account.expenditure.payable.batch;

import com.ho.account.expenditure.application.port.in.PaymentRunCommand;
import com.ho.account.expenditure.application.port.in.PaymentUseCase;
import com.ho.account.expenditure.domain.PayableStatus;
import com.ho.account.expenditure.domain.PaymentRun;
import com.ho.account.expenditure.infrastructure.persistence.entity.PayableJpaEntity;
import jakarta.persistence.EntityManagerFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * [Spring Batch 청크 지향 배치 구성 - 지급 실행(Payment Run) 배치]
 *
 * 🎓 [교육적 주석: Spring Batch 청크 지향 아키텍처 (Chunk-oriented Architecture) 및 트랜잭션 경계 분리]
 *
 * 1. 단일 트랜잭션 Tasklet 방식의 위험성:
 *    기존에는 단일 Tasklet에서 만기 도래한 모든 채무(Payable) 목록을 힙 메모리에 한 번에 불러와
 *    단일 트랜잭션 내에서 지급(Payment) 레코드를 일괄 생성했습니다.
 *    이 방식은 대량 결제 데이터 발생 시 힙 메모리 OOM(Out Of Memory), DB Long-Transaction Lock,
 *    Connection Timeout 및 중간 실패 시 전체 롤백 문제를 야기합니다.
 *
 * 2. Step 분리 및 청크 지향 아키텍처 전환의 이점:
 *    - 1단계 (Tasklet Step): PaymentRun 헤더 엔티티를 생성하고 INITIATED 상태로 초기화하여 JobExecutionContext에 저장합니다.
 *    - 2단계 (Chunk Step): Paging ItemReader로 만기 도래한 채무 엔티티(`PayableJpaEntity`)를 CHUNK_SIZE(100건) 단위로 나누어 조회하고,
 *      ItemWriter를 통해 100건 단위로 지급(Payment)을 등록/영속화합니다.
 *    - 3단계 (Tasklet Step): 청크 처리가 완료된 후 PaymentRun 상태를 PROCESSING으로 전이시킵니다.
 *    - 메모리 풋프린트 관리(Memory Footprint Control): 수십만 건의 채무 데이터가 존재해도 constant dynamic chunk size(100)로
 *      메모리 사용량을 엄격히 상한 제어합니다.
 *    - 트랜잭션 경계 분리(Transaction Boundary Segregation): 100건 청크 단위 커밋으로 부분 롤백 및 재시도(Retry/Skip)가 가능합니다.
 */
@Configuration
public class PayablePaymentRunBatchConfig {

    public static final String JOB_NAME = "payablePaymentRunJob";
    private static final int CHUNK_SIZE = 100;

    @Bean
    Job payablePaymentRunJob(
            JobRepository jobRepository,
            Step createPaymentRunStep,
            Step processPayablePaymentRunChunkStep,
            Step completePaymentRunStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(createPaymentRunStep)
                .next(processPayablePaymentRunChunkStep)
                .next(completePaymentRunStep)
                .build();
    }

    @Bean
    Step createPaymentRunStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PaymentUseCase paymentUseCase) {
        return new StepBuilder("createPaymentRunStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    JobParameters parameters = contribution.getStepExecution().getJobParameters();
                    LocalDate runDate = localDate(parameters, "runDate", LocalDate.now());
                    String createdBy = text(parameters, "createdBy", "PAYABLE_BATCH");
                    String description = text(parameters, "description", "Scheduled payable payment run");

                    PaymentRun paymentRun = paymentUseCase.createPaymentRun(
                            new PaymentRunCommand(runDate, description, createdBy));

                    chunkContext.getStepContext().getStepExecution().getJobExecution()
                            .getExecutionContext().put("paymentRunId", paymentRun.getId());
                    chunkContext.getStepContext().getStepExecution().getJobExecution()
                            .getExecutionContext().put("runDate", runDate.toString());

                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    @Bean
    Step processPayablePaymentRunChunkStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            JpaPagingItemReader<PayableJpaEntity> payablePaymentRunItemReader,
            ItemWriter<PayableJpaEntity> payablePaymentRunItemWriter) {
        return new StepBuilder("processPayablePaymentRunChunkStep", jobRepository)
                .<PayableJpaEntity, PayableJpaEntity>chunk(CHUNK_SIZE, transactionManager)
                .reader(payablePaymentRunItemReader)
                .writer(payablePaymentRunItemWriter)
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<PayableJpaEntity> payablePaymentRunItemReader(
            EntityManagerFactory entityManagerFactory,
            @Value("#{jobExecutionContext['runDate']}") String runDateStr) {
        LocalDate runDate = (runDateStr != null && !runDateStr.isBlank())
                ? LocalDate.parse(runDateStr) : LocalDate.now();

        return new JpaPagingItemReaderBuilder<PayableJpaEntity>()
                .name("payablePaymentRunItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT p FROM PayableJpaEntity p WHERE p.dueDate < :cutoffDate AND p.status != :paidStatus AND p.status != :inPaymentStatus AND p.status != :writtenOffStatus ORDER BY p.id")
                .parameterValues(Map.of(
                        "cutoffDate", runDate.plusDays(1),
                        "paidStatus", PayableStatus.PAID,
                        "inPaymentStatus", PayableStatus.IN_PAYMENT,
                        "writtenOffStatus", PayableStatus.WRITTEN_OFF))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    @StepScope
    public ItemWriter<PayableJpaEntity> payablePaymentRunItemWriter(
            PaymentUseCase paymentUseCase,
            @Value("#{jobExecutionContext['paymentRunId']}") Long paymentRunId,
            @Value("#{jobExecutionContext['runDate']}") String runDateStr) {
        LocalDate runDate = (runDateStr != null && !runDateStr.isBlank())
                ? LocalDate.parse(runDateStr) : LocalDate.now();

        return items -> {
            List<Long> payableIds = items.getItems().stream()
                    .map(PayableJpaEntity::getId)
                    .toList();
            if (!payableIds.isEmpty()) {
                paymentUseCase.processPaymentRunChunk(paymentRunId, runDate, payableIds);
            }
        };
    }

    @Bean
    Step completePaymentRunStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PaymentUseCase paymentUseCase) {
        return new StepBuilder("completePaymentRunStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    Long paymentRunId = (Long) chunkContext.getStepContext().getStepExecution().getJobExecution()
                            .getExecutionContext().get("paymentRunId");
                    if (paymentRunId != null) {
                        paymentUseCase.completePaymentRun(paymentRunId);
                    }
                    return RepeatStatus.FINISHED;
                }, transactionManager)
                .build();
    }

    private LocalDate localDate(JobParameters parameters, String key, LocalDate defaultValue) {
        String value = parameters.getString(key);
        return value == null || value.isBlank() ? defaultValue : LocalDate.parse(value.trim());
    }

    private String text(JobParameters parameters, String key, String defaultValue) {
        String value = parameters.getString(key);
        return value == null || value.isBlank() ? defaultValue : value.trim();
    }
}