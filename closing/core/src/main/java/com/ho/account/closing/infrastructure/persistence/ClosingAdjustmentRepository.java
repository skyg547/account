package com.ho.account.closing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ClosingAdjustmentRepository extends JpaRepository<ClosingAdjustmentEntity, Long> {
    List<ClosingAdjustmentEntity> findByFiscalPeriodId(Long fiscalPeriodId);
}
