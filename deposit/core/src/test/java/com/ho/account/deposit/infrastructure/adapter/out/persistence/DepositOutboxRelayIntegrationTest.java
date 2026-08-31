package com.ho.account.deposit.infrastructure.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.contracts.outbox.JournalOutboxEvent;
import com.ho.account.contracts.outbox.JournalOutboxRelayService;
import com.ho.account.contracts.outbox.OutboxEventPublisher;
import com.ho.account.contracts.outbox.OutboxPort;
import com.ho.account.contracts.outbox.OutboxStatus;
import com.ho.account.deposit.application.port.in.OpenAccountUseCase;
import com.ho.account.deposit.application.port.out.DepositAccountMappingPort;
import com.ho.account.deposit.application.port.out.DepositAccountPersistencePort;
import com.ho.account.deposit.application.service.DepositService;
import com.ho.account.deposit.domain.DepositAccount;
import com.ho.account.deposit.domain.DepositOutboxEntity;
import com.ho.account.deposit.domain.DepositStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * [예금 트랜잭셔널 아웃박스 및 릴레이 엔진 통합 테스트]
 *
 * 🐣 [초보자를 위한 설명]
 * 이 통합 테스트는 다음 핵심 사항들을 검증합니다:
 * 1. 로컬 DB 트랜잭션 원자성: 예금 계좌 생성과 전표 Outbox 저장이 동일 트랜잭션 내에서 원자적으로 DB에 저장되는지 확인.
 * 2. 릴레이 엔진 및 상태 전이: PENDING 상태의 Outbox 이벤트를 읽어 전표 원장으로 전달 후 PUBLISHED 상태로 안전하게 전이하는지 확인.
 * 3. 장애 복구 및 재시도: 외부 전표 서비스 일시 장애 시 Outbox 이벤트가 유실되지 않고 PENDING 상태 및 에러 정보를 보존하며,
 *    서비스 복구 후 재시도를 통해 최종 정합성(Eventual Consistency)을 달성하는지 확인.
 * 4. 롤백 정합성: 트랜잭션 롤백 시 계좌 및 Outbox 이벤트가 둘 다 롤백되는지 확인.
 */
@DataJpaTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ContextConfiguration(classes = DepositOutboxRelayIntegrationTest.TestConfig.class)
@Import({
        DepositAccountPersistenceAdapter.class,
        DepositTransactionalOutboxAdapter.class
})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class DepositOutboxRelayIntegrationTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = {DepositAccount.class, DepositOutboxEntity.class})
    @EnableJpaRepositories(basePackageClasses = {SpringDataDepositAccountRepository.class, SpringDataDepositOutboxRepository.class})
    static class TestConfig {
        @Bean
        public ObjectMapper objectMapper() {
            ObjectMapper mapper = new ObjectMapper();
            mapper.registerModule(new JavaTimeModule());
            mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            return mapper;
        }
    }

    @Autowired
    private DepositAccountPersistencePort accountPersistencePort;

    @Autowired
    private OutboxPort outboxPort;

    @Autowired
    private SpringDataDepositOutboxRepository outboxRepository;

    @Autowired
    private SpringDataDepositAccountRepository accountRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @MockBean
    private DepositAccountMappingPort mappingPort;

    @MockBean
    private MasterDataQueryPort masterDataQueryPort;

    private MockJournalPostingPort journalPostingPort;
    private OutboxEventPublisher outboxEventPublisher;
    private DepositOutboxRelayScheduler relayScheduler;
    private DepositService depositService;

    @BeforeEach
    void setUp() {
        outboxRepository.deleteAll();
        accountRepository.deleteAll();

        journalPostingPort = new MockJournalPostingPort();
        outboxEventPublisher = new JournalOutboxRelayService(outboxPort, journalPostingPort);
        relayScheduler = new DepositOutboxRelayScheduler(outboxEventPublisher, outboxRepository);

        depositService = new DepositService(
                accountPersistencePort,
                mappingPort,
                masterDataQueryPort,
                journalPostingPort,
                outboxPort,
                outboxEventPublisher
        );

        when(mappingPort.resolveInitialDepositAccounts(any(DepositAccount.class)))
                .thenReturn(new DepositAccountMappingPort.InitialDepositAccounts("10100", "20200"));
        when(masterDataQueryPort.findAccountSubject(anyString()))
                .thenReturn(Optional.of(new AccountSubjectRef("dummy", "Dummy", false, false)));
    }

    @Test
    @DisplayName("계좌 개설 시 초기 입금액이 있으면 계좌 엔티티와 Outbox 이벤트가 DB에 원자적으로 함께 저장된다")
    void openAccountSavesAccountAndOutboxEventAtomicallyInDatabase() {
        OpenAccountUseCase.OpenAccountCommand command = new OpenAccountUseCase.OpenAccountCommand(
                "CUST-101",
                "PROD-SAVING",
                "KRW",
                new BigDecimal("500000.00"),
                new BigDecimal("0.035")
        );

        String accountNumber = depositService.openAccount(command);

        // 1. 계좌 저장 확인
        DepositAccount savedAccount = accountPersistencePort.findByAccountNumber(accountNumber).orElseThrow();
        assertThat(savedAccount.getBalance()).isEqualByComparingTo("500000.00");
        assertThat(savedAccount.getStatus()).isEqualTo(DepositStatus.ACTIVE);

        // 2. Outbox 이벤트 저장 확인
        String idempotencyKey = "DEPOSIT_ACCOUNT:" + accountNumber;
        DepositOutboxEntity outboxEntity = outboxRepository.findByIdempotencyKey(idempotencyKey).orElseThrow();

        assertThat(outboxEntity.getSourceModule()).isEqualTo("DEPOSIT");
        assertThat(outboxEntity.getLineageSourceType()).isEqualTo("DEPOSIT_ACCOUNT");
        assertThat(outboxEntity.getLineageSourceId()).isEqualTo(accountNumber);
        assertThat(outboxEntity.getStatus()).isEqualTo(OutboxStatus.PUBLISHED); // 초기 자동 릴레이 성공
        assertThat(outboxEntity.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("전표 릴레이 스케줄러가 PENDING 상태 이벤트를 폴링하여 전표를 생성하고 PUBLISHED로 갱신한다")
    void relayProcessesPendingOutboxEventAndMarksPublished() {
        // Given - 수동으로 PENDING Outbox 이벤트 DB 저장
        OpenAccountUseCase.OpenAccountCommand command = new OpenAccountUseCase.OpenAccountCommand(
                "CUST-202",
                "PROD-CHECKING",
                "KRW",
                BigDecimal.ZERO,
                new BigDecimal("0.01")
        );
        String accountNumber = depositService.openAccount(command);

        JournalEntryCommand journalCmd = new JournalEntryCommand(
                LocalDate.now(),
                LocalDate.now(),
                "Manual pending deposit",
                "DEPOSIT_INITIAL_DEPOSIT",
                "KRW",
                null,
                "SYSTEM",
                "SYSTEM",
                "DEPOSIT_ACCOUNT",
                accountNumber,
                java.util.List.of(
                        new com.ho.account.contracts.journal.JournalLineCommand("DEBIT", "10100", new BigDecimal("100000"), new BigDecimal("100000"), null, "CUST-202", "Cash"),
                        new com.ho.account.contracts.journal.JournalLineCommand("CREDIT", "20200", new BigDecimal("100000"), new BigDecimal("100000"), null, "CUST-202", "Deposit")
                )
        );

        JournalOutboxEvent outboxEvent = JournalOutboxEvent.createPending("DEPOSIT", "DEPOSIT_ACCOUNT", accountNumber, journalCmd, "KEY-MANUAL-" + accountNumber);
        outboxPort.saveJournalEvent(outboxEvent);

        DepositOutboxEntity savedBeforeRelay = outboxRepository.findByIdempotencyKey("KEY-MANUAL-" + accountNumber).orElseThrow();
        assertThat(savedBeforeRelay.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // When - 릴레이 실행
        int relayedCount = relayScheduler.relayPendingEvents();

        // Then
        assertThat(relayedCount).isEqualTo(1);
        DepositOutboxEntity savedAfterRelay = outboxRepository.findByIdempotencyKey("KEY-MANUAL-" + accountNumber).orElseThrow();
        assertThat(savedAfterRelay.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(savedAfterRelay.getPublishedAt()).isNotNull();
        assertThat(savedAfterRelay.getErrorMessage()).isNull();
    }

    @Test
    @DisplayName("네트워크 장애로 전표 생성이 실패하면 Outbox 이벤트는 DB에 PENDING/오류상태로 보존되고, 복구 후 재시도로 최종 정합성을 달성한다")
    void retryOnTransientJournalFailureEnsuresEventualConsistency() {
        // Given - 1회차 호출 실패 유도
        journalPostingPort.setFailTimes(1);

        OpenAccountUseCase.OpenAccountCommand command = new OpenAccountUseCase.OpenAccountCommand(
                "CUST-303",
                "PROD-RETRY",
                "KRW",
                new BigDecimal("200000.00"),
                new BigDecimal("0.02")
        );

        // 1차 시도 (openAccount 시 내부에서 즉시 릴레이를 시도하나 네트워크 에러 발생)
        String accountNumber = depositService.openAccount(command);

        // 계좌는 정상 저장되어 있음
        DepositAccount savedAccount = accountPersistencePort.findByAccountNumber(accountNumber).orElseThrow();
        assertThat(savedAccount.getBalance()).isEqualByComparingTo("200000.00");

        // Outbox 이벤트는 1회 실패 기록되고 PENDING 상태 유지 (retryCount = 1)
        DepositOutboxEntity outboxEntityAfterFail = outboxRepository.findByIdempotencyKey("DEPOSIT_ACCOUNT:" + accountNumber).orElseThrow();
        assertThat(outboxEntityAfterFail.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(outboxEntityAfterFail.getRetryCount()).isEqualTo(1);
        assertThat(outboxEntityAfterFail.getErrorMessage()).contains("Simulated Network Failure");

        // When - 2차 릴레이 실행 (스케줄러가 폴링하여 재시도, 이번엔 성공)
        int relayedCount = relayScheduler.relayPendingEvents();

        // Then - 재시도 성공으로 최종 정합성 달성
        assertThat(relayedCount).isEqualTo(1);
        DepositOutboxEntity outboxEntityAfterSuccess = outboxRepository.findByIdempotencyKey("DEPOSIT_ACCOUNT:" + accountNumber).orElseThrow();
        assertThat(outboxEntityAfterSuccess.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(outboxEntityAfterSuccess.getPublishedAt()).isNotNull();
    }

    @Test
    @DisplayName("최대 재시도 초과로 FAILED된 Outbox 이벤트를 retryFailedEvents로 복구 후 재전송할 수 있다")
    void retryFailedEventsRecoversPermanentlyFailedOutbox() {
        // Given - 5회 실패 유도
        journalPostingPort.setFailTimes(5);

        OpenAccountUseCase.OpenAccountCommand command = new OpenAccountUseCase.OpenAccountCommand(
                "CUST-404",
                "PROD-FAIL-RESET",
                "KRW",
                new BigDecimal("300000.00"),
                new BigDecimal("0.02")
        );

        String accountNumber = depositService.openAccount(command);

        // 4회 추가 릴레이 시도 -> 5회 초과로 FAILED 상태로 전이
        for (int i = 0; i < 4; i++) {
            relayScheduler.relayPendingEvents();
        }

        DepositOutboxEntity failedEntity = outboxRepository.findByIdempotencyKey("DEPOSIT_ACCOUNT:" + accountNumber).orElseThrow();
        assertThat(failedEntity.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(failedEntity.getRetryCount()).isGreaterThanOrEqualTo(5);

        // When - 관리자/스케줄러에 의한 FAILED 이벤트 복구
        int resetCount = relayScheduler.retryFailedEvents(10);
        assertThat(resetCount).isEqualTo(1);

        DepositOutboxEntity resetEntity = outboxRepository.findByIdempotencyKey("DEPOSIT_ACCOUNT:" + accountNumber).orElseThrow();
        assertThat(resetEntity.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(resetEntity.getRetryCount()).isEqualTo(0);

        // 복구 후 릴레이 재전송 (이번엔 전표 서비스가 정상 응답)
        int successRelayed = relayScheduler.relayPendingEvents();
        assertThat(successRelayed).isEqualTo(1);

        DepositOutboxEntity finalEntity = outboxRepository.findByIdempotencyKey("DEPOSIT_ACCOUNT:" + accountNumber).orElseThrow();
        assertThat(finalEntity.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("트랜잭션 롤백 시 계좌와 Outbox 이벤트가 둘 다 롤백되어 불일치가 발생하지 않는다")
    void transactionRollbackPreservesAtomicity() {
        DefaultTransactionDefinition def = new DefaultTransactionDefinition();
        def.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        TransactionStatus status = transactionManager.getTransaction(def);

        try {
            DepositAccount account = new DepositAccount();
            account.setAccountNumber("DEP-ROLLBACK-01");
            account.setCustomerCode("CUST-RB");
            account.setProductCode("PROD-RB");
            account.setCurrencyCode("KRW");
            account.setInterestRate(BigDecimal.ZERO);
            account.setOpenedAt(LocalDate.now());
            accountPersistencePort.save(account);

            JournalEntryCommand journalCmd = new JournalEntryCommand(
                    LocalDate.now(),
                    LocalDate.now(),
                    "Rollback test",
                    "DEPOSIT_INITIAL_DEPOSIT",
                    "KRW",
                    null,
                    "SYSTEM",
                    "SYSTEM",
                    "DEPOSIT_ACCOUNT",
                    "DEP-ROLLBACK-01",
                    java.util.List.of(
                            new com.ho.account.contracts.journal.JournalLineCommand("DEBIT", "10100", new BigDecimal("1000"), new BigDecimal("1000"), null, "CUST-RB", "Cash"),
                            new com.ho.account.contracts.journal.JournalLineCommand("CREDIT", "20200", new BigDecimal("1000"), new BigDecimal("1000"), null, "CUST-RB", "Deposit")
                    )
            );
            JournalOutboxEvent outboxEvent = JournalOutboxEvent.createPending("DEPOSIT", "DEPOSIT_ACCOUNT", "DEP-ROLLBACK-01", journalCmd, "KEY-RB-01");
            outboxPort.saveJournalEvent(outboxEvent);

            // 강제 롤백
            transactionManager.rollback(status);
        } catch (Exception e) {
            transactionManager.rollback(status);
        }

        // Then - DB에 계좌와 Outbox 이벤트 모두 존재하지 않아야 함
        assertThat(accountPersistencePort.findByAccountNumber("DEP-ROLLBACK-01")).isEmpty();
        assertThat(outboxRepository.findByIdempotencyKey("KEY-RB-01")).isEmpty();
    }

    private static class MockJournalPostingPort implements JournalPostingPort {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private int failTimes = 0;
        private long sequence = 5000L;

        public void setFailTimes(int failTimes) {
            this.failTimes = failTimes;
        }

        @Override
        public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
            int currentCall = callCount.incrementAndGet();
            if (currentCall <= failTimes) {
                throw new RuntimeException("Simulated Network Failure to Journal Ledger Service (Call #" + currentCall + ")");
            }
            long id = sequence++;
            return new JournalPostingResult(id, "SLIP-DEP-" + id, "DRAFT");
        }

        @Override
        public void approveAndPost(Long journalEntryId, String actor) {
        }
    }
}
