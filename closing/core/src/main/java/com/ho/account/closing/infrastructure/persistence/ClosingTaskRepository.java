package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ClosingTaskPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ClosingTaskRepository extends JpaRepository<ClosingTask, Long>, ClosingTaskPersistencePort {
    List<ClosingTask> findByClosingCalendarOrderByTaskOrderAsc(ClosingCalendar closingCalendar);

    @Override
    List<ClosingTask> findByClosingCalendarId(Long calendarId);

    @Override
    @Query("select t from ClosingTask t where t.closingCalendar.id = :calendarId "
            + "and t.cycleNumber = t.closingCalendar.cycleNumber order by t.taskOrder asc")
    List<ClosingTask> findActiveByClosingCalendarId(@Param("calendarId") Long calendarId);
}
