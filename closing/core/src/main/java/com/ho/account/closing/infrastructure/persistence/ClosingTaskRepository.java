package com.ho.account.closing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ClosingTaskRepository extends JpaRepository<ClosingTaskEntity, Long> {
    List<ClosingTaskEntity> findByClosingCalendarOrderByTaskOrderAsc(ClosingCalendarEntity closingCalendar);

    List<ClosingTaskEntity> findByClosingCalendarId(Long calendarId);

    @Query("select t from ClosingTask t where t.closingCalendar.id = :calendarId "
            + "and t.cycleNumber = t.closingCalendar.cycleNumber order by t.taskOrder asc")
    List<ClosingTaskEntity> findActiveByClosingCalendarId(@Param("calendarId") Long calendarId);
}
