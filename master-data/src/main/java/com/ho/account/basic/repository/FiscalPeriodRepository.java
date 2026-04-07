package com.ho.account.basic.repository;

import com.ho.account.basic.domain.FiscalPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, Long> {
    Optional<FiscalPeriod> findByFiscalYearAndFiscalPeriod(String fiscalYear, String fiscalPeriod);

    Optional<FiscalPeriod> findFirstByStartDateBeforeAndEndDateAfter(LocalDate date, LocalDate date2);

    default Optional<FiscalPeriod> findByDate(LocalDate date) {
        return findFirstByStartDateBeforeAndEndDateAfter(date, date);
    }
}
