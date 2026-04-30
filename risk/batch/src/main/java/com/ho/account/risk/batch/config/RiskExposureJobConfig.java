package com.ho.account.risk.batch.config;

import com.ho.account.loan.domain.Loan;
import com.ho.account.risk.application.pipeline.RiskExposurePipeline;
import com.ho.account.risk.application.port.out.RiskPersistencePort;
import com.ho.account.risk.domain.CreditRiskExposure;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
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
import java.util.List;

/**
 * [RiskExposureJobConfig]
 * 대출 원천 데이터를 리스크 RDM(익스포저)으로 추출 및 변환하는 배치 잡 설정.
 * 고속 처리를 위해 Chunk 지향 프로세싱과 JDBC Bulk Insert를 결합합니다.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class RiskExposureJobConfig {

    private final EntityManagerFactory entityManagerFactory;
    private final RiskExposurePipeline exposurePipeline;
    private final RiskPersistencePort riskPersistencePort;

    @Bean
    public Job extractRiskExposureJob(JobRepository jobRepository, Step extractExposureStep) {
        return new JobBuilder("extractRiskExposureJob", jobRepository)
                .start(extractExposureStep)
                .build();
    }

    @Bean
    public Step extractExposureStep(JobRepository jobRepository, PlatformTransactionManager transactionManager) {
        return new StepBuilder("extractExposureStep", jobRepository)
                .<Loan, CreditRiskExposure>chunk(1000, transactionManager)
                .reader(loanReader())
                .processor(exposureProcessor(null))
                .writer(exposureWriter())
                .build();
    }

    @Bean
    public JpaPagingItemReader<Loan> loanReader() {
        return new JpaPagingItemReaderBuilder<Loan>()
                .name("loanReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT l FROM Loan l WHERE l.status = 'ACTIVE'")
                .pageSize(1000)
                .build();
    }

    @Bean
    @StepScope
    public ItemProcessor<Loan, CreditRiskExposure> exposureProcessor(
            @Value("#{jobParameters['baseDate']}") String baseDateStr) {
        return loan -> {
            LocalDate baseDate = (baseDateStr != null) ? LocalDate.parse(baseDateStr) : LocalDate.now();
            return exposurePipeline.process(loan, baseDate);
        };
    }

    @Bean
    public ItemWriter<CreditRiskExposure> exposureWriter() {
        return items -> {
            log.info("Writing chunk of {} exposures to RDM via JDBC Bulk", items.size());
            riskPersistencePort.saveAllExposures((List<CreditRiskExposure>) items.getItems());
        };
    }
}
