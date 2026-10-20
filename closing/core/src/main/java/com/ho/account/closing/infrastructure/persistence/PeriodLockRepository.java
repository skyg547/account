package com.ho.account.closing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface PeriodLockRepository extends JpaRepository<PeriodLockEntity, Long> {
    @Query("select periodLock from PeriodLock periodLock "
            + "where periodLock.fiscalPeriodId = :fiscalPeriodId and periodLock.active = true")
    Optional<PeriodLockEntity> findByFiscalPeriodId(@Param("fiscalPeriodId") Long fiscalPeriodId);
}
