package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.ReconciliationDifference;
import com.ho.account.reconciliation.domain.ReconciliationRun; // Added import
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Added import

/**
 * ReconciliationDifference 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface ReconciliationDifferenceRepository extends JpaRepository<ReconciliationDifference, Long> {
    List<ReconciliationDifference> findByReconciliationRun(ReconciliationRun reconciliationRun);
}
