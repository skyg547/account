package com.ho.account.closing.repository;

import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ClosingTask ?뷀떚?곕? ?꾪븳 Spring Data JPA Repository
 */
@Repository
public interface ClosingTaskRepository extends JpaRepository<ClosingTask, Long> {
    List<ClosingTask> findByClosingCalendarOrderByTaskOrderAsc(ClosingCalendar closingCalendar);
}
