package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.ReconciliationRun;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ReconciliationRun 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface ReconciliationRunRepository extends JpaRepository<ReconciliationRun, Long> {
}
