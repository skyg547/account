package com.ho.account.masterdata.core.infrastructure.persistence.repository;

import com.ho.account.masterdata.core.infrastructure.persistence.entity.FiscalPeriodEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FiscalPeriodRepository extends JpaRepository<FiscalPeriodEntity, Long> {
    Optional<FiscalPeriodEntity> findByFiscalYearAndFiscalPeriod(String fiscalYear, String fiscalPeriod);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT fp FROM FiscalPeriodEntity fp WHERE fp.id = :id")
    Optional<FiscalPeriodEntity> findByIdForUpdate(Long id);
}

