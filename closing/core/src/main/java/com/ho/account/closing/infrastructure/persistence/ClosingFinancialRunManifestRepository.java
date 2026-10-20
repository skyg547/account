package com.ho.account.closing.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClosingFinancialRunManifestRepository extends JpaRepository<ClosingFinancialRunManifest, Long> {
    boolean existsByScopeAndBatchId(String scope, Long batchId);
    Optional<ClosingFinancialRunManifest> findByScopeAndBatchId(String scope, Long batchId);
}
