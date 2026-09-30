package com.ho.account.journalledger.application.service.journal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.application.port.in.KafkaJournalEventUseCase;
import com.ho.account.journalledger.application.port.out.JournalEventPayloadCodec;
import com.ho.account.journalledger.application.port.out.JournalEventQuarantinePort;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEventQuarantine;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
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
        Map<String, Object> event = Map.of("ruleCode", "NO_RULE", "amount", "10.00");
        when(journals.createJournalEntryFromEvent(event, LocalDate.of(2026, 9, 28)))
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
        Map<String, Object> decoded = Map.of("ruleCode", "NO_RULE");
        when(quarantinePort.findByIdForUpdate(1L)).thenReturn(Optional.of(quarantine));
        when(codec.decode(quarantine.getPayloadJson())).thenReturn(decoded);
        when(journals.createJournalEntryFromEvent(decoded, quarantine.getAccountingDate()))
                .thenReturn(Optional.empty());

        var result = service.replay(1L, " accounting-admin ");

        assertThat(result.replayed()).isFalse();
        assertThat(quarantine.getReplayAttempts()).isOne();
        assertThat(quarantine.getLastReplayActor()).isEqualTo("accounting-admin");
        verify(quarantinePort).save(quarantine);
    }

    @Test
    void repairedRuleAtomicallyLinksCreatedJournalToLockedQuarantine() {
        JournalEventQuarantine quarantine = JournalEventQuarantine.unmatched(
                "transaction-events", 0, 1L, "{\"ruleCode\":\"RULE\"}",
                LocalDate.of(2026, 9, 28), Instant.parse("2026-09-28T00:00:00Z"));
        JournalEntry journal = new JournalEntry();
        journal.setId(99L);
        Map<String, Object> decoded = Map.of("ruleCode", "RULE");
        when(quarantinePort.findByIdForUpdate(1L)).thenReturn(Optional.of(quarantine));
        when(codec.decode(quarantine.getPayloadJson())).thenReturn(decoded);
        when(journals.createJournalEntryFromEvent(decoded, quarantine.getAccountingDate()))
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
