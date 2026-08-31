package com.ho.account.deposit.infrastructure.adapter.out.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.outbox.JournalOutboxEvent;
import com.ho.account.contracts.outbox.OutboxEvent;
import com.ho.account.contracts.outbox.OutboxStatus;
import com.ho.account.deposit.domain.DepositOutboxEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DepositTransactionalOutboxAdapterTest {

    @Mock
    private SpringDataDepositOutboxRepository repository;

    private ObjectMapper objectMapper;
    private DepositTransactionalOutboxAdapter adapter;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        adapter = new DepositTransactionalOutboxAdapter(repository, objectMapper);
    }

    @Test
    @DisplayName("전표 Outbox 이벤트를 JSON 직렬화하여 영속 엔티티로 저장한다")
    void saveJournalEventSerializesCommandAndSavesEntity() {
        JournalEntryCommand command = createSampleCommand("DEP-1001", new BigDecimal("50000.00"));
        JournalOutboxEvent event = JournalOutboxEvent.createPending(
                "DEPOSIT",
                "DEPOSIT_ACCOUNT",
                "DEP-1001",
                command,
                "DEPOSIT_ACCOUNT:DEP-1001"
        );

        when(repository.findByIdempotencyKey("DEPOSIT_ACCOUNT:DEP-1001")).thenReturn(Optional.empty());
        when(repository.save(any(DepositOutboxEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        JournalOutboxEvent saved = adapter.saveJournalEvent(event);

        assertThat(saved).isNotNull();
        assertThat(saved.getEventId()).isEqualTo(event.getEventId());
        assertThat(saved.getIdempotencyKey()).isEqualTo("DEPOSIT_ACCOUNT:DEP-1001");
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);

        ArgumentCaptor<DepositOutboxEntity> captor = ArgumentCaptor.forClass(DepositOutboxEntity.class);
        verify(repository).save(captor.capture());
        DepositOutboxEntity entity = captor.getValue();

        assertThat(entity.getEventId()).isEqualTo(event.getEventId());
        assertThat(entity.getSourceModule()).isEqualTo("DEPOSIT");
        assertThat(entity.getLineageSourceType()).isEqualTo("DEPOSIT_ACCOUNT");
        assertThat(entity.getLineageSourceId()).isEqualTo("DEP-1001");
        assertThat(entity.getPayload()).contains("DEP-1001");
        assertThat(entity.getPayload()).contains("DEPOSIT_INITIAL_DEPOSIT");
    }

    @Test
    @DisplayName("동일한 idempotencyKey를 갖는 이벤트 저장 시 중복 insert 없이 기존 이벤트를 반환한다 (Idempotency Guard)")
    void saveJournalEventDeduplicatesByIdempotencyKey() throws Exception {
        JournalEntryCommand command = createSampleCommand("DEP-2002", new BigDecimal("100000.00"));
        DepositOutboxEntity existingEntity = new DepositOutboxEntity();
        existingEntity.setEventId("EVT-EXISTING");
        existingEntity.setSourceModule("DEPOSIT");
        existingEntity.setLineageSourceType("DEPOSIT_ACCOUNT");
        existingEntity.setLineageSourceId("DEP-2002");
        existingEntity.setPayload(objectMapper.writeValueAsString(command));
        existingEntity.setStatus(OutboxStatus.PENDING);
        existingEntity.setCreatedAt(LocalDateTime.now());
        existingEntity.setIdempotencyKey("DEPOSIT_ACCOUNT:DEP-2002");

        when(repository.findByIdempotencyKey("DEPOSIT_ACCOUNT:DEP-2002")).thenReturn(Optional.of(existingEntity));

        JournalOutboxEvent duplicateEvent = JournalOutboxEvent.createPending(
                "DEPOSIT",
                "DEPOSIT_ACCOUNT",
                "DEP-2002",
                command,
                "DEPOSIT_ACCOUNT:DEP-2002"
        );

        JournalOutboxEvent result = adapter.saveJournalEvent(duplicateEvent);

        assertThat(result.getEventId()).isEqualTo("EVT-EXISTING");
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("PENDING 상태의 전표 Outbox 이벤트를 생성일시 오름차순으로 조회하고 역직렬화한다")
    void findPendingJournalEventsDeserializesPayload() throws Exception {
        JournalEntryCommand command = createSampleCommand("DEP-3003", new BigDecimal("30000.00"));
        DepositOutboxEntity entity = new DepositOutboxEntity();
        entity.setEventId("EVT-3003");
        entity.setSourceModule("DEPOSIT");
        entity.setLineageSourceType("DEPOSIT_ACCOUNT");
        entity.setLineageSourceId("DEP-3003");
        entity.setPayload(objectMapper.writeValueAsString(command));
        entity.setStatus(OutboxStatus.PENDING);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setIdempotencyKey("KEY-3003");

        when(repository.findByStatusInOrderByCreatedAtAsc(eq(List.of(OutboxStatus.PENDING)), any(Pageable.class)))
                .thenReturn(List.of(entity));

        List<JournalOutboxEvent> pending = adapter.findPendingJournalEvents(10);

        assertThat(pending).hasSize(1);
        JournalOutboxEvent event = pending.get(0);
        assertThat(event.getEventId()).isEqualTo("EVT-3003");
        assertThat(event.getCommand().entryType()).isEqualTo("DEPOSIT_INITIAL_DEPOSIT");
        assertThat(event.getCommand().lines()).hasSize(2);
    }

    @Test
    @DisplayName("markJournalEventAsPublished 호출 시 상태를 PUBLISHED로 전환하고 publishedAt을 기록한다")
    void markJournalEventAsPublishedTransitionsStatus() {
        DepositOutboxEntity entity = new DepositOutboxEntity();
        entity.setEventId("EVT-4004");
        entity.setStatus(OutboxStatus.PENDING);

        when(repository.findByEventId("EVT-4004")).thenReturn(Optional.of(entity));

        LocalDateTime now = LocalDateTime.now();
        adapter.markJournalEventAsPublished("EVT-4004", now);

        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(entity.getPublishedAt()).isEqualTo(now);
        assertThat(entity.getErrorMessage()).isNull();
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("이미 PUBLISHED 상태인 이벤트에 markJournalEventAsPublished 재호출 시 상태 변경 없이 무시한다 (Status Guard)")
    void markJournalEventAsPublishedIgnoresAlreadyPublished() {
        DepositOutboxEntity entity = new DepositOutboxEntity();
        entity.setEventId("EVT-5005");
        entity.setStatus(OutboxStatus.PUBLISHED);
        LocalDateTime originalTime = LocalDateTime.now().minusMinutes(5);
        entity.setPublishedAt(originalTime);

        when(repository.findByEventId("EVT-5005")).thenReturn(Optional.of(entity));

        adapter.markJournalEventAsPublished("EVT-5005", LocalDateTime.now());

        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.PUBLISHED);
        assertThat(entity.getPublishedAt()).isEqualTo(originalTime);
        verify(repository, never()).save(entity);
    }

    @Test
    @DisplayName("markJournalEventAsFailed 호출 시 재시도 횟수를 증가시키고 5회 이상 시 FAILED 상태로 전환한다")
    void markJournalEventAsFailedIncrementsRetryAndFailsAtMax() {
        DepositOutboxEntity entity = new DepositOutboxEntity();
        entity.setEventId("EVT-6006");
        entity.setStatus(OutboxStatus.PENDING);
        entity.setRetryCount(4);

        when(repository.findByEventId("EVT-6006")).thenReturn(Optional.of(entity));

        adapter.markJournalEventAsFailed("EVT-6006", "Connection Timeout to Journal Ledger");

        assertThat(entity.getRetryCount()).isEqualTo(5);
        assertThat(entity.getStatus()).isEqualTo(OutboxStatus.FAILED);
        assertThat(entity.getErrorMessage()).isEqualTo("Connection Timeout to Journal Ledger");
        verify(repository).save(entity);
    }

    @Test
    @DisplayName("일반 OutboxEvent 저장 및 조회가 정상 동작한다")
    void generalOutboxEventSaveAndQuery() {
        OutboxEvent event = OutboxEvent.createPending("DEPOSIT", "DEP-7007", "ACCOUNT_OPENED", "{\"balance\":1000}", "IDEM-7007");

        when(repository.findByIdempotencyKey("IDEM-7007")).thenReturn(Optional.empty());
        when(repository.save(any(DepositOutboxEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        OutboxEvent saved = adapter.save(event);

        assertThat(saved.getEventId()).isEqualTo(event.getEventId());
        assertThat(saved.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    private JournalEntryCommand createSampleCommand(String accountNo, BigDecimal amount) {
        return new JournalEntryCommand(
                LocalDate.now(),
                LocalDate.now(),
                "Initial deposit: " + accountNo,
                "DEPOSIT_INITIAL_DEPOSIT",
                "KRW",
                null,
                "SYSTEM",
                "SYSTEM",
                "DEPOSIT_ACCOUNT",
                accountNo,
                List.of(
                        new JournalLineCommand("DEBIT", "10100", amount, amount, null, "CUST-1", "Initial cash deposit"),
                        new JournalLineCommand("CREDIT", "20200", amount, amount, null, "CUST-1", "Deposit liability recognized")
                )
        );
    }
}
