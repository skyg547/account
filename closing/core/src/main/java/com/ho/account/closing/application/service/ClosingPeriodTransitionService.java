package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.in.ClosingTransitionRecoveryUseCase;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.TransitionStage;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates committed local intent and the non-transactional remote Master operation. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.NEVER)
public class ClosingPeriodTransitionService implements ClosingTransitionRecoveryUseCase {
    private final ClosingTransitionTransactions transactions;
    private final ClosingCalendarPersistencePort calendars;

    public ClosingCalendar close(Long calendarId, String actor) {
        return dispatch(transactions.prepareClose(calendarId, actor), null);
    }

    public ReopenApproval decide(Long approvalId, ReopenApprovalStatus decision, String actor) {
        ClosingTransitionTransactions.PreparedDecision prepared = transactions.prepareDecision(approvalId, decision, actor);
        if (decision == ReopenApprovalStatus.APPROVED) {
            dispatch(prepared.calendar(), null);
        }
        return prepared.approval();
    }

    @Override
    @Transactional(readOnly = true)
    public ClosingCalendar findTransition(Long calendarId) {
        return calendars.findById(calendarId)
                .orElseThrow(() -> new EntityNotFoundException("ClosingCalendar not found with id: " + calendarId));
    }

    @Override
    public ClosingCalendar recoverTransition(Long calendarId, String operationId,
            boolean remoteRequestTerminated, String recoveredBy) {
        if (recoveredBy == null || recoveredBy.isBlank()) {
            throw new IllegalArgumentException("recoveredBy must not be blank");
        }
        ClosingCalendar calendar = calendars.findById(calendarId)
                .orElseThrow(() -> new EntityNotFoundException("ClosingCalendar not found with id: " + calendarId));
        calendar.requireTransition(operationId);
        if (calendar.getTransitionStage() == TransitionStage.PREPARED) {
            return dispatch(calendar, recoveredBy.trim());
        }
        return transactions.reconcile(calendarId, operationId, remoteRequestTerminated, recoveredBy.trim());
    }

    private ClosingCalendar dispatch(ClosingCalendar calendar, String recoveryActor) {
        String operationId = calendar.getTransitionId();
        try {
            transactions.markDispatched(calendar.getId(), operationId);
            return transactions.dispatchAndFinish(calendar.getId(), operationId, recoveryActor);
        } catch (RuntimeException failure) {
            throw new ClosingTransitionPendingException(operationId, failure);
        }
    }
}
