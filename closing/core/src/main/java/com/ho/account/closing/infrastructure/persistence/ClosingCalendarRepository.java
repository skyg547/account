package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface ClosingCalendarRepository extends JpaRepository<ClosingCalendar, Long>, ClosingCalendarPersistencePort {
    Optional<ClosingCalendar> findByFiscalYearAndFiscalPeriod(String fiscalYear, String fiscalPeriod);
}
