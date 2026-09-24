package com.ho.account.closing.dto;

import com.ho.account.closing.domain.ClosingCalendar;
import java.time.LocalDateTime;

public record ClosingTransitionDto(Long calendarId, ClosingCalendar.ClosingCalendarStatus calendarStatus,
        String operationId, String target, ClosingCalendar.TransitionStage stage,
        Long fiscalPeriodId, Long approvalId, String decisionActor, LocalDateTime preparedAt) {
    public static ClosingTransitionDto from(ClosingCalendar calendar) {
        return new ClosingTransitionDto(calendar.getId(), calendar.getStatus(), calendar.getTransitionId(),
                calendar.getTransitionTarget(), calendar.getTransitionStage(), calendar.getTransitionFiscalPeriodId(),
                calendar.getTransitionApprovalId(), calendar.getTransitionActor(), calendar.getTransitionPreparedAt());
    }
}
