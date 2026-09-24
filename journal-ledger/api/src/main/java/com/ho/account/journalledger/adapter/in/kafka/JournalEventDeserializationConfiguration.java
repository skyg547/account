package com.ho.account.journalledger.adapter.in.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.converter.RecordMessageConverter;
import org.springframework.kafka.support.converter.StringJsonMessageConverter;

/**
 * Configures Kafka JSON conversion with the application-wide Jackson settings.
 *
 * <p>Reusing Spring Boot's {@link ObjectMapper} preserves financial decimal values as
 * {@code BigDecimal} in untyped event maps; JSON integers remain normal integral types.</p>
 */
@Configuration(proxyBeanMethods = false)
public class JournalEventDeserializationConfiguration {

    @Bean
    RecordMessageConverter journalEventRecordMessageConverter(ObjectMapper objectMapper) {
        return new StringJsonMessageConverter(objectMapper);
    }
}
