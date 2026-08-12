package com.ho.account.journalledger.batch.config;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlBalance;
import com.ho.account.journalledger.domain.ledger.repository.GlBalanceRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlBalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBatchTest
@SpringBootTest(
        classes = BalanceReaggregationBatchConfigTest.TestConfig.class,
        properties = {
                "spring.profiles.active=local",
                "spring.main.allow-bean-definition-overriding=true",
                "spring.datasource.url=jdbc:h2:mem:journal-batch-reagg-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                "spring.datasource.driver-class-name=org.h2.Driver",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.flyway.enabled=true",
                "spring.flyway.locations=classpath:db/journal-migration",
                "spring.flyway.clean-disabled=true",
                "spring.jpa.hibernate.ddl-auto=validate",
                "spring.batch.job.enabled=false",
                "spring.batch.jdbc.initialize-schema=always",
                "spring.sql.init.mode=never",
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false"
        }
)
@ActiveProfiles("local")
class BalanceReaggregationBatchConfigTest {

    @Autowired
    private JobLauncherTestUtils jobLauncherTestUtils;

    @Autowired
    private Job dailyBalanceReaggregationJob;

    @Autowired
    private JournalEntryRepository journalEntryRepository;

    @Autowired
    private JournalDetailRepository journalDetailRepository;

    @Autowired
    private GlBalanceRepository glBalanceRepository;

    @Autowired
    private SlBalanceRepository slBalanceRepository;

    @BeforeEach
    void setUp() {
        jobLauncherTestUtils.setJob(dailyBalanceReaggregationJob);
        glBalanceRepository.deleteAllInBatch();
        slBalanceRepository.deleteAllInBatch();
        journalDetailRepository.deleteAllInBatch();
        journalEntryRepository.deleteAllInBatch();
    }

    @Test
    @DisplayName("Chunk 기반 잔액 재집계 배치 실행 및 멱등성 검증")
    void testBalanceReaggregationChunkBatchAndIdempotency() throws Exception {
        // given: 2026-08-01 날짜의 POSTED 전표 1건 (차변 50000, 대변 50000)
        LocalDate targetDate = LocalDate.of(2026, 8, 1);

        JournalEntry entry1 = new JournalEntry();
        entry1.setSlipNo("SLIP-20260801-001");
        entry1.setSlipDate(targetDate);
        entry1.setAccountingDate(targetDate);
        entry1.setCurrencyCode("KRW");
        entry1.setDescription("배치 테스트 전표 1");

        JournalDetail detail1 = new JournalDetail();
        detail1.setSide(JournalSide.DEBIT);
        detail1.setAccountCode("10100");
        detail1.setAmount(new BigDecimal("50000.00"));
        detail1.setBaseAmount(new BigDecimal("50000.00"));
        entry1.addDetail(detail1);

        JournalDetail detail2 = new JournalDetail();
        detail2.setSide(JournalSide.CREDIT);
        detail2.setAccountCode("10200");
        detail2.setAmount(new BigDecimal("50000.00"));
        detail2.setBaseAmount(new BigDecimal("50000.00"));
        entry1.addDetail(detail2);

        entry1.initializeDraft();
        entry1.approve("TEST");
        entry1.post("TEST");

        journalEntryRepository.save(entry1);

        // 1회차 배치 실행
        JobParameters jobParameters1 = new JobParametersBuilder()
                .addString("startDate", "2026-08-01")
                .addString("endDate", "2026-08-01")
                .addLong("time", System.currentTimeMillis())
                .toJobParameters();

        JobExecution jobExecution1 = jobLauncherTestUtils.launchJob(jobParameters1);

        // then 1: 1회차 실행 성공 및 잔액 확인
        assertThat(jobExecution1.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        List<GlBalance> glBalances1 = glBalanceRepository.findAll();
        assertThat(glBalances1).hasSize(2);

        GlBalance debitBal1 = glBalanceRepository.findByAccountCodeAndCurrencyCodeAndBalanceDateAndPeriod(
                "10100", "KRW", targetDate, YearMonth.from(targetDate)).orElseThrow();
        assertThat(debitBal1.getDebitAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(debitBal1.getCreditAmount()).isEqualByComparingTo(BigDecimal.ZERO);

        // 2회차 재실행 (동일 기간 - 멱등성 검증)
        JobParameters jobParameters2 = new JobParametersBuilder()
                .addString("startDate", "2026-08-01")
                .addString("endDate", "2026-08-01")
                .addLong("time", System.currentTimeMillis() + 1000)
                .toJobParameters();

        JobExecution jobExecution2 = jobLauncherTestUtils.launchJob(jobParameters2);

        // then 2: 2회차 실행 성공 및 잔액이 이중 합산되지 않고 1회차와 동일함을 보장
        assertThat(jobExecution2.getExitStatus()).isEqualTo(ExitStatus.COMPLETED);

        List<GlBalance> glBalances2 = glBalanceRepository.findAll();
        assertThat(glBalances2).hasSize(2);

        GlBalance debitBal2 = glBalanceRepository.findByAccountCodeAndCurrencyCodeAndBalanceDateAndPeriod(
                "10100", "KRW", targetDate, YearMonth.from(targetDate)).orElseThrow();
        assertThat(debitBal2.getDebitAmount()).isEqualByComparingTo(new BigDecimal("50000.00"));
        assertThat(debitBal2.getCreditAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    @EntityScan(basePackages = {
            "com.ho.account.journalledger.domain",
            "org.springframework.batch.core"
    })
    @EnableJpaRepositories(basePackages = {
            "com.ho.account.journalledger.domain",
            "com.ho.account.journalledger.adapter.out.persistence"
    })
    @ComponentScan(basePackages = {
            "com.ho.account.journalledger.batch",
            "com.ho.account.journalledger.application",
            "com.ho.account.journalledger.infrastructure"
    })
    static class TestConfig {
    }
}
