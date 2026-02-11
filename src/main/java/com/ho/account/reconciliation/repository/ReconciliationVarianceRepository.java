package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.ReconciliationVariance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReconciliationVarianceRepository extends JpaRepository<ReconciliationVariance, Long> {
}
