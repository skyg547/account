package com.ho.account.closing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface ClosingCalendarRepository extends JpaRepository<ClosingCalendarEntity, Long> {
    Optional<ClosingCalendarEntity> findByFiscalYearAndFiscalPeriod(String fiscalYear, String fiscalPeriod);
}
