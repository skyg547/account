package com.ho.account.closing.infrastructure.persistence;

import com.ho.account.closing.application.port.out.ReopenApprovalPersistencePort;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.closing.domain.ReopenApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ReopenApprovalRepository extends JpaRepository<ReopenApproval, Long>, ReopenApprovalPersistencePort {
    List<ReopenApproval> findByFiscalPeriodOrderByRequestedAtDesc(FiscalPeriod fiscalPeriod);
}
