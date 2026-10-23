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
        // The same operation ID is safe to retry even when the previous response was lost.
        return resume(calendarId, operationId, recoveredBy.trim());
    }

    @Override
    public ClosingCalendar cancelPreparedClose(Long calendarId, String operationId, String reason, String actor) {
        return transactions.cancelPreparedClose(calendarId, operationId, reason, actor);
    }

    @Override
    public ClosingCalendar cancelDispatchedClose(Long calendarId, String operationId, String reason, String actor) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 300) {
            throw new IllegalArgumentException("reason must be 1-300 non-blank characters");
        }
        transactions.cancelRemoteClose(calendarId, operationId, actor);
        try {
            return transactions.clearCancelledClose(calendarId, operationId, reason, actor);
        } catch (RuntimeException failure) {
            throw new ClosingTransitionPendingException(operationId, failure);
        }
    }

    private ClosingCalendar dispatch(ClosingCalendar calendar, String recoveryActor) {
        requireReceiptProtocol(calendar);
        String operationId = calendar.getTransitionId();
        try {
            transactions.markDispatched(calendar.getId(), operationId);
        } catch (FinalCloseEvidenceValidationException rejection) {
            // PREPARED remains durable and no Master write occurred; expose the actual evidence failure.
            throw rejection;
        } catch (RuntimeException failure) {
            throw new ClosingTransitionPendingException(operationId, failure);
        }
        return resume(calendar.getId(), operationId, recoveryActor);
    }

    private ClosingCalendar resume(Long calendarId, String operationId, String recoveryActor) {
        try {
            ClosingCalendar current = findTransition(calendarId);
            current.requireTransition(operationId);
            requireReceiptProtocol(current);
            if (current.getTransitionStage() == TransitionStage.DISPATCHED) {
                transactions.prepareRemote(calendarId, operationId);
                if ("OPEN".equals(current.getTransitionTarget())) {
                    transactions.authorizeReopen(calendarId, operationId);
                }
            }
            if (current.getTransitionStage() != TransitionStage.READY_TO_CONFIRM) {
                current = transactions.applyAndFinish(calendarId, operationId, recoveryActor);
            }
            if (current.getTransitionStage() == TransitionStage.READY_TO_CONFIRM) {
                return transactions.confirmAndFinishReopen(calendarId, operationId, recoveryActor);
            }
            return current;
        } catch (ClosingTransitionPendingException pending) {
            throw pending;
        } catch (RuntimeException failure) {
            throw new ClosingTransitionPendingException(operationId, failure);
        }
    }

    private void requireReceiptProtocol(ClosingCalendar calendar) {
        if (!"RECEIPT_V1".equals(calendar.getTransitionProtocol())) {
            throw new IllegalStateException(
                    "Legacy fiscal transition has no operation receipt provenance; operator preflight is required.");
        }
    }
}
