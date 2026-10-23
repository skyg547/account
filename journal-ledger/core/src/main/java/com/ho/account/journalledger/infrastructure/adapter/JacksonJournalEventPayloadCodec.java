package com.ho.account.journalledger.infrastructure.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.journalledger.application.port.out.JournalEventPayloadCodec;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** JSON payload adapter used only at the durable quarantine boundary. */
@Component
@RequiredArgsConstructor
public class JacksonJournalEventPayloadCodec implements JournalEventPayloadCodec {

    private static final TypeReference<Map<String, Object>> EVENT_TYPE = new TypeReference<>() { };

    private final ObjectMapper objectMapper;

    @Override
    public String encode(Map<String, Object> event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalArgumentException("Kafka journal event cannot be serialized for quarantine", exception);
        }
    }

    @Override
    public Map<String, Object> decode(String payloadJson) {
        try {
            return objectMapper.readValue(payloadJson, EVENT_TYPE);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Quarantined journal event payload cannot be decoded", exception);
        }
    }
}
