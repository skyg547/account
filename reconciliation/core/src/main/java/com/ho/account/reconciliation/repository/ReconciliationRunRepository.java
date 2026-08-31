package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.ReconciliationRun;
import com.ho.account.reconciliation.domain.ReconciliationUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * ReconciliationRun 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, Long> {
    List<ReconciliationRun> findByReconciliationUnitAndReconciliationDate(ReconciliationUnit reconciliationUnit, LocalDate reconciliationDate);

    Optional<ReconciliationRun> findTopByReconciliationUnitAndReconciliationDateAndStatusOrderByRunStartTimeDesc(
            ReconciliationUnit reconciliationUnit, LocalDate reconciliationDate, ReconciliationRun.ReconciliationRunStatus status);

    Optional<ReconciliationRun> findTopByReconciliationUnitAndReconciliationDateOrderByRunStartTimeDesc(
            ReconciliationUnit reconciliationUnit, LocalDate reconciliationDate);
}
