package com.ho.account.closing.repository;

import com.ho.account.basic.domain.FiscalPeriod;
import com.ho.account.closing.domain.ReopenApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * ReopenApproval 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface ReopenApprovalRepository extends JpaRepository<ReopenApproval, Long> {
    List<ReopenApproval> findByFiscalPeriodOrderByRequestedAtDesc(FiscalPeriod fiscalPeriod);
}
