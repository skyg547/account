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

    private ClosingTask mandatoryTask() {
        ClosingTask task = new ClosingTask();
        task.setMandatory(true);
        return task;
    }
}
