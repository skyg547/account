package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.ReconStageResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconStageResultRepository extends JpaRepository<ReconStageResult, Long> {
    List<ReconStageResult> findByReconciliationResult_Id(Long reconciliationResultId);
}
