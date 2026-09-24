package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.ClosingAggregatePersistencePort;
import com.ho.account.closing.application.port.out.ClosingAuditLogPersistencePort;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.ReopenApprovalPersistencePort;
import com.ho.account.closing.domain.ClosingAuditLog;
import com.ho.account.closing.domain.ClosingAuditLog.ActionType;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingCalendar.TransitionStage;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import jakarta.persistence.EntityNotFoundException;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Each public phase commits independently, before the coordinator enters the next phase. */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ClosingTransitionTransactions {
    private final ClosingAggregatePersistencePort aggregates;
    private final ClosingCalendarPersistencePort calendars;
    private final ReopenApprovalPersistencePort approvals;
    private final ClosingAuditLogPersistencePort audits;
    private final FiscalPeriodControlPort master;

    public ClosingCalendar start(Long calendarId, String actor) {
        ClosingCalendar calendar = lock(calendarId);
        calendar.requireNoTransition();
        String previous = calendar.getStatus().name();
        calendar.start(actor);
        audits.save(ClosingAuditLog.create(calendar, ActionType.CALENDAR_IN_PROGRESS,
                previous, calendar.getStatus().name(), actor, "Status updated by user"));
        return calendars.save(calendar);
    }

    public ClosingCalendar prepareClose(Long calendarId, String actor) {
        ClosingCalendar calendar = lock(calendarId);
        calendar.requireNoTransition();
        calendar.validateReadyToClose(aggregates.refreshTasks(calendar), aggregates.refreshGates(calendar));
        FiscalPeriodRef period = master.findFiscalPeriod(calendar.getFiscalYear(), calendar.getFiscalPeriod())
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        requirePeriod(calendar, period, "OPEN");
        calendar.prepareTransition("CLOSED", period.id(), null, actor);
        savePrepared(calendar);
        return calendar;
    }

    public PreparedDecision prepareDecision(Long approvalId, ReopenApprovalStatus decision, String actor) {
        Objects.requireNonNull(decision, "newStatus must not be null");
        if (decision != ReopenApprovalStatus.APPROVED && decision != ReopenApprovalStatus.REJECTED) {
            throw new IllegalStateException("Reopen decision must be APPROVED or REJECTED.");
        }
        Long periodId = aggregates.findApprovalFiscalPeriodId(approvalId)
                .orElseThrow(() -> new EntityNotFoundException("ReopenApproval not found"));
        // This first read only locates the immutable aggregate key. Status is read again under its lock.
        FiscalPeriodRef identity = master.findFiscalPeriodById(periodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        requirePeriodId(periodId, identity);
        ClosingCalendar calendar = aggregates.lockCalendar(identity.fiscalYear(), identity.fiscalPeriod())
                .orElseThrow(() -> new EntityNotFoundException("ClosingCalendar not found"));
        calendar.requireNoTransition();
        ReopenApproval approval = aggregates.refreshApproval(approvalId)
                .orElseThrow(() -> new EntityNotFoundException("ReopenApproval not found"));
        if (!periodId.equals(approval.getFiscalPeriodId())) {
            throw new IllegalStateException("Reopen request fiscal period changed.");
        }
        if (decision == ReopenApprovalStatus.APPROVED) {
            approval.approve(actor);
        } else {
            approval.reject(actor);
        }
        FiscalPeriodRef period = master.findFiscalPeriodById(periodId)
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        requirePeriodId(periodId, period);
        requirePeriod(calendar, period, "CLOSED");
        if (calendar.getStatus() != ClosingCalendarStatus.CLOSED) {
            throw new IllegalStateException("Only a CLOSED closing calendar can be reopened.");
        }
        approval.assignFiscalPeriod(period.id(), period.fiscalYear(), period.fiscalPeriod());
        approvals.save(approval);
        if (decision == ReopenApprovalStatus.APPROVED) {
            calendar.prepareTransition("OPEN", periodId, approvalId, actor);
            savePrepared(calendar);
        } else {
            audits.save(ClosingAuditLog.create(calendar, ActionType.REOPEN_REJECTED,
                    "REOPEN_PENDING", "CLOSED", actor, "Reopen rejected"));
        }
        return new PreparedDecision(approval, calendar);
    }

    public void markDispatched(Long calendarId, String operationId) {
        ClosingCalendar calendar = lock(calendarId);
        calendar.markTransitionDispatched(operationId);
        calendars.save(calendar);
    }

    public ClosingCalendar dispatchAndFinish(Long calendarId, String operationId, String recoveryActor) {
        ClosingCalendar calendar = lock(calendarId);
        calendar.requireTransition(operationId);
        if (calendar.getTransitionStage() != TransitionStage.DISPATCHED) {
            throw new IllegalStateException("Fiscal period transition has not been dispatched.");
        }
        // DISPATCHED committed before this call. A rollback here must never make the PUT replayable.
        // Holding the root lock also prevents a recovery call overtaking an active synchronous PUT.
        FiscalPeriodRef result = master.updateClosingStatus(calendar.getTransitionFiscalPeriodId(),
                calendar.getTransitionTarget(), calendar.getTransitionActor());
        requireTarget(calendar, result);
        return finish(calendar, operationId, recoveryActor, false);
    }

    public ClosingCalendar reconcile(Long calendarId, String operationId,
            boolean remoteRequestTerminated, String recoveredBy) {
        requireActor(recoveredBy);
        ClosingCalendar calendar = lock(calendarId);
        calendar.requireTransition(operationId);
        if (calendar.getTransitionStage() != TransitionStage.DISPATCHED) {
            throw new IllegalStateException("Only a dispatched transition needs reconciliation.");
        }
        if (!remoteRequestTerminated) {
            throw new IllegalStateException("Confirm the original remote request has terminated before recovery.");
        }
        FiscalPeriodRef observed = master.findFiscalPeriodById(calendar.getTransitionFiscalPeriodId())
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        // A source-state GET cannot prove a timed-out PUT will never arrive. Never replay or cancel it.
        // The privileged operator must establish transport quiescence AND the intended Master target.
        requireTarget(calendar, observed);
        return finish(calendar, operationId, recoveredBy, true);
    }

    private ClosingCalendar finish(ClosingCalendar calendar, String operationId, String recoveryActor, boolean reconciled) {
        String previous = calendar.getStatus().name();
        String target = calendar.getTransitionTarget();
        String originalActor = calendar.getTransitionActor();
        Long approvalId = calendar.getTransitionApprovalId();
        String evidence = transitionEvidence(calendar) + "; decisionActor=" + originalActor;
        if (approvalId != null) {
            ReopenApproval approval = aggregates.refreshApproval(approvalId)
                    .orElseThrow(() -> new EntityNotFoundException("ReopenApproval not found"));
            if (approval.getStatus() != ReopenApprovalStatus.APPROVED
                    || !Objects.equals(originalActor, approval.getApprovedBy())
                    || !Objects.equals(calendar.getTransitionFiscalPeriodId(), approval.getFiscalPeriodId())) {
                throw new IllegalStateException("Durable reopen decision no longer matches its transition.");
            }
        }
        calendar.finishTransition(operationId);
        audits.save(ClosingAuditLog.create(calendar,
                "OPEN".equals(target) ? ActionType.REOPEN_APPROVED : ActionType.CALENDAR_CLOSED,
                previous, target, originalActor, evidence));
        if (recoveryActor != null) {
            audits.save(ClosingAuditLog.create(calendar, ActionType.PERIOD_TRANSITION_RECOVERED,
                    previous, target, recoveryActor, evidence + (reconciled
                            ? "; original request termination confirmed" : "; prepared operation resumed before dispatch")));
        }
        return calendars.save(calendar);
    }

    private void savePrepared(ClosingCalendar calendar) {
        calendars.save(calendar);
        audits.save(ClosingAuditLog.create(calendar, ActionType.PERIOD_TRANSITION_PREPARED,
                calendar.getStatus().name(), "PENDING_" + calendar.getTransitionTarget(),
                calendar.getTransitionActor(), transitionEvidence(calendar)));
    }

    private String transitionEvidence(ClosingCalendar calendar) {
        return "operationId=" + calendar.getTransitionId()
                + "; fiscalPeriodId=" + calendar.getTransitionFiscalPeriodId()
                + "; approvalId=" + calendar.getTransitionApprovalId();
    }

    private ClosingCalendar lock(Long id) {
        return aggregates.lockCalendar(id)
                .orElseThrow(() -> new EntityNotFoundException("ClosingCalendar not found with id: " + id));
    }

    private void requireTarget(ClosingCalendar calendar, FiscalPeriodRef result) {
        requirePeriod(calendar, result, calendar.getTransitionTarget());
        requirePeriodId(calendar.getTransitionFiscalPeriodId(), result);
    }

    private void requirePeriodId(Long expectedId, FiscalPeriodRef result) {
        if (result == null || !Objects.equals(expectedId, result.id())) {
            throw new IllegalStateException("Master returned another fiscal period; transition remains fenced.");
        }
    }

    private void requirePeriod(ClosingCalendar calendar, FiscalPeriodRef period, String status) {
        if (period == null || period.id() == null || period.id() <= 0
                || !Objects.equals(calendar.getFiscalYear(), period.fiscalYear())
                || !Objects.equals(calendar.getFiscalPeriod(), period.fiscalPeriod())
                || !status.equals(period.closingStatus())) {
            throw new IllegalStateException("Master fiscal period must be " + status + "; transition remains fenced.");
        }
    }

    private void requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("recoveredBy must not be blank");
        }
    }

    public record PreparedDecision(ReopenApproval approval, ClosingCalendar calendar) { }
}
