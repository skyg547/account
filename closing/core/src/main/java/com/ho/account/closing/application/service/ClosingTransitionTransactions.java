package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.out.ClosingAggregatePersistencePort;
import com.ho.account.closing.application.port.out.ClosingAuditLogPersistencePort;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.ClosingGatePersistencePort;
import com.ho.account.closing.application.port.out.ClosingTaskPersistencePort;
import com.ho.account.closing.application.port.out.ReopenApprovalPersistencePort;
import com.ho.account.closing.domain.ClosingAuditLog;
import com.ho.account.closing.domain.ClosingAuditLog.ActionType;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingCalendar.TransitionStage;
import com.ho.account.closing.domain.ClosingGate;
import com.ho.account.closing.domain.ClosingTask;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.domain.FinalCloseEvidenceSet;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodClosingStatus;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionCommand;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionReceipt;
import jakarta.persistence.EntityNotFoundException;
import java.util.Objects;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Each public phase commits independently, before the coordinator enters the next phase. */
@Service
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ClosingTransitionTransactions {
    private final ClosingAggregatePersistencePort aggregates;
    private final ClosingCalendarPersistencePort calendars;
    private final ReopenApprovalPersistencePort approvals;
    private final ClosingAuditLogPersistencePort audits;
    private final FiscalPeriodControlPort master;
    private final FinalCloseEvidenceService finalCloseEvidence;
    private final ClosingTaskPersistencePort tasks;
    private final ClosingGatePersistencePort gates;

    @Autowired
    public ClosingTransitionTransactions(
            ClosingAggregatePersistencePort aggregates,
            ClosingCalendarPersistencePort calendars,
            ReopenApprovalPersistencePort approvals,
            ClosingAuditLogPersistencePort audits,
            FiscalPeriodControlPort master,
            FinalCloseEvidenceService finalCloseEvidence,
            ClosingTaskPersistencePort tasks,
            ClosingGatePersistencePort gates) {
        this.aggregates = aggregates;
        this.calendars = calendars;
        this.approvals = approvals;
        this.audits = audits;
        this.master = master;
        this.finalCloseEvidence = finalCloseEvidence;
        this.tasks = tasks;
        this.gates = gates;
    }

    /** @deprecated Compatibility for old callers; reopen requires checklist persistence. */
    @Deprecated
    public ClosingTransitionTransactions(
            ClosingAggregatePersistencePort aggregates,
            ClosingCalendarPersistencePort calendars,
            ReopenApprovalPersistencePort approvals,
            ClosingAuditLogPersistencePort audits,
            FiscalPeriodControlPort master,
            FinalCloseEvidenceService finalCloseEvidence) {
        this(aggregates, calendars, approvals, audits, master, finalCloseEvidence, null, null);
    }

    /**
     * @deprecated Compatibility for callers that have not supplied the mandatory evidence gate.
     * Final close remains fail-closed.
     */
    @Deprecated
    public ClosingTransitionTransactions(
            ClosingAggregatePersistencePort aggregates,
            ClosingCalendarPersistencePort calendars,
            ReopenApprovalPersistencePort approvals,
            ClosingAuditLogPersistencePort audits,
            FiscalPeriodControlPort master) {
        this(aggregates, calendars, approvals, audits, master, null, null, null);
    }

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
        requireServiceTransitionActor(actor);
        ClosingCalendar calendar = lock(calendarId);
        calendar.requireNoTransition();
        calendar.validateReadyToClose(aggregates.refreshActiveTasks(calendar), aggregates.refreshActiveGates(calendar));
        FiscalPeriodRef period = master.findFiscalPeriod(calendar.getFiscalYear(), calendar.getFiscalPeriod())
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        requirePeriod(calendar, period, "OPEN");
        if (finalCloseEvidence == null) {
            throw new FinalCloseEvidenceValidationException("mandatory evidence service is unavailable");
        }
        // Master identity is authoritative; validate under the same calendar lock before any durable intent.
        FinalCloseEvidenceSet selected = finalCloseEvidence.requireForFinalClose(calendar, period);
        calendar.prepareTransition("CLOSED", period.id(), null, selected.evidenceSetId(), actor);
        savePrepared(calendar);
        return calendar;
    }

    public PreparedDecision prepareDecision(Long approvalId, ReopenApprovalStatus decision, String actor) {
        requireServiceTransitionActor(actor);
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
            calendar.prepareTransition("OPEN", periodId, approvalId, null, actor);
            savePrepared(calendar);
        } else {
            audits.save(ClosingAuditLog.create(calendar, ActionType.REOPEN_REJECTED,
                    "REOPEN_PENDING", "CLOSED", actor, "Reopen rejected"));
        }
        return new PreparedDecision(approval, calendar);
    }

    public void markDispatched(Long calendarId, String operationId) {
        ClosingCalendar calendar = lock(calendarId);
        calendar.requireTransition(operationId);
        if (calendar.getTransitionStage() != TransitionStage.PREPARED) {
            throw new IllegalStateException("A dispatched fiscal period transition cannot be replayed.");
        }
        if ("CLOSED".equals(calendar.getTransitionTarget())) {
            if (finalCloseEvidence == null) {
                throw new FinalCloseEvidenceValidationException("mandatory evidence service is unavailable");
            }
            FiscalPeriodRef period = master.findFiscalPeriodById(calendar.getTransitionFiscalPeriodId())
                    .orElseThrow(() -> new FinalCloseEvidenceValidationException(
                            "bound fiscal period does not exist in Master"));
            if (period.id() == null
                    || !Objects.equals(calendar.getTransitionFiscalPeriodId(), period.id())
                    || !Objects.equals(calendar.getFiscalYear(), period.fiscalYear())
                    || !Objects.equals(calendar.getFiscalPeriod(), period.fiscalPeriod())
                    || !"OPEN".equals(period.closingStatus())) {
                throw new FinalCloseEvidenceValidationException(
                        "bound fiscal period is no longer the OPEN calendar period");
            }
            // Under the calendar lock, validate the original immutable ID at the last local gate.
            // A newer snapshot may only be used by cancelling and preparing a new operation.
            finalCloseEvidence.requireBoundForFinalClose(
                    calendar, period, calendar.getTransitionEvidenceSetId());
        }
        calendar.markTransitionDispatched(operationId);
        calendars.save(calendar);
    }

    public ClosingCalendar cancelPreparedClose(Long calendarId, String operationId, String reason, String actor) {
        String normalizedActor = requireBoundedText(actor, "actor", 50);
        String normalizedReason = requireBoundedText(reason, "reason", 300);
        ClosingCalendar calendar = lock(calendarId);
        calendar.requireTransition(operationId);
        if (calendar.getStatus() != ClosingCalendarStatus.IN_PROGRESS
                || calendar.getTransitionStage() != TransitionStage.PREPARED
                || !"CLOSED".equals(calendar.getTransitionTarget())) {
            throw new IllegalStateException("Only a PREPARED final-close transition can be cancelled.");
        }
        FiscalPeriodRef period = master.findFiscalPeriodById(calendar.getTransitionFiscalPeriodId())
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        requirePeriodId(calendar.getTransitionFiscalPeriodId(), period);
        requirePeriod(calendar, period, "OPEN");
        String priorBinding = transitionEvidence(calendar);
        calendar.cancelPreparedClose(operationId, normalizedActor);
        ClosingCalendar saved = calendars.save(calendar);
        // This append keeps the original operation and evidence lineage after the intent is cleared.
        audits.save(ClosingAuditLog.create(saved, ActionType.PERIOD_TRANSITION_CANCELLED,
                "PENDING_CLOSED", saved.getStatus().name(), normalizedActor,
                priorBinding + "; reason=" + normalizedReason));
        return saved;
    }

    /** Master cancellation creates a terminal tombstone before the local fence can be removed. */
    public FiscalPeriodTransitionReceipt cancelRemoteClose(Long calendarId, String operationId, String cancellationActor) {
        requireServiceTransitionActor(cancellationActor);
        ClosingCalendar calendar = lock(calendarId);
        requireDispatchedClose(calendar, operationId);
        FiscalPeriodTransitionCommand command = command(calendar);
        FiscalPeriodTransitionReceipt receipt = master.findTransitionReceipt(operationId)
                .orElseThrow(() -> new IllegalStateException(
                        "Master receipt is missing; original prepare may still arrive."));
        requireReceipt(command, receipt);
        if (receipt.phase() == FiscalPeriodTransitionReceipt.Phase.PREPARED) {
            try {
                receipt = master.cancelClosingTransition(operationId, cancellationActor);
            } catch (RuntimeException uncertain) {
                throw new ClosingTransitionPendingException(operationId, uncertain);
            }
            requireReceipt(command, receipt);
        }
        if (receipt.phase() != FiscalPeriodTransitionReceipt.Phase.CANCELLED) {
            throw new IllegalStateException("Applied fiscal close cannot be cancelled.");
        }
        return receipt;
    }

    /** A lost DELETE response is recoverable by reading the exact CANCELLED receipt. */
    public ClosingCalendar clearCancelledClose(Long calendarId, String operationId, String reason, String actor) {
        String normalizedActor = requireBoundedText(actor, "actor", 50);
        String normalizedReason = requireBoundedText(reason, "reason", 300);
        ClosingCalendar calendar = lock(calendarId);
        requireDispatchedClose(calendar, operationId);
        FiscalPeriodTransitionReceipt receipt = master.findTransitionReceipt(operationId)
                .orElseThrow(() -> new IllegalStateException("Master cancellation receipt is missing."));
        requireReceipt(command(calendar), receipt);
        if (receipt.phase() != FiscalPeriodTransitionReceipt.Phase.CANCELLED) {
            throw new IllegalStateException("Master has not cancelled this fiscal close.");
        }
        FiscalPeriodRef observed = master.findFiscalPeriodById(calendar.getTransitionFiscalPeriodId())
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        requirePeriodId(calendar.getTransitionFiscalPeriodId(), observed);
        requirePeriod(calendar, observed, "OPEN");
        String priorBinding = transitionEvidence(calendar);
        calendar.cancelDispatchedClose(operationId, normalizedActor);
        ClosingCalendar saved = calendars.save(calendar);
        audits.save(ClosingAuditLog.create(saved, ActionType.PERIOD_TRANSITION_CANCELLED,
                "PENDING_CLOSED", saved.getStatus().name(), normalizedActor,
                priorBinding + "; Master receipt CANCELLED; reason=" + normalizedReason));
        return saved;
    }

    private void requireDispatchedClose(ClosingCalendar calendar, String operationId) {
        calendar.requireTransition(operationId);
        if (!"RECEIPT_V1".equals(calendar.getTransitionProtocol())
                || calendar.getStatus() != ClosingCalendarStatus.IN_PROGRESS
                || calendar.getTransitionStage() != TransitionStage.DISPATCHED
                || !"CLOSED".equals(calendar.getTransitionTarget())) {
            throw new IllegalStateException("Only a receipt-backed dispatched close can be cancelled.");
        }
    }

    /** Preparing the remote CAS is safe while local admission remains fenced. */
    public FiscalPeriodTransitionReceipt prepareRemote(Long calendarId, String operationId) {
        ClosingCalendar calendar = lock(calendarId);
        calendar.requireTransition(operationId);
        if (calendar.getTransitionStage() != TransitionStage.DISPATCHED) {
            throw new IllegalStateException("Fiscal period transition has not been dispatched.");
        }
        FiscalPeriodTransitionCommand command = command(calendar);
        FiscalPeriodTransitionReceipt receipt = master.findTransitionReceipt(operationId)
                .orElseGet(() -> master.prepareClosingTransition(command));
        requireReceipt(command, receipt);
        if (receipt.phase() == FiscalPeriodTransitionReceipt.Phase.CANCELLED) {
            throw new IllegalStateException("Master transition was cancelled; prepare a new operation.");
        }
        if (receipt.phase() == FiscalPeriodTransitionReceipt.Phase.APPLIED
                && "OPEN".equals(calendar.getTransitionTarget())) {
            // An OPEN provider outcome before durable local authorization is never reconciled silently.
            throw new IllegalStateException("Master OPEN preceded local reopen authorization.");
        }
        return receipt;
    }

    /** The whole local authorization commits before the separate Master OPEN apply. */
    public ClosingCalendar authorizeReopen(Long calendarId, String operationId) {
        ClosingCalendar calendar = lock(calendarId);
        calendar.requireTransition(operationId);
        if (calendar.getTransitionStage() == TransitionStage.AUTHORIZED) {
            return calendar;
        }
        if (calendar.getTransitionStage() != TransitionStage.DISPATCHED
                || !"OPEN".equals(calendar.getTransitionTarget())) {
            throw new IllegalStateException("Only a dispatched reopen can be authorized.");
        }
        FiscalPeriodTransitionCommand command = command(calendar);
        FiscalPeriodTransitionReceipt receipt = master.findTransitionReceipt(operationId)
                .orElseThrow(() -> new IllegalStateException("Remote transition preparation is missing."));
        requireReceipt(command, receipt);
        if (receipt.phase() != FiscalPeriodTransitionReceipt.Phase.PREPARED) {
            throw new IllegalStateException("Master OPEN preceded local reopen authorization.");
        }
        if (tasks == null || gates == null) {
            throw new IllegalStateException("Checklist persistence is required to reopen a close cycle.");
        }
        ReopenApproval approval = requireApprovedDecision(calendar);
        int previousCycle = calendar.getCycleNumber();
        List<ClosingTask> priorTasks = aggregates.refreshActiveTasks(calendar);
        List<ClosingGate> priorGates = aggregates.refreshActiveGates(calendar);
        calendar.authorizeReopen(operationId);
        // All four local effects roll back together on any save or commit failure.
        for (ClosingTask priorTask : priorTasks) {
            tasks.save(priorTask.nextCycle(calendar, calendar.getTransitionActor()));
        }
        for (ClosingGate priorGate : priorGates) {
            gates.save(priorGate.nextCycle(calendar, calendar.getTransitionActor()));
        }
        audits.save(ClosingAuditLog.create(calendar, ActionType.REOPEN_APPROVED,
                ClosingCalendarStatus.CLOSED.name(), ClosingCalendarStatus.OPEN.name(),
                approval.getApprovedBy(), transitionEvidence(calendar) + "; decisionActor="
                        + approval.getApprovedBy() + "; previousCycle=" + previousCycle
                        + "; activeCycle=" + calendar.getCycleNumber()));
        return calendars.save(calendar);
    }

    /** The remote apply is bound to the original operation and is idempotent at Master. */
    public ClosingCalendar applyAndFinish(Long calendarId, String operationId, String recoveryActor) {
        ClosingCalendar calendar = lock(calendarId);
        calendar.requireTransition(operationId);
        if (("OPEN".equals(calendar.getTransitionTarget())
                && calendar.getTransitionStage() != TransitionStage.AUTHORIZED)
                || ("CLOSED".equals(calendar.getTransitionTarget())
                && calendar.getTransitionStage() != TransitionStage.DISPATCHED)) {
            throw new IllegalStateException("Local transition is not authorized for remote apply.");
        }
        FiscalPeriodTransitionCommand command = command(calendar);
        FiscalPeriodTransitionReceipt receipt = master.findTransitionReceipt(operationId)
                .orElseThrow(() -> new IllegalStateException("Remote transition preparation is missing."));
        requireReceipt(command, receipt);
        if (receipt.phase() == FiscalPeriodTransitionReceipt.Phase.CANCELLED) {
            throw new IllegalStateException("Master transition was cancelled; prepare a new operation.");
        }
        if (receipt.phase() == FiscalPeriodTransitionReceipt.Phase.CONFIRMED) {
            throw new IllegalStateException("Master confirmation preceded local finalization.");
        }
        if (receipt.phase() != FiscalPeriodTransitionReceipt.Phase.APPLIED) {
            if ("CLOSED".equals(calendar.getTransitionTarget())) {
                FiscalPeriodRef beforeApply = master.findFiscalPeriodById(calendar.getTransitionFiscalPeriodId())
                        .orElseThrow(() -> new FinalCloseEvidenceValidationException(
                                "bound fiscal period does not exist in Master"));
                requirePeriodId(calendar.getTransitionFiscalPeriodId(), beforeApply);
                if (!Objects.equals(calendar.getFiscalYear(), beforeApply.fiscalYear())
                        || !Objects.equals(calendar.getFiscalPeriod(), beforeApply.fiscalPeriod())) {
                    throw new FinalCloseEvidenceValidationException("bound fiscal period identity changed");
                }
                // A failed prior apply may leave Journal able to post; recheck the immutable evidence
                // at every attempt. Master enforces the expected-state CAS at the following apply.
                finalCloseEvidence.requireBoundForFinalClose(
                        calendar, beforeApply, calendar.getTransitionEvidenceSetId());
            }
            receipt = master.applyClosingTransition(operationId);
            requireReceipt(command, receipt);
        }
        if (receipt.phase() != FiscalPeriodTransitionReceipt.Phase.APPLIED) {
            throw new IllegalStateException("Master transition was not applied.");
        }
        // Reopen APPLIED is still publicly CLOSED. Commit the final local marker before confirm can
        // expose OPEN to Journal; a failed local write leaves the Master read fence intact.
        FiscalPeriodRef observed = master.findFiscalPeriodById(calendar.getTransitionFiscalPeriodId())
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        if ("OPEN".equals(calendar.getTransitionTarget())) {
            requirePeriodId(calendar.getTransitionFiscalPeriodId(), observed);
            requirePeriod(calendar, observed, "CLOSED");
            requireApprovedDecision(calendar);
            calendar.readyToConfirmReopen(operationId);
            return calendars.save(calendar);
        }
        // A stale close receipt cannot clear the local fence after a later Master mutation.
        requireTarget(calendar, observed);
        return finish(calendar, operationId, recoveryActor);
    }

    /** Master exposes reopen OPEN only after this local READY_TO_CONFIRM marker has committed. */
    public ClosingCalendar confirmAndFinishReopen(Long calendarId, String operationId, String recoveryActor) {
        ClosingCalendar calendar = lock(calendarId);
        calendar.requireTransition(operationId);
        if (!"OPEN".equals(calendar.getTransitionTarget())
                || calendar.getTransitionStage() != TransitionStage.READY_TO_CONFIRM) {
            throw new IllegalStateException("Reopen has not durably finalized before confirmation.");
        }
        FiscalPeriodTransitionCommand command = command(calendar);
        FiscalPeriodTransitionReceipt receipt = master.findTransitionReceipt(operationId)
                .orElseThrow(() -> new IllegalStateException("Remote reopen receipt is missing."));
        requireReceipt(command, receipt);
        if (receipt.phase() == FiscalPeriodTransitionReceipt.Phase.APPLIED) {
            receipt = master.confirmClosingTransition(operationId);
            requireReceipt(command, receipt);
        }
        if (receipt.phase() != FiscalPeriodTransitionReceipt.Phase.CONFIRMED) {
            throw new IllegalStateException("Master reopen is not confirmed.");
        }
        FiscalPeriodRef observed = master.findFiscalPeriodById(calendar.getTransitionFiscalPeriodId())
                .orElseThrow(() -> new EntityNotFoundException("FiscalPeriod not found"));
        requireTarget(calendar, observed);
        return finish(calendar, operationId, recoveryActor);
    }

    private ClosingCalendar finish(ClosingCalendar calendar, String operationId, String recoveryActor) {
        String previous = calendar.getStatus().name();
        String target = calendar.getTransitionTarget();
        String originalActor = calendar.getTransitionActor();
        String evidence = transitionEvidence(calendar) + "; decisionActor=" + originalActor;
        if ("OPEN".equals(target)) requireApprovedDecision(calendar);
        calendar.finishTransition(operationId);
        if ("CLOSED".equals(target)) {
            audits.save(ClosingAuditLog.create(calendar, ActionType.CALENDAR_CLOSED,
                    previous, target, originalActor, evidence));
        }
        if (recoveryActor != null) {
            audits.save(ClosingAuditLog.create(calendar, ActionType.PERIOD_TRANSITION_RECOVERED,
                    previous, target, recoveryActor, evidence + "; provider receipt reconciled"));
        }
        return calendars.save(calendar);
    }

    private ReopenApproval requireApprovedDecision(ClosingCalendar calendar) {
        ReopenApproval approval = aggregates.refreshApproval(calendar.getTransitionApprovalId())
                .orElseThrow(() -> new EntityNotFoundException("ReopenApproval not found"));
        if (approval.getStatus() != ReopenApprovalStatus.APPROVED
                || !Objects.equals(calendar.getTransitionActor(), approval.getApprovedBy())
                || !Objects.equals(calendar.getTransitionFiscalPeriodId(), approval.getFiscalPeriodId())) {
            throw new IllegalStateException("Durable reopen decision no longer matches its transition.");
        }
        return approval;
    }

    private FiscalPeriodTransitionCommand command(ClosingCalendar calendar) {
        String target = calendar.getTransitionTarget();
        return new FiscalPeriodTransitionCommand(calendar.getTransitionId(),
                calendar.getTransitionFiscalPeriodId(),
                "OPEN".equals(target) ? FiscalPeriodClosingStatus.CLOSED : FiscalPeriodClosingStatus.OPEN,
                FiscalPeriodClosingStatus.valueOf(target), calendar.getTransitionActor());
    }

    private void requireReceipt(FiscalPeriodTransitionCommand command, FiscalPeriodTransitionReceipt receipt) {
        if (receipt == null || !receipt.matches(command)) {
            throw new IllegalStateException("Master receipt does not match the durable transition.");
        }
    }

    private void savePrepared(ClosingCalendar calendar) {
        calendars.save(calendar);
        audits.save(ClosingAuditLog.create(calendar, ActionType.PERIOD_TRANSITION_PREPARED,
                calendar.getStatus().name(), "PENDING_" + calendar.getTransitionTarget(),
                calendar.getTransitionActor(), transitionEvidence(calendar)));
    }

    private String transitionEvidence(ClosingCalendar calendar) {
        return "operationId=" + calendar.getTransitionId()
                + "; cycleNumber=" + calendar.getCycleNumber()
                + "; fiscalPeriodId=" + calendar.getTransitionFiscalPeriodId()
                + "; approvalId=" + calendar.getTransitionApprovalId()
                + "; evidenceSetId=" + calendar.getTransitionEvidenceSetId();
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

    private void requireServiceTransitionActor(String actor) {
        if (actor == null || !actor.matches("[A-Za-z0-9._@-]{1,42}")) {
            throw new IllegalArgumentException("transition actor must be a safe identifier of at most 42 characters");
        }
    }

    private String requireBoundedText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
        String normalized = value.trim();
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(field + " must not exceed " + maxLength + " characters");
        }
        return normalized;
    }

    public record PreparedDecision(ReopenApproval approval, ClosingCalendar calendar) { }
}
