package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ClosingAdjustmentPersistencePort;
import com.ho.account.closing.domain.ClosingAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ClosingAdjustmentRepository extends JpaRepository<ClosingAdjustment, Long>, ClosingAdjustmentPersistencePort {
    List<ClosingAdjustment> findByFiscalPeriodId(Long fiscalPeriodId);
}
