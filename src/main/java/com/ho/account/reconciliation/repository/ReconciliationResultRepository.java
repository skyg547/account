package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.ReconciliationResult;
import com.ho.account.reconciliation.domain.ReconciliationStatus;
import com.ho.account.reconciliation.domain.ReconciliationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, Long> {

    List<ReconciliationResult> findByReconciliationType(ReconciliationType type);

    List<ReconciliationResult> findByReconciliationDate(LocalDate date);

    List<ReconciliationResult> findByReconciliationDateAndReconciliationType(LocalDate date, ReconciliationType type);

    List<ReconciliationResult> findByStatus(ReconciliationStatus status);

    List<ReconciliationResult> findByReconciliationDateBetween(LocalDate startDate, LocalDate endDate);
}
