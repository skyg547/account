package com.ho.account.journalledger.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.journalledger.application.port.in.KafkaJournalEventUseCase;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

class KafkaTransactionListenerCompletenessTest {

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
                Map.of("ruleCode", "NO_RULE", "accountingDate", "2026-09-28"),
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
                        Map.of("accountingDate", "2026-09-28"),
                        "transaction-events",
                        0,
                        770L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("retry/DLQ")
                .hasRootCauseMessage("database unavailable");
    }
}
