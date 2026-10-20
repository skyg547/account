package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ValuationBatchPersistencePort;
import com.ho.account.closing.domain.ValuationBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ValuationBatchRepository extends JpaRepository<ValuationBatch, Long>, ValuationBatchPersistencePort {
    List<ValuationBatch> findByFiscalPeriodId(Long fiscalPeriodId);
    @Override
    Optional<ValuationBatch> findByExecutionKey(String executionKey);
}
