package com.ho.account.journalledger.adapter.in.kafka;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaOperations;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/** Explicit retry and dead-letter policy for journal Kafka listener failures. */
@Configuration(proxyBeanMethods = false)
public class JournalKafkaRecoveryConfiguration {

    @Bean
    DeadLetterPublishingRecoverer journalDeadLetterPublishingRecoverer(
            KafkaOperations<Object, Object> kafkaOperations,
            @Value("${journal-ledger.kafka.recovery.dlt-suffix:.DLT}") String dltSuffix) {
        if (dltSuffix == null || dltSuffix.isBlank()) {
            throw new IllegalArgumentException("Kafka DLT suffix must not be blank");
        }
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaOperations,
                (record, exception) -> new TopicPartition(
                        record.topic() + dltSuffix,
                        record.partition()));
        // A failed DLT publish must not acknowledge and lose the original economic event.
        recoverer.setFailIfSendResultIsError(true);
        return recoverer;
    }

    @Bean
    DefaultErrorHandler journalKafkaErrorHandler(
            DeadLetterPublishingRecoverer recoverer,
            @Value("${journal-ledger.kafka.recovery.retry-interval-ms:1000}") long retryIntervalMs,
            @Value("${journal-ledger.kafka.recovery.max-retries:2}") long maxRetries) {
        if (retryIntervalMs < 0 || retryIntervalMs > 60_000 || maxRetries < 0 || maxRetries > 10) {
            throw new IllegalArgumentException(
                    "Kafka retry interval must be 0..60000ms and max retries must be 0..10");
        }
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(retryIntervalMs, maxRetries));
        // Retain the failed record for bounded in-memory redelivery, then invoke the DLT recoverer.
        errorHandler.setSeekAfterError(false);
        errorHandler.setAckAfterHandle(true);
        return errorHandler;
    }
}
