package com.ho.account.risk.application.port.out;

import com.ho.account.risk.domain.CreditRiskExposure;
import com.ho.account.risk.domain.RiskWeightedAsset;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [RiskPersistencePort]
 * 리스크 도메인의 영속성을 위한 아웃바운드 포트.
 */
public interface RiskPersistencePort {

    void saveExposure(CreditRiskExposure exposure);

    void saveAllExposures(List<CreditRiskExposure> exposures);

    void saveRwa(RiskWeightedAsset rwa);

    List<CreditRiskExposure> findExposuresByBaseDate(LocalDate baseDate);

    List<RiskWeightedAsset> findRwaResultsBetween(LocalDate startDate, LocalDate endDate);

    Optional<CreditRiskExposure> findExposureBySource(String sourceSystemId, String sourceReferenceId, LocalDate baseDate);

    Optional<BigDecimal> findParameterValue(String category, String assetClass, String approach, LocalDate baseDate);
}
