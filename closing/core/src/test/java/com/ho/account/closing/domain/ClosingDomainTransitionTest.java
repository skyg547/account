package com.ho.account.closing.domain;

import com.ho.account.contracts.journal.JournalPeriodCheckpoint;
import com.ho.account.contracts.journal.JournalPeriodFenceCommand;
import com.ho.account.contracts.journal.JournalPeriodScope;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ClosingDomainTransitionTest {

    @Test
    void calendarRequiresInProgressAndDefinedControlsBeforeClose() {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.OPEN);

        assertThatThrownBy(() -> calendar.validateReadyToClose(List.of(), List.of()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mandatory closing task");

        calendar.start("MAKER");
        ClosingTask task = mandatoryTask();
        task.setStatus(ClosingTask.ClosingTaskStatus.COMPLETED);
        ClosingGate gate = new ClosingGate();
        gate.setStatus(ClosingGate.ClosingGateStatus.PASSED);
        calendar.validateReadyToClose(List.of(task), List.of(gate));
        calendar.close("CHECKER");

        assertThat(calendar.getStatus()).isEqualTo(ClosingCalendar.ClosingCalendarStatus.CLOSED);
        assertThat(calendar.getClosedBy()).isEqualTo("CHECKER");
    }

    @Test
    void taskCannotSkipMandatoryWorkOrCompleteBeforeStart() {
        ClosingTask task = mandatoryTask();
        task.setStatus(ClosingTask.ClosingTaskStatus.PENDING);

        assertThatThrownBy(() -> task.changeStatus(ClosingTask.ClosingTaskStatus.SKIPPED, "USER"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> task.complete("USER"))
                .isInstanceOf(IllegalStateException.class);

        task.start("USER");
        task.complete("USER");
        assertThat(task.getStatus()).isEqualTo(ClosingTask.ClosingTaskStatus.COMPLETED);
    }

    @Test
    void reopenApprovalEnforcesMakerCheckerAndOneWayDecision() {
        ReopenApproval approval = new ReopenApproval();
        approval.request("MAKER", "late adjustment");

        assertThatThrownBy(() -> approval.approve("MAKER"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own request");

        approval.approve("CHECKER");
        assertThat(approval.getStatus()).isEqualTo(ReopenApproval.ReopenApprovalStatus.APPROVED);
        assertThatThrownBy(() -> approval.reject("OTHER"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void preparedAndDispatchedCloseKeepTheOriginallyBoundEvidenceSet() {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS);
        calendar.prepareTransition("CLOSED", 20L, null, "original-evidence", "closer");
        String operationId = calendar.getTransitionId();

        assertThat(calendar.getTransitionEvidenceSetId()).isEqualTo("original-evidence");
        calendar.markTransitionDispatched(operationId);
        assertThat(calendar.getTransitionEvidenceSetId()).isEqualTo("original-evidence");
        assertThatThrownBy(() -> calendar.prepareTransition(
                "CLOSED", 20L, null, "newer-evidence", "other"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("pending recovery");
        assertThat(calendar.getTransitionEvidenceSetId()).isEqualTo("original-evidence");
    }

    @Test
    void reopenWithoutNewActivityCannotReusePriorCompletedTaskAndPassedGate() {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.OPEN);
        ClosingTask priorTask = mandatoryTask();
        priorTask.setStatus(ClosingTask.ClosingTaskStatus.COMPLETED);
        ClosingGate priorGate = new ClosingGate();
        priorGate.setStatus(ClosingGate.ClosingGateStatus.PASSED);

        calendar.start("CLOSER");
        calendar.validateReadyToClose(List.of(priorTask), List.of(priorGate));
        calendar.close("CHECKER");
        calendar.reopen("REOPEN_APPROVER");
        calendar.start("CLOSER");

        // The old rows stay traceable, but reopening always requires a fresh control run.
        assertThat(calendar.getCycleNumber()).isEqualTo(2);
        assertThat(calendar.getLastSourceChangedAt()).isNotNull();
        assertThat(priorTask.getCycleNumber()).isEqualTo(1);
        assertThat(priorGate.getCycleNumber()).isEqualTo(1);
        assertThat(priorTask.getStatus()).isEqualTo(ClosingTask.ClosingTaskStatus.COMPLETED);
        assertThat(priorGate.getStatus()).isEqualTo(ClosingGate.ClosingGateStatus.PASSED);
        assertThat(calendar.isReadyToClose(List.of(priorTask), List.of(priorGate))).isFalse();
        assertThatThrownBy(() -> calendar.validateReadyToClose(List.of(priorTask), List.of(priorGate)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void changedReopenRequiresBothCurrentCycleControlsToRunAgain() {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.OPEN);
        ClosingTask priorTask = mandatoryTask();
        priorTask.assignCycle(calendar);
        priorTask.setStatus(ClosingTask.ClosingTaskStatus.COMPLETED);
        ClosingGate priorGate = new ClosingGate();
        priorGate.assignCycle(calendar);
        priorGate.setStatus(ClosingGate.ClosingGateStatus.PASSED);
        calendar.start("CLOSER");
        calendar.validateReadyToClose(List.of(priorTask), List.of(priorGate));
        calendar.close("CHECKER");
        calendar.reopen("REOPEN_APPROVER");
        calendar.start("CLOSER");

        ClosingTask renewedTask = mandatoryTask();
        renewedTask.assignCycle(calendar);
        renewedTask.setStatus(ClosingTask.ClosingTaskStatus.PENDING);
        ClosingGate renewedGate = new ClosingGate();
        renewedGate.assignCycle(calendar);
        renewedGate.setStatus(ClosingGate.ClosingGateStatus.PENDING);

        // A post-reopen adjustment cannot borrow either control from the old cycle.
        assertThat(calendar.isReadyToClose(List.of(priorTask, renewedTask), List.of(priorGate, renewedGate)))
                .isFalse();
        renewedTask.start("RECONCILER");
        renewedTask.complete("RECONCILER");
        assertThat(calendar.isReadyToClose(List.of(priorTask, renewedTask), List.of(priorGate, renewedGate)))
                .isFalse();
        renewedGate.pass("APPROVER", List.of(renewedTask));
        assertThat(calendar.isReadyToClose(List.of(priorTask, renewedTask), List.of(priorGate, renewedGate)))
                .isTrue();
        calendar.validateReadyToClose(List.of(priorTask, renewedTask), List.of(priorGate, renewedGate));
        assertThat(priorTask.getStatus()).isEqualTo(ClosingTask.ClosingTaskStatus.COMPLETED);
        assertThat(priorGate.getStatus()).isEqualTo(ClosingGate.ClosingGateStatus.PASSED);
    }

    @Test
    void completedCloseKeepsTheExactFenceForTheFutureReopen() {
        ClosingCalendar calendar = preparedClose();
        String operationId = calendar.getTransitionId();
        JournalPeriodFenceCommand command = journalCommand(operationId);
        calendar.bindJournalFence(command);
        calendar.markTransitionDispatched(operationId);

        calendar.finishTransition(operationId);

        assertThat(calendar.getStatus()).isEqualTo(ClosingCalendar.ClosingCalendarStatus.CLOSED);
        assertThat(calendar.getTransitionId()).isNull();
        assertThat(calendar.getTransitionEvidenceSetId()).isNull();
        assertThat(calendar.getJournalFenceCommand()).isEqualTo(command);
        assertThat(calendar.getClosedBy()).isEqualTo("closer");
    }

    @ParameterizedTest
    @ValueSource(strings = {"operation", "actor", "period"})
    void journalFenceMustMatchThePreparedCloseIdentity(String mismatch) {
        ClosingCalendar calendar = preparedClose();
        String operationId = calendar.getTransitionId();
        JournalPeriodFenceCommand command = new JournalPeriodFenceCommand(
                "operation".equals(mismatch) ? "another-close" : operationId,
                new JournalPeriodCheckpoint(new JournalPeriodScope("period".equals(mismatch) ? 21L : 20L,
                        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)), 0),
                "actor".equals(mismatch) ? "another-closer" : "closer");

        assertThatThrownBy(() -> calendar.bindJournalFence(command)).isInstanceOf(IllegalStateException.class);

        assertThat(calendar.getTransitionId()).isEqualTo(operationId);
        assertThat(calendar.getTransitionStage()).isEqualTo(ClosingCalendar.TransitionStage.PREPARED);
        assertThat(calendar.getTransitionEvidenceSetId()).isEqualTo("original-evidence");
        assertThat(calendar.getJournalFenceCommand()).isNull();
    }

    @Test
    void preparedCloseCannotReplaceItsBoundJournalCheckpoint() {
        ClosingCalendar calendar = preparedClose();
        JournalPeriodFenceCommand original = journalCommand(calendar.getTransitionId());
        calendar.bindJournalFence(original);
        calendar.bindJournalFence(original);
        JournalPeriodFenceCommand newer = new JournalPeriodFenceCommand(original.operationId(),
                new JournalPeriodCheckpoint(original.checkpoint().scope(), 1), original.actor());

        assertThatThrownBy(() -> calendar.bindJournalFence(newer)).isInstanceOf(IllegalStateException.class);

        assertThat(calendar.getJournalFenceCommand()).isEqualTo(original);
        assertThat(calendar.getTransitionStage()).isEqualTo(ClosingCalendar.TransitionStage.PREPARED);
        assertThat(calendar.getTransitionEvidenceSetId()).isEqualTo("original-evidence");
    }

    @Test
    void cancellationCannotEraseEvidenceOrFenceUntilReleaseIsPending() {
        ClosingCalendar calendar = preparedClose();
        String operationId = calendar.getTransitionId();
        JournalPeriodFenceCommand command = journalCommand(operationId);
        calendar.bindJournalFence(command);

        assertThatThrownBy(() -> calendar.cancelPreparedClose(operationId, "operator"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("release must be pending");
        calendar.prepareCancellation(operationId);

        // Keep the original binding while a release acknowledgement can still be lost and retried.
        assertThat(calendar.getTransitionStage()).isEqualTo(ClosingCalendar.TransitionStage.CANCEL_RELEASE_PENDING);
        assertThat(calendar.getTransitionEvidenceSetId()).isEqualTo("original-evidence");
        assertThat(calendar.getJournalFenceCommand()).isEqualTo(command);
        assertThatThrownBy(calendar::requireChecklistMutable).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> calendar.markTransitionDispatched(operationId))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> calendar.cancelPreparedClose("other-operation", "operator"))
                .isInstanceOf(IllegalStateException.class);

        calendar.cancelPreparedClose(operationId, "operator");

        assertThat(calendar.getStatus()).isEqualTo(ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS);
        assertThat(calendar.getTransitionId()).isNull();
        assertThat(calendar.getTransitionEvidenceSetId()).isNull();
        assertThat(calendar.getJournalFenceCommand()).isNull();
        assertThat(calendar.getCycleNumber()).isEqualTo(1);
        calendar.requireChecklistMutable();
    }

    @Test
    void dispatchedCloseCannotBeCancelledOrHaveItsFenceRebound() {
        ClosingCalendar calendar = preparedClose();
        String operationId = calendar.getTransitionId();
        JournalPeriodFenceCommand command = journalCommand(operationId);
        calendar.bindJournalFence(command);
        calendar.markTransitionDispatched(operationId);

        assertThatThrownBy(() -> calendar.prepareCancellation(operationId))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> calendar.bindJournalFence(command)).isInstanceOf(IllegalStateException.class);
        assertThat(calendar.getTransitionStage()).isEqualTo(ClosingCalendar.TransitionStage.DISPATCHED);
        assertThat(calendar.getJournalFenceCommand()).isEqualTo(command);
        assertThat(calendar.getTransitionEvidenceSetId()).isEqualTo("original-evidence");
    }

    @Test
    void reopenedCalendarRemainsFencedUntilItsOriginalCloseFenceIsReleased() {
        ClosingCalendar calendar = preparedClose();
        String closeOperation = calendar.getTransitionId();
        JournalPeriodFenceCommand originalClose = journalCommand(closeOperation);
        calendar.bindJournalFence(originalClose);
        calendar.markTransitionDispatched(closeOperation);
        calendar.finishTransition(closeOperation);
        calendar.prepareTransition("OPEN", 20L, 30L, null, "approver");
        String reopenOperation = calendar.getTransitionId();
        calendar.markTransitionDispatched(reopenOperation);

        calendar.finishTransition(reopenOperation);

        assertThat(calendar.getStatus()).isEqualTo(ClosingCalendar.ClosingCalendarStatus.OPEN);
        assertThat(calendar.getCycleNumber()).isEqualTo(2);
        assertThat(calendar.getTransitionStage()).isEqualTo(ClosingCalendar.TransitionStage.RELEASE_PENDING);
        assertThat(calendar.getJournalFenceCommand()).isEqualTo(originalClose);
        assertThatThrownBy(calendar::requireChecklistMutable).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> calendar.finishTransition(reopenOperation)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> calendar.finishJournalRelease(closeOperation)).isInstanceOf(IllegalStateException.class);
        assertThat(calendar.getCycleNumber()).isEqualTo(2);

        // Reopen has a new operation ID, but releases the durable command from the earlier close.
        calendar.finishJournalRelease(reopenOperation);

        assertThat(calendar.getTransitionId()).isNull();
        assertThat(calendar.getJournalFenceCommand()).isNull();
        assertThat(calendar.getCycleNumber()).isEqualTo(2);
        calendar.requireChecklistMutable();
    }

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "CLOSED"})
    void legacyTransitionWithoutDurableFenceCannotFinish(String target) {
        ClosingCalendar calendar = new ClosingCalendar();
        ClosingCalendar.ClosingCalendarStatus initial = "OPEN".equals(target)
                ? ClosingCalendar.ClosingCalendarStatus.CLOSED : ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS;
        calendar.setStatus(initial);
        calendar.prepareTransition(target, 20L, "OPEN".equals(target) ? 30L : null,
                "CLOSED".equals(target) ? "legacy-evidence" : null, "actor");
        String operationId = calendar.getTransitionId();
        calendar.markTransitionDispatched(operationId);

        assertThatThrownBy(() -> calendar.finishTransition(operationId))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("no durable Journal fence");

        assertThat(calendar.getStatus()).isEqualTo(initial);
        assertThat(calendar.getTransitionStage()).isEqualTo(ClosingCalendar.TransitionStage.DISPATCHED);
        assertThat(calendar.getTransitionId()).isEqualTo(operationId);
        assertThat(calendar.getCycleNumber()).isEqualTo(1);
        assertThat(calendar.getClosedAt()).isNull();
        assertThat(calendar.getReopenedAt()).isNull();
    }

    private ClosingCalendar preparedClose() {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setStatus(ClosingCalendar.ClosingCalendarStatus.IN_PROGRESS);
        calendar.prepareTransition("CLOSED", 20L, null, "original-evidence", "closer");
        return calendar;
    }

    private JournalPeriodFenceCommand journalCommand(String operationId) {
        return new JournalPeriodFenceCommand(operationId, new JournalPeriodCheckpoint(
                new JournalPeriodScope(20L, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31)), 0), "closer");
    }

    private ClosingTask mandatoryTask() {
        ClosingTask task = new ClosingTask();
        task.setMandatory(true);
        return task;
    }
}
