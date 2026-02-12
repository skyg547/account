package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.ReconciliationVariance;
import com.ho.account.reconciliation.domain.VarianceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconciliationVarianceRepository extends JpaRepository<ReconciliationVariance, Long> {

    List<ReconciliationVariance> findByReconciliationResultId(Long reconciliationResultId);

    List<ReconciliationVariance> findByStatus(VarianceStatus status);

    List<ReconciliationVariance> findByReconciliationResultIdAndStatus(Long reconciliationResultId,
            VarianceStatus status);
}
