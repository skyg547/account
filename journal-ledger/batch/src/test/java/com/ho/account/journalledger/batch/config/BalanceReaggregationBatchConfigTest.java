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
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.application.service.ledger.BalanceReaggregationService;
import com.ho.account.journalledger.application.port.out.BalanceReaggregationControlPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.BatchStatus;
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
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

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

    @SpyBean
    private LedgerService ledgerService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @SpyBean
    private BalanceReaggregationService reaggregationService;

    @Autowired
    private BalanceReaggregationControlPort reaggregationControl;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void setUp() {
        reset(ledgerService, reaggregationService);
        jdbcTemplate.update("UPDATE ledger_reaggregation_control SET status = 'OPEN',"
                + " owner_job_instance_id = NULL, range_start = NULL, range_end = NULL, epoch = epoch + 1"
                + " WHERE control_id = 1");
        jobLauncherTestUtils.setJob(dailyBalanceReaggregationJob);
        glBalanceRepository.deleteAllInBatch();
        slBalanceRepository.deleteAllInBatch();
        // This fixed in-memory H2 database is disposable test state. Child rows must be removed first
        // so fixture cleanup respects the journal_details -> journal_entries foreign key.
        jdbcTemplate.update("DELETE FROM journal_details");
        jdbcTemplate.update("DELETE FROM journal_entries");
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

        entry1.setCreatedBy("batch-maker");
        entry1.initializeDraft();
        entry1.requestApproval("batch-maker");
        entry1.approve("TEST");
        journalEntryRepository.saveAndFlush(entry1);
        postPersistedEntries(List.of(entry1));

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

    @Test
    @DisplayName("재집계는 POSTED 원본과 POSTED 역분개를 함께 반영하고 DRAFT·REJECTED는 제외한다")
    void rebuildNetsPostedOriginalAndReversalAndExcludesIneffectiveDrafts() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 8, 5);
        JournalEntry original = approvedEntry(
                "REBUILD-ORIGINAL", targetDate, "10100", "20100", new BigDecimal("100.00"));
        JournalEntry reversal = approvedEntry(
                "REBUILD-REVERSAL", targetDate, "20100", "10100", new BigDecimal("100.00"));
        reversal.setEntryType("REVERSAL");
        reversal.setLineageSourceType("JOURNAL_ENTRY");
        reversal.setLineageSourceId("759");
        JournalEntry abandonedDraft = draftEntry(
                "REBUILD-DRAFT", targetDate, "10100", "20100", new BigDecimal("70.00"));
        JournalEntry rejected = draftEntry(
                "REBUILD-REJECTED", targetDate, "10100", "20100", new BigDecimal("90.00"));
        rejected.reject("checker", "abandoned reversal");
        journalEntryRepository.saveAllAndFlush(List.of(original, reversal, abandonedDraft, rejected));
        postPersistedEntries(List.of(original, reversal));

        JobExecution execution = jobLauncherTestUtils.launchJob(new JobParametersBuilder()
                .addString("startDate", targetDate.toString())
                .addString("endDate", targetDate.toString())
                .addLong("postedReversalNet", System.currentTimeMillis())
                .toJobParameters());

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(glBalanceRepository.findAll()).hasSize(2).allSatisfy(balance -> {
            assertThat(balance.getDebitAmount()).isEqualByComparingTo("100.00");
            assertThat(balance.getCreditAmount()).isEqualByComparingTo("100.00");
            assertThat(balance.getEndingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        });
        assertThat(slBalanceRepository.findAll()).hasSize(2).allSatisfy(balance -> {
            assertThat(balance.getDebitAmount()).isEqualByComparingTo("100.00");
            assertThat(balance.getCreditAmount()).isEqualByComparingTo("100.00");
            assertThat(balance.getEndingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        });
    }

    @Test
    @DisplayName("cleanup이 커밋된 뒤 장애가 나도 삭제된 잔액을 정상 조회로 수락하지 않는다")
    void rejectsAcceptedEmptyBalancesAfterCommittedCleanup() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 8, 2);
        GlBalance existing = new GlBalance();
        existing.setAccountCode("10100");
        existing.setCurrencyCode("KRW");
        existing.setBalanceDate(targetDate);
        existing.setPeriod(YearMonth.from(targetDate));
        existing.setBeginningBalance(BigDecimal.ZERO);
        existing.setDebitAmount(new BigDecimal("100.00"));
        existing.setCreditAmount(BigDecimal.ZERO);
        existing.setEndingBalance(new BigDecimal("100.00"));
        glBalanceRepository.saveAndFlush(existing);

        JournalEntry source = new JournalEntry();
        source.setSlipNo("SLIP-FAIL-001");
        source.setSlipDate(targetDate);
        source.setAccountingDate(targetDate);
        source.setCurrencyCode("KRW");
        JournalDetail debit = new JournalDetail();
        debit.setSide(JournalSide.DEBIT);
        debit.setAccountCode("10100");
        debit.setAmount(new BigDecimal("100.00"));
        debit.setBaseAmount(new BigDecimal("100.00"));
        source.addDetail(debit);
        JournalDetail credit = new JournalDetail();
        credit.setSide(JournalSide.CREDIT);
        credit.setAccountCode("20100");
        credit.setAmount(new BigDecimal("100.00"));
        credit.setBaseAmount(new BigDecimal("100.00"));
        source.addDetail(credit);
        source.setCreatedBy("batch-maker");
        source.initializeDraft();
        source.requestApproval("batch-maker");
        source.approve("TEST");
        journalEntryRepository.saveAndFlush(source);
        postPersistedEntries(List.of(source));

        doThrow(new IllegalStateException("injected writer failure after cleanup"))
                .when(ledgerService).updateLedgerBalancesBulkForReaggregation(
                        anyLong(), any(LocalDate.class), any(LocalDate.class), anyList());

        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", targetDate.toString())
                .addString("endDate", targetDate.toString())
                .addLong("redRun", System.currentTimeMillis())
                .toJobParameters();

        JobExecution cleanup = jobLauncherTestUtils.launchJob(parameters);

        assertThat(cleanup.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(glBalanceRepository.findAll()).isEmpty();
        assertThatThrownBy(() -> ledgerService.getGlBalances(targetDate, targetDate, "10100", "KRW"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reaggregation");
    }

    @ParameterizedTest
    @ValueSource(ints = {2, 3})
    @DisplayName("201건 초과 입력은 chunk 경계 장애 후 같은 JobInstance restart에서 정확히 이어진다")
    void sameInstanceRestartResumesCommittedChunksWithoutCleanupOrDuplication(int failingWriterInvocation) throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 8, 3);
        List<JournalEntry> sources = new ArrayList<>();
        for (int index = 0; index < 101; index++) {
            sources.add(approvedEntry("MC-" + failingWriterInvocation + "-" + index, targetDate,
                    "10100", "20100", BigDecimal.ONE));
        }
        journalEntryRepository.saveAllAndFlush(sources);
        postPersistedEntries(sources);

        AtomicInteger invocations = new AtomicInteger();
        doAnswer(call -> {
            if (invocations.incrementAndGet() == failingWriterInvocation) {
                throw new IllegalStateException("injected chunk boundary failure " + failingWriterInvocation);
            }
            return call.callRealMethod();
        }).when(ledgerService).updateLedgerBalancesBulkForReaggregation(
                anyLong(), any(LocalDate.class), any(LocalDate.class), anyList());

        JobParameters parameters = new JobParametersBuilder()
                .addString("startDate", targetDate.toString())
                .addString("endDate", targetDate.toString())
                .addLong("restartCase", (long) failingWriterInvocation)
                .toJobParameters();
        JobExecution failed = jobLauncherTestUtils.launchJob(parameters);

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        BigDecimal committedDebit = failingWriterInvocation == 2 ? new BigDecimal("50.00") : new BigDecimal("100.00");
        assertThat(glBalanceRepository.findByAccountCodeAndCurrencyCodeAndBalanceDateAndPeriod(
                "10100", "KRW", targetDate, YearMonth.from(targetDate)).orElseThrow().getDebitAmount())
                .isEqualByComparingTo(committedDebit);
        assertThatThrownBy(() -> ledgerService.getGlBalances(targetDate, targetDate, "10100", "KRW"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("reaggregation");

        reset(ledgerService);
        JobExecution restarted = jobLauncherTestUtils.launchJob(parameters);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(restarted.getJobInstance().getInstanceId()).isEqualTo(failed.getJobInstance().getInstanceId());
        verify(reaggregationService, times(1)).clean(anyLong(), any(LocalDate.class), any(LocalDate.class));
        assertThat(glBalanceRepository.findByAccountCodeAndCurrencyCodeAndBalanceDateAndPeriod(
                "10100", "KRW", targetDate, YearMonth.from(targetDate)).orElseThrow().getDebitAmount())
                .isEqualByComparingTo("101.00");
        assertThat(ledgerService.getGlBalances(targetDate, targetDate, "10100", "KRW")).hasSize(1);
    }

    @Test
    @DisplayName("다른 JobInstance는 진행 중 owner/range와 겹치면 start에서 거부된다")
    void distinctOverlappingJobInstanceIsRejected() throws Exception {
        LocalDate targetDate = LocalDate.of(2026, 8, 4);
        reaggregationService.start(Long.MAX_VALUE, targetDate, targetDate);
        JobParameters parameters = new JobParametersBuilder()
                .addString("baseDate", targetDate.toString())
                .addLong("overlap", System.currentTimeMillis())
                .toJobParameters();

        JobExecution rejected = jobLauncherTestUtils.launchJob(parameters);

        assertThat(rejected.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(reaggregationControl.snapshot().ownerJobInstanceId()).isEqualTo(Long.MAX_VALUE);
        assertThatThrownBy(() -> ledgerService.getGlBalances(targetDate, targetDate, null, null))
                .isInstanceOf(IllegalStateException.class);
    }

    private JournalEntry approvedEntry(String slipNo, LocalDate date, String debitAccount,
                                       String creditAccount, BigDecimal amount) {
        JournalEntry entry = draftEntry(slipNo, date, debitAccount, creditAccount, amount);
        entry.requestApproval("batch-maker");
        entry.approve("TEST");
        return entry;
    }

    private JournalEntry draftEntry(String slipNo, LocalDate date, String debitAccount,
                                    String creditAccount, BigDecimal amount) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipNo(slipNo);
        entry.setSlipDate(date);
        entry.setAccountingDate(date);
        entry.setCurrencyCode("KRW");
        JournalDetail debit = new JournalDetail();
        debit.setSide(JournalSide.DEBIT);
        debit.setAccountCode(debitAccount);
        debit.setAmount(amount);
        debit.setBaseAmount(amount);
        entry.addDetail(debit);
        JournalDetail credit = new JournalDetail();
        credit.setSide(JournalSide.CREDIT);
        credit.setAccountCode(creditAccount);
        credit.setAmount(amount);
        credit.setBaseAmount(amount);
        entry.addDetail(credit);
        entry.setCreatedBy("batch-maker");
        entry.initializeDraft();
        return entry;
    }

    private void postPersistedEntries(List<JournalEntry> approvedEntries) {
        List<Long> ids = approvedEntries.stream().map(JournalEntry::getId).toList();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            List<JournalEntry> persistedEntries = journalEntryRepository.findAllById(ids);
            persistedEntries.forEach(entry -> entry.post("TEST"));
            journalEntryRepository.saveAllAndFlush(persistedEntries);
        });
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
