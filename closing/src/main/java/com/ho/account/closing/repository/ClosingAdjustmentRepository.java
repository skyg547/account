package com.ho.account.closing.repository;

import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.closing.domain.ClosingAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ClosingAdjustment ?뷀떚?곕? ?꾪븳 Spring Data JPA Repository
 */
@Repository
public interface ClosingAdjustmentRepository extends JpaRepository<ClosingAdjustment, Long> {
    List<ClosingAdjustment> findByFiscalPeriod(FiscalPeriod fiscalPeriod);
}
