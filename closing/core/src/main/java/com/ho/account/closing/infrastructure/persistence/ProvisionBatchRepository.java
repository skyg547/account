package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ProvisionBatchPersistencePort;
import com.ho.account.closing.domain.ProvisionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ProvisionBatchRepository extends JpaRepository<ProvisionBatch, Long>, ProvisionBatchPersistencePort {
    List<ProvisionBatch> findByFiscalPeriodId(Long fiscalPeriodId);
}
