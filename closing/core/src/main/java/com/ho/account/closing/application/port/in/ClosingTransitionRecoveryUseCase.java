package com.ho.account.closing.application.port.in;

import com.ho.account.closing.domain.ClosingCalendar;

public interface ClosingTransitionRecoveryUseCase {
    ClosingCalendar findTransition(Long calendarId);

    /** Legacy boolean is ignored; an exact Master operation receipt governs recovery. */
    ClosingCalendar recoverTransition(Long calendarId, String operationId,
            boolean remoteRequestTerminated, String recoveredBy);

    /** Cancel a never-dispatched final-close intent so a new immutable snapshot can be submitted. */
    ClosingCalendar cancelPreparedClose(Long calendarId, String operationId, String reason, String actor);

    /** Release a dispatched close only after Master durably confirms CANCELLED for the same operation. */
    ClosingCalendar cancelDispatchedClose(Long calendarId, String operationId, String reason, String actor);
}
