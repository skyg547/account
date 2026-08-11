package com.ho.account.contracts.outbox;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class JournalOutboxPatternTest {

    private InMemoryOutboxAdapter outboxAdapter;
    private MockJournalPostingPort journalPostingPort;
    private JournalOutboxRelayService relayService;

    @BeforeEach
    void setUp() {
        outboxAdapter = new InMemoryOutboxAdapter();
        journalPostingPort = new MockJournalPostingPort();
        relayService = new JournalOutboxRelayService(outboxAdapter, journalPostingPort);
    }

    @Test
    @DisplayName("Transactional Outbox: 비즈니스 트랜잭션과 함께 전표 이벤트를 원자적으로 저장하고 릴레이로 성공 발행한다")
    void outboxEventAtomicSaveAndRelaySuccess() {
        // Given
        JournalEntryCommand command = createSampleCommand("DEP-1001", new BigDecimal("100000"));
        JournalOutboxEvent event = JournalOutboxEvent.createPending("DEPOSIT", "DEPOSIT_ACCOUNT", "DEP-1001", command);

        // When - 트랜잭션 수반 시 Outbox 저장
        outboxAdapter.saveJournalEvent(event);

        // Then - 저장 직후 상태는 PENDING
        assertThat(outboxAdapter.findPendingJournalEvents(10)).hasSize(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);

        // When - 비동기 릴레이 엔진 실행
        int publishedCount = relayService.publishPendingJournalEvents();

        // Then - 발행 완료 후 상태는 PUBLISHED, PENDING 목록은 0건
        assertThat(publishedCount).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(event.getPublishedAt()).isNotNull();
        assertThat(outboxAdapter.findPendingJournalEvents(10)).isEmpty();
    }

    @Test
    @DisplayName("Dual Write 정합성: 네트워크 1차 실패 시에도 Outbox 비동기 재시도를 통해 최종 정합성을 달성한다")
    void eventualConsistencyWithNetworkFailureAndRetry() {
        // Given - 1차 호출 시 예외를 발생하는 네트워크 장애 모동작 설정
        journalPostingPort.setFailTimes(1);

        JournalEntryCommand command = createSampleCommand("LOAN-7701", new BigDecimal("5000000"));
        JournalOutboxEvent event = JournalOutboxEvent.createPending("LOAN", "LOAN_DISBURSAL", "LOAN-7701", command);
        outboxAdapter.saveJournalEvent(event);

        // When - 1차 릴레이 시도 (네트워크 장애 발생)
        int firstPublishCount = relayService.publishPendingJournalEvents();

        // Then - 1차 시도 실패 및 retryCount 증가, PENDING 유지
        assertThat(firstPublishCount).isEqualTo(0);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PENDING);
        assertThat(event.getRetryCount()).isEqualTo(1);
        assertThat(event.getErrorMessage()).contains("Network Error");

        // When - 2차 릴레이 시도 (네트워크 복구됨)
        int secondPublishCount = relayService.publishPendingJournalEvents();

        // Then - 최종 정합성 달성 (PUBLISHED 상태로 전환)
        assertThat(secondPublishCount).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
    }

    @Test
    @DisplayName("멱등성(Idempotency): 동일한 idempotencyKey를 갖는 Outbox 이벤트를 조회하여 중복 저장을 차단할 수 있다")
    void idempotencyKeyDeduplication() {
        // Given
        String key = "LOAN_DISBURSAL:LOAN-9999";
        JournalEntryCommand command = createSampleCommand("LOAN-9999", new BigDecimal("3000000"));
        JournalOutboxEvent event1 = JournalOutboxEvent.createPending("LOAN", "LOAN_DISBURSAL", "LOAN-9999", command, key);

        outboxAdapter.saveJournalEvent(event1);

        // When
        boolean exists = outboxAdapter.findJournalEventByIdempotencyKey(key).isPresent();

        // Then
        assertThat(exists).isTrue();
    }

    private JournalEntryCommand createSampleCommand(String accountNo, BigDecimal amount) {
        return new JournalEntryCommand(
                LocalDate.now(),
                LocalDate.now(),
                "Sample entry " + accountNo,
                "TEST_ENTRY",
                "KRW",
                null,
                "SYSTEM",
                "SYSTEM",
                "TEST_MODULE",
                accountNo,
                List.of(
                        new JournalLineCommand("DEBIT", "1110", amount, amount, null, "CUST-1", "Debit line"),
                        new JournalLineCommand("CREDIT", "2010", amount, amount, null, "CUST-1", "Credit line")
                )
        );
    }

    private static class MockJournalPostingPort implements JournalPostingPort {
        private final AtomicInteger callCount = new AtomicInteger(0);
        private int failTimes = 0;
        private long sequence = 100L;

        public void setFailTimes(int failTimes) {
            this.failTimes = failTimes;
        }

        @Override
        public JournalPostingResult createDraftEntry(JournalEntryCommand command) {
            int currentCall = callCount.incrementAndGet();
            if (currentCall <= failTimes) {
                throw new RuntimeException("Network Error: Connection refused to Journal Service");
            }
            long id = sequence++;
            return new JournalPostingResult(id, "SLIP-TEST-" + id, "DRAFT");
        }
    }
}
