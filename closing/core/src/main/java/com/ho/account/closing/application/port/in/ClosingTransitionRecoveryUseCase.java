package com.ho.account.closing.application.port.in;

import com.ho.account.closing.domain.ClosingCalendar;

public interface ClosingTransitionRecoveryUseCase {
    ClosingCalendar findTransition(Long calendarId);

    /** Confirmation attests external transport quiescence; it is not inferred from a Master GET. */
    ClosingCalendar recoverTransition(Long calendarId, String operationId,
            boolean remoteRequestTerminated, String recoveredBy);

    /** Cancel a never-dispatched final-close intent so a new immutable snapshot can be submitted. */
    ClosingCalendar cancelPreparedClose(Long calendarId, String operationId, String reason, String actor);
}
