package com.ho.account.closing.repository;

import com.ho.account.basic.domain.FiscalPeriod;
import com.ho.account.closing.domain.ClosingAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ClosingAdjustment 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface ClosingAdjustmentRepository extends JpaRepository<ClosingAdjustment, Long> {
    List<ClosingAdjustment> findByFiscalPeriod(FiscalPeriod fiscalPeriod);
}
