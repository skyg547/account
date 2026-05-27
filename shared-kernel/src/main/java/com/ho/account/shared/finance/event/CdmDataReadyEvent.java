package com.ho.account.shared.finance.event;

import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
public class CdmDataReadyEvent {

    private final String eventId;
    private final LocalDate baseDate;
    private final String traceId;
    private final LocalDateTime occurredAt;

    public CdmDataReadyEvent(LocalDate baseDate, String traceId) {
        this.eventId = UUID.randomUUID().toString();
        this.baseDate = baseDate;
        this.traceId = traceId;
        this.occurredAt = LocalDateTime.now();
    }
}
