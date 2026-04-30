package com.ho.account.risk.infrastructure.persistence;

import com.ho.account.risk.domain.RiskWeightedAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RiskWeightedAssetRepository extends JpaRepository<RiskWeightedAsset, Long> {
    List<RiskWeightedAsset> findByCalculationDateBetween(LocalDate startDate, LocalDate endDate);
}
