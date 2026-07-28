package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriod, Long> {
    Optional<FiscalPeriod> findByFiscalYearAndFiscalPeriod(String fiscalYear, String fiscalPeriod);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fp FROM FiscalPeriod fp WHERE fp.id = :id")
    Optional<FiscalPeriod> findByIdForUpdate(Long id);
}
