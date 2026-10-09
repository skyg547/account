package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingGate;
import com.ho.account.closing.domain.ClosingTask;
import com.ho.account.closing.domain.ReopenApproval;
import java.util.Optional;
import java.util.List;

/** Monthly mutations lock the calendar before reading mutable child state. */
public interface ClosingAggregatePersistencePort {
    Optional<ClosingCalendar> lockCalendar(Long calendarId);
    Optional<ClosingCalendar> lockCalendar(String fiscalYear, String fiscalPeriod);
    Optional<ClosingCalendar> lockCalendarForTask(Long taskId);
    Optional<ClosingCalendar> lockCalendarForGate(Long gateId);
    Optional<Long> findApprovalFiscalPeriodId(Long approvalId);
    Optional<ClosingTask> refreshTask(Long taskId);
    Optional<ClosingGate> refreshGate(Long gateId);
    Optional<ReopenApproval> refreshApproval(Long approvalId);
    List<ClosingTask> refreshTasks(ClosingCalendar calendar);
    List<ClosingGate> refreshGates(ClosingCalendar calendar);
    List<ClosingTask> refreshActiveTasks(ClosingCalendar calendar);
    List<ClosingGate> refreshActiveGates(ClosingCalendar calendar);
}
