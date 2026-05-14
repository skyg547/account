package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.PeriodLockPersistencePort;
import com.ho.account.closing.domain.PeriodLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
@Repository
public interface PeriodLockRepository extends JpaRepository<PeriodLock, Long>, PeriodLockPersistencePort {
    Optional<PeriodLock> findByFiscalPeriodId(Long fiscalPeriodId);
}
