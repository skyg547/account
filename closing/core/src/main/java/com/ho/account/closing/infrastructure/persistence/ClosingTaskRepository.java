package com.ho.account.closing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ClosingTaskRepository extends JpaRepository<ClosingTaskEntity, Long> {
    List<ClosingTaskEntity> findByClosingCalendarOrderByTaskOrderAsc(ClosingCalendarEntity closingCalendar);

    List<ClosingTaskEntity> findByClosingCalendarId(Long calendarId);
}
