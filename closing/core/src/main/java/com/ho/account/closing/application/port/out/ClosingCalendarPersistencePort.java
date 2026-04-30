package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ClosingCalendar;
import java.util.Optional;

public interface ClosingCalendarPersistencePort {
    ClosingCalendar save(ClosingCalendar closingCalendar);
    Optional<ClosingCalendar> findById(Long id);
    Optional<ClosingCalendar> findByFiscalYearAndFiscalPeriod(String fiscalYear, String fiscalPeriod);
}
