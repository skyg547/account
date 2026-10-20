package com.ho.account.closing.application.port.out;

import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingTask;
import java.util.List;
import java.util.Optional;

public interface ClosingTaskPersistencePort {
    ClosingTask save(ClosingTask closingTask);
    Optional<ClosingTask> findById(Long id);
    List<ClosingTask> findByClosingCalendar(ClosingCalendar closingCalendar);
    List<ClosingTask> findByClosingCalendarId(Long calendarId);
    List<ClosingTask> findActiveByClosingCalendarId(Long calendarId);
}
