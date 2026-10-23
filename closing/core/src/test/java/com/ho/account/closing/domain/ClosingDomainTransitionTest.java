package com.ho.account.closing.domain;

import org.junit.jupiter.api.Test;

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

    private ClosingTask mandatoryTask() {
        ClosingTask task = new ClosingTask();
        task.setMandatory(true);
        return task;
    }
}
