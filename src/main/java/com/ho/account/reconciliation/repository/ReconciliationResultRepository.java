package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.ReconciliationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconciliationResultRepository extends JpaRepository<ReconciliationResult, Long> {
}
