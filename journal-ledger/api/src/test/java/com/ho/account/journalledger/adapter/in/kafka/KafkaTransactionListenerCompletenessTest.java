package com.ho.account.journalledger.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.journalledger.application.port.in.KafkaJournalEventUseCase;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class KafkaTransactionListenerCompletenessTest {

    @Test
    void invalidAccountingDatesFailBeforeDurableProcessing() {
        KafkaJournalEventUseCase useCase = Mockito.mock(KafkaJournalEventUseCase.class);
        KafkaTransactionListener listener = new KafkaTransactionListener(useCase);

        assertThatThrownBy(() -> listener.listenTransactionEvent(
                        Map.of("ruleCode", "SOURCE_EVENT"), "transaction-events", 0, 771L))
                .isInstanceOf(RuntimeException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class);
        for (Object invalid : new Object[] {"", "2026-02-30", "2026-09-28T00:00:00Z", 20260928}) {
            assertThatThrownBy(() -> listener.listenTransactionEvent(
                            Map.of("accountingDate", invalid), "transaction-events", 0, 771L))
                    .isInstanceOf(RuntimeException.class)
                    .hasCauseInstanceOf(IllegalArgumentException.class);
        }
        verifyNoInteractions(useCase);
    }

    @Test
    void explicitSourceDateSurvivesCalendarBoundaries() {
        KafkaJournalEventUseCase useCase = Mockito.mock(KafkaJournalEventUseCase.class);
        when(useCase.process(any(), any(), any())).thenReturn(
                new KafkaJournalEventUseCase.ProcessingResult(
                        KafkaJournalEventUseCase.ProcessingResult.Disposition.QUARANTINED,
                        null,
                        19L));
        KafkaTransactionListener listener = new KafkaTransactionListener(useCase);

        for (String sourceDate : new String[] {"2026-01-31", "2026-12-31", "2027-01-01"}) {
            listener.listenTransactionEvent(
                    Map.of("accountingDate", sourceDate, "ruleCode", "SOURCE_EVENT",
                            "eventId", "EVENT-772", "journalOperationKey", "EVENT-772:v1"),
                    "transaction-events", 0, 772L);
            verify(useCase).process(
                    new KafkaJournalEventUseCase.BrokerRecord("transaction-events", 0, 772L),
                    Map.of("accountingDate", sourceDate,
                            "createdBy", "service:journal-kafka-maker",
                            "auditUser", "service:journal-kafka-maker",
                            "ruleCode", "SOURCE_EVENT", "eventId", "EVENT-772",
                            "operationKey", "EVENT-772:v1",
                            "journalOperationKey", "EVENT-772:v1",
                            "lineageSourceType", "SOURCE_EVENT", "lineageSourceId", "EVENT-772"),
                    LocalDate.parse(sourceDate));
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void productionListenerAcknowledgesNoMatchOnlyAfterUseCaseReturnsDurableQuarantine() {
        KafkaJournalEventUseCase useCase = Mockito.mock(KafkaJournalEventUseCase.class);
        when(useCase.process(any(), any(), any())).thenReturn(
                new KafkaJournalEventUseCase.ProcessingResult(
                        KafkaJournalEventUseCase.ProcessingResult.Disposition.QUARANTINED,
                        null,
                        18L));
        KafkaTransactionListener listener = new KafkaTransactionListener(useCase);

        listener.listenTransactionEvent(
                Map.of("ruleCode", "NO_RULE", "eventId", "EVENT-769",
                        "journalOperationKey", "EVENT-769:v1", "accountingDate", "2026-09-28"),
                "transaction-events",
                3,
                769L);

        ArgumentCaptor<KafkaJournalEventUseCase.BrokerRecord> record =
                ArgumentCaptor.forClass(KafkaJournalEventUseCase.BrokerRecord.class);
        ArgumentCaptor<Map<String, Object>> event = ArgumentCaptor.forClass(Map.class);
        verify(useCase).process(record.capture(), event.capture(), any(LocalDate.class));
        assertThat(record.getValue()).isEqualTo(
                new KafkaJournalEventUseCase.BrokerRecord("transaction-events", 3, 769L));
        assertThat(event.getValue())
                .containsEntry("createdBy", "service:journal-kafka-maker")
                .containsEntry("auditUser", "service:journal-kafka-maker");
    }

    @Test
    void productionListenerPropagatesProcessingFailureToConfiguredErrorHandler() {
        KafkaJournalEventUseCase useCase = Mockito.mock(KafkaJournalEventUseCase.class);
        when(useCase.process(any(), any(), any()))
                .thenThrow(new IllegalStateException("database unavailable"));
        KafkaTransactionListener listener = new KafkaTransactionListener(useCase);

        assertThatThrownBy(() -> listener.listenTransactionEvent(
                        Map.of("accountingDate", "2026-09-28", "ruleCode", "SOURCE_EVENT",
                                "eventId", "EVENT-770", "journalOperationKey", "EVENT-770:v1"),
                        "transaction-events",
                        0,
                        770L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("retry/DLQ")
                .hasRootCauseMessage("database unavailable");
    }
}
