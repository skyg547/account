package com.ho.account.journalledger.adapter.in.kafka;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.MessageListenerContainer;
import org.springframework.kafka.support.SendResult;

class JournalKafkaRecoveryConfigurationTest {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void exhaustedRecordPublishesToSamePartitionDltWithOriginalValue() {
        KafkaOperations<Object, Object> operations = mock(KafkaOperations.class);
        when(operations.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(mock(SendResult.class)));
        var configuration = new JournalKafkaRecoveryConfiguration();
        var recoverer = configuration.journalDeadLetterPublishingRecoverer(operations, ".DLT");
        ConsumerRecord<String, Map<String, Object>> failed = new ConsumerRecord<>(
                "transaction-events", 4, 769L, "key-769", Map.of("amount", "100.00"));

        recoverer.accept(failed, null, new IllegalStateException("processing failed"));

        ArgumentCaptor<ProducerRecord> published = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(operations).send(published.capture());
        assertThat(published.getValue().topic()).isEqualTo("transaction-events.DLT");
        assertThat(published.getValue().partition()).isEqualTo(4);
        assertThat(published.getValue().key()).isEqualTo("key-769");
        assertThat(published.getValue().value()).isEqualTo(Map.of("amount", "100.00"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void errorHandlerRetriesTwiceThenInvokesRecovererOnThirdFailure() {
        DeadLetterPublishingRecoverer recoverer = mock(DeadLetterPublishingRecoverer.class);
        var handler = new JournalKafkaRecoveryConfiguration()
                .journalKafkaErrorHandler(recoverer, 0L, 2L);
        ConsumerRecord<String, String> failed = new ConsumerRecord<>(
                "transaction-events", 0, 769L, "key", "{}");
        Consumer consumer = mock(Consumer.class);
        MessageListenerContainer container = mock(MessageListenerContainer.class);
        IllegalStateException failure = new IllegalStateException("processing failed");

        assertThat(handler.handleOne(failure, failed, consumer, container)).isFalse();
        assertThat(handler.handleOne(failure, failed, consumer, container)).isFalse();
        assertThat(handler.handleOne(failure, failed, consumer, container)).isTrue();

        verify(recoverer, times(1)).accept(failed, consumer, failure);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void dltPublishFailureRemainsAnErrorInsteadOfAcknowledgingTheRecord() {
        KafkaOperations<Object, Object> operations = mock(KafkaOperations.class);
        when(operations.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.failedFuture(new IllegalStateException("broker unavailable")));
        var recoverer = new JournalKafkaRecoveryConfiguration()
                .journalDeadLetterPublishingRecoverer(operations, ".DLT");
        ConsumerRecord<String, String> failed = new ConsumerRecord<>(
                "transaction-events", 1, 770L, "key", "{}");

                org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        recoverer.accept(failed, null, new IllegalStateException("processing failed")))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Dead-letter publication")
                .hasRootCauseMessage("broker unavailable");
    }
}
