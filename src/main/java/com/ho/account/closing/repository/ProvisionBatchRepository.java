package com.ho.account.closing.repository;

import com.ho.account.basic.domain.FiscalPeriod;
import com.ho.account.closing.domain.ProvisionBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ProvisionBatch 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface ProvisionBatchRepository extends JpaRepository<ProvisionBatch, Long> {
    List<ProvisionBatch> findByFiscalPeriod(FiscalPeriod fiscalPeriod);
}
