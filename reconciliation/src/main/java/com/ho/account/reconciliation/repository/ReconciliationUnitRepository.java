package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.ReconciliationUnit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ReconciliationUnit 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface ReconciliationUnitRepository extends JpaRepository<ReconciliationUnit, Long> {
}
