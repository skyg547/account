package com.ho.account.closing.repository;

import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.closing.domain.ValuationBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ValuationBatch ?뷀떚?곕? ?꾪븳 Spring Data JPA Repository
 */
@Repository
public interface ValuationBatchRepository extends JpaRepository<ValuationBatch, Long> {
    List<ValuationBatch> findByFiscalPeriod(FiscalPeriod fiscalPeriod);
}
