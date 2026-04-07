package com.ho.account.closing.repository;

import com.ho.account.basic.domain.FiscalPeriod;
import com.ho.account.closing.domain.PeriodLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * PeriodLock 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface PeriodLockRepository extends JpaRepository<PeriodLock, Long> {
    Optional<PeriodLock> findByFiscalPeriod(FiscalPeriod fiscalPeriod);
}
