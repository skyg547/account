package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.in.ClosingTransitionRecoveryUseCase;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.TransitionStage;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import jakarta.persistence.EntityNotFoundException;
import com.ho.account.contracts.journal.JournalPeriodFencePort;
import com.ho.account.contracts.journal.JournalPeriodFenceCommand;
import com.ho.account.contracts.journal.JournalPeriodFenceResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Coordinates committed local intent and the non-transactional remote Master operation. */
@Service
@Transactional(propagation = Propagation.NEVER)
public class ClosingPeriodTransitionService implements ClosingTransitionRecoveryUseCase {
    private final ClosingTransitionTransactions transactions;
    private final ClosingCalendarPersistencePort calendars;
    private final JournalPeriodFencePort journal;

    @Autowired
    public ClosingPeriodTransitionService(ClosingTransitionTransactions transactions,
            ClosingCalendarPersistencePort calendars,
            @org.springframework.beans.factory.annotation.Qualifier("closingJournalPeriodFence") JournalPeriodFencePort journal) {
        this.transactions = transactions;
        this.calendars = calendars;
        this.journal = journal;
    }

    public ClosingPeriodTransitionService(ClosingTransitionTransactions transactions,
            ClosingCalendarPersistencePort calendars) {
        this(transactions, calendars, null);
    }

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
        if (calendar.getTransitionStage() == TransitionStage.CANCEL_RELEASE_PENDING
                || calendar.getTransitionStage() == TransitionStage.RELEASE_PENDING) {
            return releaseAndFinish(calendar, recoveredBy.trim());
        }
        // The same operation ID is safe to retry even when the previous response was lost.
        return resume(calendarId, operationId, recoveredBy.trim());
    }

    @Override
    public ClosingCalendar cancelPreparedClose(Long calendarId, String operationId, String reason, String actor) {
        return releaseAndFinish(transactions.cancelPreparedClose(calendarId, operationId, reason, actor));
    }

    @Override
    public ClosingCalendar cancelDispatchedClose(Long calendarId, String operationId, String reason, String actor) {
        if (reason == null || reason.isBlank() || reason.trim().length() > 300) {
            throw new IllegalArgumentException("reason must be 1-300 non-blank characters");
        }
        transactions.cancelRemoteClose(calendarId, operationId, actor);
        try {
            return releaseAndFinish(transactions.clearCancelledClose(calendarId, operationId, reason, actor));
        } catch (RuntimeException failure) {
            throw new ClosingTransitionPendingException(operationId, failure);
        }
    }

    private ClosingCalendar dispatch(ClosingCalendar calendar, String recoveryActor) {
        requireReceiptProtocol(calendar);
        String operationId = calendar.getTransitionId();
        try {
            if ("CLOSED".equals(calendar.getTransitionTarget())) {
                JournalPeriodFenceCommand command = transactions.requirePreparedFence(calendar.getId(), operationId);
                requireJournal();
                // Journal drains monetary transactions without holding the local calendar lock.
                requireResult(command, journal.fence(command), JournalPeriodFenceResult.State.ACTIVE);
            }
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
            if (current.getTransitionStage() == TransitionStage.RELEASE_PENDING
                    || current.getTransitionStage() == TransitionStage.CANCEL_RELEASE_PENDING) {
                return releaseAndFinish(current, recoveryActor);
            }
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
                current = transactions.confirmAndFinishReopen(calendarId, operationId, recoveryActor);
            }
            return current.getTransitionStage() == TransitionStage.RELEASE_PENDING
                    ? releaseAndFinish(current, recoveryActor) : current;
        } catch (ClosingTransitionPendingException pending) {
            throw pending;
        } catch (RuntimeException failure) {
            throw new ClosingTransitionPendingException(operationId, failure);
        }
    }

    private ClosingCalendar releaseAndFinish(ClosingCalendar calendar) {
        return releaseAndFinish(calendar, null);
    }

    private ClosingCalendar releaseAndFinish(ClosingCalendar calendar, String recoveryActor) {
        String operationId = calendar.getTransitionId();
        try {
            requireJournal();
            calendar.requireJournalFence();
            JournalPeriodFenceCommand command = calendar.getJournalFenceCommand();
            requireResult(command, journal.release(command), JournalPeriodFenceResult.State.RELEASED);
            return calendar.getTransitionStage() == TransitionStage.CANCEL_RELEASE_PENDING
                    ? transactions.finishCancellation(calendar.getId(), operationId, recoveryActor)
                    : transactions.finishJournalRelease(calendar.getId(), operationId, recoveryActor);
        } catch (RuntimeException failure) {
            throw new ClosingTransitionPendingException(operationId, failure);
        }
    }

    private void requireJournal() {
        if (journal == null) throw new IllegalStateException("mandatory Journal fence integration is unavailable");
    }

    private static void requireResult(JournalPeriodFenceCommand command, JournalPeriodFenceResult result,
            JournalPeriodFenceResult.State expected) {
        if (result == null || !command.equals(result.command()) || result.state() != expected) {
            throw new IllegalStateException("Journal returned a mismatched fence acknowledgement");
        }
    }

    private void requireReceiptProtocol(ClosingCalendar calendar) {
        if (!"RECEIPT_V1".equals(calendar.getTransitionProtocol())) {
            throw new IllegalStateException(
                    "Legacy fiscal transition has no operation receipt provenance; operator preflight is required.");
        }
    }
}
