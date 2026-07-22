package com.ho.account.shared.finance.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CdmDataReadyEventTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void preservesProducerIdentityAcrossJsonRoundTrip() throws Exception {
        CdmDataReadyEvent event = new CdmDataReadyEvent(
                "event-20260722-1",
                LocalDate.of(2026, 7, 22),
                "trace-1",
                LocalDateTime.of(2026, 7, 22, 18, 0));

        CdmDataReadyEvent restored = objectMapper.readValue(
                objectMapper.writeValueAsString(event),
                CdmDataReadyEvent.class);

        assertThat(restored.getEventId()).isEqualTo(event.getEventId());
        assertThat(restored.getBaseDate()).isEqualTo(event.getBaseDate());
        assertThat(restored.getTraceId()).isEqualTo(event.getTraceId());
        assertThat(restored.getOccurredAt()).isEqualTo(event.getOccurredAt());
    }

    @Test
    void rejectsMissingIdempotencyFields() {
        assertThatThrownBy(() -> new CdmDataReadyEvent(
                " ",
                LocalDate.of(2026, 7, 22),
                "trace-1",
                LocalDateTime.of(2026, 7, 22, 18, 0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("eventId");
    }
}