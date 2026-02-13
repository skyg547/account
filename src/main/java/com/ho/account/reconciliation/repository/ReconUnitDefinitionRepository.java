package com.ho.account.reconciliation.repository;

import com.ho.account.reconciliation.domain.ReconUnitDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReconUnitDefinitionRepository extends JpaRepository<ReconUnitDefinition, String> {
    List<ReconUnitDefinition> findByIsActive(String isActive);
}
