package com.ho.account.closing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface PeriodLockRepository extends JpaRepository<PeriodLockEntity, Long> {
    Optional<PeriodLockEntity> findByFiscalPeriodId(Long fiscalPeriodId);
}
