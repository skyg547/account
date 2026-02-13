package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.ReconciliationRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * ReconciliationRule 엔티티를 위한 Spring Data JPA Repository
 */
@Repository
public interface ReconciliationRuleRepository extends JpaRepository<ReconciliationRule, Long> {
    List<ReconciliationRule> findByReconciliationUnitOrderByPriorityAsc(ReconciliationUnit reconciliationUnit);
}
