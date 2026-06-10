package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.ReconciliationStageResult;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReconciliationStageResultRepository extends JpaRepository<ReconciliationStageResult, Long> {
    List<ReconciliationStageResult> findByReconciliationRun_Id(Long runId);
}
