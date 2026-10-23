package com.ho.account.journalledger.application.service.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.application.port.in.JournalCreationIdentity;
import com.ho.account.journalledger.application.port.in.KafkaJournalEventUseCase;
import com.ho.account.journalledger.application.port.out.JournalCreationOperationPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalEventPayloadCodec;
import com.ho.account.journalledger.application.port.out.JournalEventQuarantinePort;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.JournalReversalPersistencePort;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalCreationOperation;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantine;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KafkaJournalEventServiceTest {

    @Mock JournalUseCase journals;
    @Mock JournalEventQuarantinePort quarantinePort;
    @Mock JournalEventPayloadCodec codec;
    private KafkaJournalEventService service;

    @BeforeEach
    void setUp() {
        service = new KafkaJournalEventService(journals, quarantinePort, codec);
    }

    @Test
    void noRulePersistsBrokerIdentifiedCompletenessException() {
        Map<String, Object> event = Map.of("ruleCode", "NO_RULE", "eventId", "EVENT-769", "journalOperationKey", "EVENT-769:v1", "amount", "10.00");
        when(journals.createJournalEntryFromEvent(JournalCreationIdentity.fromEvent(event), event, LocalDate.of(2026, 9, 28)))
                .thenReturn(Optional.empty());
        when(codec.encode(event)).thenReturn("{\"ruleCode\":\"NO_RULE\"}");
        when(quarantinePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.process(
                new KafkaJournalEventUseCase.BrokerRecord("transaction-events", 3, 769L),
                event,
                LocalDate.of(2026, 9, 28));

        assertThat(result.disposition())
                .isEqualTo(KafkaJournalEventUseCase.ProcessingResult.Disposition.QUARANTINED);
        ArgumentCaptor<JournalEventQuarantine> saved = ArgumentCaptor.forClass(JournalEventQuarantine.class);
        verify(quarantinePort).save(saved.capture());
        assertThat(saved.getValue().getSourceTopic()).isEqualTo("transaction-events");
        assertThat(saved.getValue().getSourcePartition()).isEqualTo(3);
        assertThat(saved.getValue().getSourceOffset()).isEqualTo(769L);
        assertThat(saved.getValue().getReasonCode()).isEqualTo("NO_MATCHING_RULE");
    }

    @Test
    void closedSourceDateFailureDoesNotBecomeDurableNoRuleQuarantine() {
        LocalDate sourceDate = LocalDate.of(2026, 8, 31);
        Map<String, Object> event = Map.of("ruleCode", "SOURCE_EVENT", "eventId", "EVENT-773", "journalOperationKey", "EVENT-773:v1");
        when(journals.createJournalEntryFromEvent(JournalCreationIdentity.fromEvent(event), event, sourceDate))
                .thenThrow(new IllegalStateException("이미 마감된 기간입니다."));

        assertThatThrownBy(() -> service.process(
                        new KafkaJournalEventUseCase.BrokerRecord("transaction-events", 0, 773L),
                        event, sourceDate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("이미 마감된 기간");

        verify(quarantinePort, never()).save(any());
        verify(codec, never()).encode(any());
    }

    @Test
    void committedEventRedeliveryAfterRuleDeactivationReusesJournalWithoutQuarantine() {
        LocalDate date = LocalDate.of(2026, 9, 28);
        Map<String, Object> event = Map.of(
                "ruleCode", "PAYABLE", "lineageSourceType", "PAYABLE",
                "lineageSourceId", "INV-765", "journalOperationKey", "INV-765:v1",
                "amount", 10);
        JournalCreationIdentity identity = JournalCreationIdentity.fromEvent(event);
        JournalEntry journal = new JournalEntry();
        journal.setId(99L);
        journal.setSlipDate(date);
        journal.setSlipNo("SOURCE-99");
        journal.setCreatedBy("service:journal-kafka-maker");
        journal.setLineageSourceType(identity.sourceType());
        journal.setLineageSourceId(identity.sourceId());

        JournalPersistencePort persistence = mock(JournalPersistencePort.class);
        JournalCreationOperationPersistencePort operations = mock(JournalCreationOperationPersistencePort.class);
        JournalRuleEngine rules = mock(JournalRuleEngine.class);
        AtomicReference<JournalCreationOperation> receipt = new AtomicReference<>();
        AtomicBoolean active = new AtomicBoolean(true);
        when(operations.findByIdentity(identity)).thenAnswer(ignored -> Optional.ofNullable(receipt.get()));
        when(operations.save(any())).thenAnswer(invocation -> {
            JournalCreationOperation operation = invocation.getArgument(0);
            receipt.set(operation);
            return operation;
        });
        when(rules.generateJournalEntry(event, date)).thenAnswer(
                ignored -> active.get() ? Optional.of(journal) : Optional.empty());
        when(persistence.save(journal)).thenReturn(journal);
        when(persistence.findByIdWithDetailsWithoutLock(99L)).thenReturn(Optional.of(journal));
        JournalEntryService realJournals = new JournalEntryService(
                persistence, operations, mock(JournalReversalPersistencePort.class), rules,
                mock(PostingService.class), mock(JournalValidationEngine.class));
        KafkaJournalEventService realKafka = new KafkaJournalEventService(realJournals, quarantinePort, codec);

        var first = realKafka.process(new KafkaJournalEventUseCase.BrokerRecord("events", 0, 1L), event, date);
        active.set(false);
        var retry = realKafka.process(new KafkaJournalEventUseCase.BrokerRecord("events", 0, 2L), event, date);
        assertThat(first.disposition()).isEqualTo(KafkaJournalEventUseCase.ProcessingResult.Disposition.JOURNAL_CREATED);
        assertThat(retry.disposition()).isEqualTo(KafkaJournalEventUseCase.ProcessingResult.Disposition.JOURNAL_CREATED);
        assertThat(retry.journalEntryId()).isEqualTo(99L);

        Map<String, Object> changed = new HashMap<>(event);
        changed.put("amount", 11);
        assertThatThrownBy(() -> realKafka.process(
                new KafkaJournalEventUseCase.BrokerRecord("events", 0, 3L), changed, date))
                .isInstanceOf(JournalCreationConflictException.class);
        verify(rules, times(1)).generateJournalEntry(any(), any());
        verify(persistence, times(1)).save(any());
        verify(quarantinePort, never()).save(any());
        verify(codec, never()).encode(any());
    }

    @Test
    void duplicateBrokerCoordinateDoesNotEvaluateOrPersistAgain() {
        JournalEventQuarantine existing = JournalEventQuarantine.unmatched(
                "transaction-events", 3, 769L, "{}",
                LocalDate.of(2026, 9, 28), Instant.parse("2026-09-28T00:00:00Z"));
        when(quarantinePort.findByBrokerRecord("transaction-events", 3, 769L))
                .thenReturn(Optional.of(existing));

        var result = service.process(
                new KafkaJournalEventUseCase.BrokerRecord("transaction-events", 3, 769L),
                Map.of("ruleCode", "NOW_REPAIRED"),
                LocalDate.of(2026, 9, 28));

        assertThat(result.disposition())
                .isEqualTo(KafkaJournalEventUseCase.ProcessingResult.Disposition.ALREADY_QUARANTINED);
        verify(journals, never()).createJournalEntryFromEvent(any(), any());
        verify(quarantinePort, never()).save(any());
    }

    @Test
    void replayWithoutRepairedRuleRemainsVisibleAndRecordsAttempt() {
        JournalEventQuarantine quarantine = JournalEventQuarantine.unmatched(
                "transaction-events", 0, 1L, "{\"ruleCode\":\"NO_RULE\"}",
                LocalDate.of(2026, 9, 28), Instant.parse("2026-09-28T00:00:00Z"));
        Map<String, Object> decoded = Map.of("ruleCode", "NO_RULE", "eventId", "EVENT-1", "journalOperationKey", "EVENT-1:v1");
        when(quarantinePort.findByIdForUpdate(1L)).thenReturn(Optional.of(quarantine));
        when(codec.decode(quarantine.getPayloadJson())).thenReturn(decoded);
        when(journals.createJournalEntryFromEvent(JournalCreationIdentity.fromEvent(decoded), decoded, quarantine.getAccountingDate()))
                .thenReturn(Optional.empty());

        var result = service.replay(1L, " accounting-admin ");

        assertThat(result.replayed()).isFalse();
        assertThat(quarantine.getReplayAttempts()).isOne();
        assertThat(quarantine.getLastReplayActor()).isEqualTo("accounting-admin");
        verify(quarantinePort).save(quarantine);
    }

    @Test
    void replayUsesStoredSourceDateAcrossMonthAndYearBoundary() {
        LocalDate sourceDate = LocalDate.of(2026, 12, 31);
        JournalEventQuarantine quarantine = JournalEventQuarantine.unmatched(
                "transaction-events", 0, 2L, "{\"accountingDate\":\"2026-12-31\"}",
                sourceDate, Instant.parse("2027-01-01T00:01:00Z"));
        Map<String, Object> decoded = Map.of("accountingDate", "2026-12-31", "ruleCode", "YEAR_END", "eventId", "EVENT-2", "journalOperationKey", "EVENT-2:v1");
        when(quarantinePort.findByIdForUpdate(2L)).thenReturn(Optional.of(quarantine));
        when(codec.decode(quarantine.getPayloadJson())).thenReturn(decoded);
        when(journals.createJournalEntryFromEvent(JournalCreationIdentity.fromEvent(decoded), decoded, sourceDate)).thenReturn(Optional.empty());

        service.replay(2L, "accounting-admin");

        // Replay in the new year must reuse the date stored when the broker record was accepted.
        verify(journals).createJournalEntryFromEvent(JournalCreationIdentity.fromEvent(decoded), decoded, sourceDate);
        assertThat(quarantine.getAccountingDate()).isEqualTo(sourceDate);
    }

    @Test
    void repairedRuleAtomicallyLinksCreatedJournalToLockedQuarantine() {
        JournalEventQuarantine quarantine = JournalEventQuarantine.unmatched(
                "transaction-events", 0, 1L, "{\"ruleCode\":\"RULE\"}",
                LocalDate.of(2026, 9, 28), Instant.parse("2026-09-28T00:00:00Z"));
        JournalEntry journal = new JournalEntry();
        journal.setId(99L);
        Map<String, Object> decoded = Map.of("ruleCode", "RULE", "eventId", "EVENT-1", "journalOperationKey", "EVENT-1:v1");
        when(quarantinePort.findByIdForUpdate(1L)).thenReturn(Optional.of(quarantine));
        when(codec.decode(quarantine.getPayloadJson())).thenReturn(decoded);
        when(journals.createJournalEntryFromEvent(JournalCreationIdentity.fromEvent(decoded), decoded, quarantine.getAccountingDate()))
                .thenReturn(Optional.of(journal));

        var result = service.replay(1L, "accounting-admin");

        assertThat(result.replayed()).isTrue();
        assertThat(result.newlyCreated()).isTrue();
        assertThat(quarantine.getJournalEntryId()).isEqualTo(99L);
        verify(quarantinePort).findByIdForUpdate(1L);
        verify(quarantinePort).save(quarantine);
    }

    @Test
    void completenessUsesOneAggregateSnapshotFromTheOutputPort() {
        var oldest = Instant.parse("2026-09-28T00:00:00Z");
        when(quarantinePort.summarizeCompleteness()).thenReturn(
                new JournalEventQuarantinePort.CompletenessSnapshot(
                        2L, 5L, Optional.of(oldest)));

        var summary = service.completenessSummary();

        assertThat(summary.quarantinedCount()).isEqualTo(2L);
        assertThat(summary.replayedCount()).isEqualTo(5L);
        assertThat(summary.oldestUnresolvedAt()).contains(oldest);
        verify(quarantinePort).summarizeCompleteness();
    }
}
