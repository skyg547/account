package com.ho.account.ecl.api.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.ecl.api.port.BatchAlreadyCompletedException;
import com.ho.account.ecl.api.port.BatchTriggerPort;
import com.ho.account.ecl.api.port.BatchTriggerResponse;
import com.ho.account.shared.finance.event.CdmDataReadyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CdmDataReadyConsumerTest {

    @Test
    void usesEventIdentityAsBatchTriggerIdentity() {
        BatchTriggerPort batchTriggerPort = mock(BatchTriggerPort.class);
        when(batchTriggerPort.triggerBatch(eq("allowanceEclJob"), anyMap()))
                .thenReturn(BatchTriggerResponse.success("allowanceEclJob", "EXT-1234"));
        CdmDataReadyConsumer consumer = new CdmDataReadyConsumer(batchTriggerPort);
        CdmDataReadyEvent event = event();

        consumer.handleCdmDataReady(event);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> parameters = ArgumentCaptor.forClass(Map.class);
        verify(batchTriggerPort).triggerBatch(eq("allowanceEclJob"), parameters.capture());
        assertThat(parameters.getValue().get("eventId")).isEqualTo("event-1");
        assertThat(parameters.getValue().get("baseDate")).isEqualTo("2026-07-22");
        assertThat(parameters.getValue().get("traceId")).isEqualTo("trace-1");
        assertThat(parameters.getValue()).doesNotContainKey("timestamp");
    }

    @Test
    void treatsAnAlreadyCompletedEventAsAnIdempotentDuplicate() {
        BatchTriggerPort batchTriggerPort = mock(BatchTriggerPort.class);
        when(batchTriggerPort.triggerBatch(eq("allowanceEclJob"), anyMap()))
                .thenThrow(new BatchAlreadyCompletedException("already complete"));
        CdmDataReadyConsumer consumer = new CdmDataReadyConsumer(batchTriggerPort);

        assertThatCode(() -> consumer.handleCdmDataReady(event())).doesNotThrowAnyException();
    }

    @Test
    void propagatesJobFailureSoKafkaCanApplyRetryPolicy() {
        BatchTriggerPort batchTriggerPort = mock(BatchTriggerPort.class);
        when(batchTriggerPort.triggerBatch(eq("allowanceEclJob"), anyMap()))
                .thenThrow(new RuntimeException("job failed"));
        CdmDataReadyConsumer consumer = new CdmDataReadyConsumer(batchTriggerPort);

        assertThatThrownBy(() -> consumer.handleCdmDataReady(event()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("event-1")
                .hasCauseInstanceOf(RuntimeException.class);
    }

    private CdmDataReadyEvent event() {
        return new CdmDataReadyEvent(
                "event-1",
                LocalDate.of(2026, 7, 22),
                "trace-1",
                LocalDateTime.of(2026, 7, 22, 18, 0));
    }
}
