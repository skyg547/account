package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ClosingTaskPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ClosingTaskRepository extends JpaRepository<ClosingTask, Long>, ClosingTaskPersistencePort {
    List<ClosingTask> findByClosingCalendarOrderByTaskOrderAsc(ClosingCalendar closingCalendar);

    @Override
    List<ClosingTask> findByClosingCalendarId(Long calendarId);
}
