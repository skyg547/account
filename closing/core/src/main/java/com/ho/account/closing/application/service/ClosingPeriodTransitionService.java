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

/** Coordinates committed local intent with the non-transactional Journal and Master operations. */
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
        return releaseIfPending(transactions.reconcile(calendarId, operationId,
                remoteRequestTerminated, recoveredBy.trim()));
    }

    @Override
    public ClosingCalendar cancelPreparedClose(Long calendarId, String operationId, String reason, String actor) {
        return releaseAndFinish(transactions.cancelPreparedClose(calendarId, operationId, reason, actor));
    }

    private ClosingCalendar dispatch(ClosingCalendar calendar, String recoveryActor) {
        String operationId = calendar.getTransitionId();
        try {
            if ("CLOSED".equals(calendar.getTransitionTarget())) {
                JournalPeriodFenceCommand command = transactions.requirePreparedFence(calendar.getId(), operationId);
                requireJournal();
                // No calendar lock is held while Journal drains monetary transactions.
                requireResult(command, journal.fence(command), JournalPeriodFenceResult.State.ACTIVE);
            }
            transactions.markDispatched(calendar.getId(), operationId);
        } catch (FinalCloseEvidenceValidationException rejection) {
            // PREPARED remains durable and no Master write occurred; expose the actual evidence failure.
            throw rejection;
        } catch (RuntimeException failure) {
            throw new ClosingTransitionPendingException(operationId, failure);
        }
        try {
            return releaseIfPending(transactions.dispatchAndFinish(calendar.getId(), operationId, recoveryActor));
        } catch (RuntimeException failure) {
            // Master dispatch or the later release may be uncertain; their committed stage drives recovery.
            throw new ClosingTransitionPendingException(operationId, failure);
        }
    }

    private ClosingCalendar releaseIfPending(ClosingCalendar calendar) {
        return calendar.getTransitionStage() == TransitionStage.RELEASE_PENDING ? releaseAndFinish(calendar) : calendar;
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
}
