package com.ho.account.closing.application.port.in;

import com.ho.account.closing.domain.*;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingGate.ClosingGateStatus;
import com.ho.account.closing.domain.ClosingTask.ClosingTaskStatus;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;

import java.time.LocalDate;

/**
 * 결산 (Closing) 관련 유스케이스 인터페이스.
 */
public interface ClosingUseCase {
    boolean isClosed(LocalDate date);
    ClosingCalendar createClosingCalendar(ClosingCalendar closingCalendar);
    ClosingCalendar findClosingCalendarById(Long id);
    ClosingCalendar findClosingCalendarByFiscalPeriod(String fiscalYear, String fiscalPeriod);
    ClosingCalendar updateClosingCalendarStatus(Long id, ClosingCalendarStatus newStatus, String user);
    ClosingTask createClosingTask(ClosingTask closingTask);
    ClosingTask updateClosingTaskStatus(Long taskId, ClosingTaskStatus newStatus, String user);
    ClosingGate createClosingGate(ClosingGate closingGate);
    ClosingGate checkAndPassClosingGate(Long gateId, String user);
    PeriodLock lockPeriod(Long fiscalPeriodId, PeriodLock.PeriodLockType lockType, String user, String reason);
    void unlockPeriod(Long fiscalPeriodId, String user);
    ReopenApproval requestPeriodReopen(Long fiscalPeriodId, String requestedBy, String reason);
    ReopenApproval updateReopenApprovalStatus(Long approvalId, ReopenApprovalStatus newStatus, String approvedBy);
    ValuationBatch runValuationBatch(Long fiscalPeriodId, ValuationBatch.ValuationType valuationType, String runBy);
    ProvisionBatch runProvisionBatch(Long fiscalPeriodId, ProvisionBatch.ProvisionType provisionType, String runBy);
    ClosingAdjustment createClosingAdjustment(Long fiscalPeriodId, Long journalEntryId, ClosingAdjustment.AdjustmentType adjustmentType, String description, String approvedBy);
    ClosingCalendar determineClosingStatus(Long calendarId, String user);
}
