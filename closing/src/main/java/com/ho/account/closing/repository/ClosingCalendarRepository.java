package com.ho.account.closing.repository;

import com.ho.account.closing.domain.ClosingCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * ClosingCalendar ?뷀떚?곕? ?꾪븳 Spring Data JPA Repository
 */
@Repository
public interface ClosingCalendarRepository extends JpaRepository<ClosingCalendar, Long> {
    Optional<ClosingCalendar> findByFiscalYearAndFiscalPeriod(String fiscalYear, String fiscalPeriod);
}
