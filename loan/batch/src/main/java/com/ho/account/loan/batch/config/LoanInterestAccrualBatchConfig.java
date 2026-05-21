package com.ho.account.loan.batch.config;

import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.service.InterestAccrualService;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobScope;
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

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * [배치 처리 (Batch Processing) - 대출 EIR 기반 상각 및 이자 발생]
 *
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 매일 밤(자정)에 실행되는 '대출 이자 및 부대비용 상각 자동화 공장(Spring Batch)'입니다.
 * 
 * 1. Reader (읽기): DB에서 현재 상태가 'ACTIVE(정상)'인 대출(Loan) 건을 100건씩 끊어서 가져옵니다.
 *    -> 왜 한 번에 다 안 가져오나요? 수십만 건의 대출을 한 번에 메모리에 올리면 서버가 터지니까요! (Paging)
 * 2. Processor (가공): 가져온 대출 건에 대해 "오늘 자 기준의 상각(Amortization) 금액"을 계산합니다.
 * 3. Writer (쓰기): 계산된 결과를 바탕으로 회계 전표(Journal)를 자동으로 끊어줍니다.
 * 
 * 실행 시 파라미터로 `accrualDate=2026-05-21` 과 같이 처리 일자를 넘겨줄 수 있습니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class LoanInterestAccrualBatchConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final InterestAccrualService interestAccrualService;

    public static final String JOB_NAME = "loanInterestAccrualJob";
    private static final String STEP_NAME = "loanInterestAccrualStep";
    private static final int CHUNK_SIZE = 100;

    @Bean
    public Job loanInterestAccrualJob() {
        return new JobBuilder(JOB_NAME, jobRepository)
                .start(loanInterestAccrualStep())
                .build();
    }

    @Bean
    @JobScope
    public Step loanInterestAccrualStep() {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<Loan, Loan>chunk(CHUNK_SIZE, transactionManager)
                .reader(loanItemReader())
                .processor(loanItemProcessor(null))
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
                .queryString("SELECT l FROM Loan l WHERE l.status = :status")
                .parameterValues(Map.of("status", Loan.LoanStatus.ACTIVE))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    /**
     * [Processor] 특별한 가공 없이 대상만 패스 처리 
     * (상세 로직은 Service에 위임)
     */
    @Bean
    @StepScope
    public ItemProcessor<Loan, Loan> loanItemProcessor(
            @Value("#{jobParameters['accrualDate']}") String accrualDateStr) {
        return loan -> {
            log.debug("Processing Loan: {}", loan.getLoanNumber());
            return loan;
        };
    }

    /**
     * [Writer] 실제 상각 및 전표 발행 서비스 호출
     */
    @Bean
    @StepScope
    public ItemWriter<Loan> loanItemWriter(
            @Value("#{jobParameters['accrualDate']}") String accrualDateStr) {
        return loans -> {
            LocalDate accrualDate = (accrualDateStr != null) 
                    ? LocalDate.parse(accrualDateStr, DateTimeFormatter.ISO_DATE) 
                    : LocalDate.now();

            for (Loan loan : loans) {
                try {
                    // InterestAccrualService 내부에서 해당 Loan에 대해 단건 전표 발행 및 로그 저장 수행
                    interestAccrualService.processIndividualAccrual(loan, accrualDate);
                } catch (Exception e) {
                    log.error("Failed to process loan accrual for {}: {}", loan.getLoanNumber(), e.getMessage());
                    // Batch 운영 정책에 따라 예외를 던지거나(실패처리), 로그만 남기고 Skip 가능.
                    // 현재는 Skip & 로깅으로 타 대출건에 영향이 가지 않도록 함.
                }
            }
        };
    }
}
