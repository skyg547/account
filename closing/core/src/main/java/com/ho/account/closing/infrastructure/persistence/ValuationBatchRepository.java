package com.ho.account.closing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ValuationBatchRepository extends JpaRepository<ValuationBatchEntity, Long> {
    List<ValuationBatchEntity> findByFiscalPeriodId(Long fiscalPeriodId);
    Optional<ValuationBatchEntity> findByExecutionKey(String executionKey);
}
