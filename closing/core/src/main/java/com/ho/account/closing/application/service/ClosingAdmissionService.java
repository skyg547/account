package com.ho.account.closing.application.service;

import com.ho.account.closing.application.port.in.ClosingAdmissionQuery;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.PeriodLockPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.PeriodLock;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Combines the master period, Closing calendar and active lock into one admission decision.
 * This query has no Journal ports: Journal validation must not depend on the Closing command
 * service that itself creates journals. The read is a point-in-time guard, not a write fence
 * against a concurrent lock or closing transition.
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ClosingAdmissionService implements ClosingAdmissionQuery {

    private final FiscalPeriodControlPort fiscalPeriodControlPort;
    private final ClosingCalendarPersistencePort closingCalendarPersistencePort;
    private final PeriodLockPersistencePort periodLockPersistencePort;

    @Override
    public boolean isClosed(LocalDate accountingDate) {
        if (accountingDate == null) {
            throw new IllegalArgumentException("accountingDate must not be null");
        }
        String fiscalYear = String.valueOf(accountingDate.getYear());
        String fiscalPeriod = String.format(Locale.ROOT, "%02d", accountingDate.getMonthValue());
        Optional<FiscalPeriodRef> result = fiscalPeriodControlPort.findFiscalPeriod(fiscalYear, fiscalPeriod);
        if (result == null || result.isEmpty()) {
            throw new IllegalStateException("Fiscal period is missing for accounting date " + accountingDate);
        }
        FiscalPeriodRef period = result.get();
        validatePeriod(period, fiscalYear, fiscalPeriod, accountingDate);
        if (!"OPEN".equals(period.closingStatus())) {
            return true;
        }

        Optional<ClosingCalendar> calendarResult =
                closingCalendarPersistencePort.findByFiscalYearAndFiscalPeriod(fiscalYear, fiscalPeriod);
        if (calendarResult == null || calendarResult.isEmpty()) {
            return true;
        }
        ClosingCalendar calendar = calendarResult.get();
        if (!fiscalYear.equals(calendar.getFiscalYear()) || !fiscalPeriod.equals(calendar.getFiscalPeriod())
                || calendar.getStatus() != ClosingCalendarStatus.OPEN || calendar.getTransitionId() != null) {
            return true;
        }

        // Unlock removes the active row. Every remaining row blocks this date-only contract:
        // neither a partial lock nor NON_ADJUSTMENT_ENTRIES proves this caller is an authorized adjustment.
        Optional<PeriodLock> lock = periodLockPersistencePort.findByFiscalPeriodId(period.id());
        if (lock == null) {
            throw new IllegalStateException("Period lock lookup returned no result");
        }
        return lock.isPresent();
    }

    private void validatePeriod(
            FiscalPeriodRef period, String fiscalYear, String fiscalPeriod, LocalDate accountingDate) {
        // An OPEN response for another period or an invalid ID must never redirect the lock lookup.
        if (period.id() == null || period.id() <= 0
                || !fiscalYear.equals(period.fiscalYear()) || !fiscalPeriod.equals(period.fiscalPeriod())
                || period.startDate() == null || period.endDate() == null
                || period.startDate().isAfter(period.endDate())
                || accountingDate.isBefore(period.startDate()) || accountingDate.isAfter(period.endDate())) {
            throw new IllegalStateException("Invalid fiscal period for accounting date " + accountingDate);
        }
    }
}
