package com.ho.account.closing.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface ReopenApprovalRepository extends JpaRepository<ReopenApprovalEntity, Long> {
    List<ReopenApprovalEntity> findByFiscalPeriodIdOrderByRequestedAtDesc(Long fiscalPeriodId);
    boolean existsByFiscalPeriodIdAndStatus(Long fiscalPeriodId, com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus status);
}
