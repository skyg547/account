package com.ho.account.loan.batch.config;

import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.application.pipeline.LoanInterestAccrualPipeline;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParametersValidator;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.job.DefaultJobParametersValidator;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDate;
import java.util.Map;
import java.util.Objects;

/**
 * [배치 처리 (Batch Processing) - 대출 EIR 기반 상각 및 이자 발생]
 *
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 매일 밤(자정)에 실행되는 '대출 이자 및 부대비용 상각 자동화 공장(Spring Batch)'입니다.
 * 
 * 1. Reader (읽기): DB에서 현재 상태가 'ACTIVE(정상)'인 대출(Loan) 건을 100건씩 끊어서 가져옵니다.
 *    -> 왜 한 번에 다 안 가져오나요? 수십만 건의 대출을 한 번에 메모리에 올리면 서버가 터지니까요! (Paging)
 * 2. Writer (쓰기): chunk를 core 파이프라인에 넘깁니다. 반복 처리, 금액 판단, 전표 요청, 실패 집계는 core가 담당합니다.
 * 
 * 실행 시 `accrualDate=2026-05-21` 형식의 처리 일자를 반드시 넘겨야 합니다. 성공 로그는 재실행 시
 * 건너뛰고 실패 로그는 재시도하며, 하나라도 실패하면 Step은 성공으로 위장하지 않고 실패합니다.
 */
@Configuration
@RequiredArgsConstructor
public class LoanInterestAccrualBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final LoanInterestAccrualPipeline accrualPipeline;

    public static final String JOB_NAME = "loanInterestAccrualJob";
    private static final String STEP_NAME = "loanInterestAccrualStep";
    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job loanInterestAccrualJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(loanInterestAccrualJobParametersValidator())
                .start(loanInterestAccrualStep())
                .build();
    }

    @Bean
    public JobParametersValidator loanInterestAccrualJobParametersValidator() {
        return new DefaultJobParametersValidator(new String[]{"accrualDate"}, new String[]{});
    }

    @Bean
    @JobScope
    public Step loanInterestAccrualStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<Loan, Loan>chunk(CHUNK_SIZE, transactionManager)
                .reader(loanItemReader())
                .writer(loanItemWriter(null))
                .build();
    }

    /**
     * [Reader] 상태가 ACTIVE인 대출 건 조회
     */
    @Bean
    @StepScope
    public JpaPagingItemReader<Loan> loanItemReader() {
        return new JpaPagingItemReaderBuilder<Loan>()
                .name("loanItemReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT l FROM Loan l WHERE l.status = :status ORDER BY l.id")
                .parameterValues(Map.of("status", Loan.LoanStatus.ACTIVE))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    /**
     * [Writer] 실제 상각 및 전표 발행 서비스 호출
     */
    @Bean
    @StepScope
    public ItemWriter<Loan> loanItemWriter(
            @Value("#{jobParameters['accrualDate']}") String accrualDateStr) {
        LocalDate accrualDate = LocalDate.parse(
                Objects.requireNonNull(accrualDateStr, "accrualDate job parameter is required."));
        return loans -> accrualPipeline.processChunk(loans.getItems(), accrualDate);
    }
}
