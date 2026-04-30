package com.ho.account.risk.infrastructure.persistence;

import com.ho.account.risk.application.port.out.RiskPersistencePort;
import com.ho.account.risk.domain.CreditRiskExposure;
import com.ho.account.risk.domain.RiskWeightedAsset;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class RiskPersistenceAdapter implements RiskPersistencePort {

    private final CreditRiskExposureRepository exposureRepository;
    private final RiskWeightedAssetRepository rwaRepository;
    private final RiskParameterRepository riskParameterRepository;

    public RiskPersistenceAdapter(CreditRiskExposureRepository exposureRepository, 
                                 RiskWeightedAssetRepository rwaRepository,
                                 RiskParameterRepository riskParameterRepository) {
        this.exposureRepository = exposureRepository;
        this.rwaRepository = rwaRepository;
        this.riskParameterRepository = riskParameterRepository;
    }

    @Override
    public void saveExposure(CreditRiskExposure exposure) {
        exposureRepository.save(exposure);
    }

    @Override
    public void saveRwa(RiskWeightedAsset rwa) {
        rwaRepository.save(rwa);
    }

    @Override
    public List<CreditRiskExposure> findExposuresByBaseDate(LocalDate baseDate) {
        return exposureRepository.findByBaseDate(baseDate);
    }

    @Override
    public List<RiskWeightedAsset> findRwaResultsBetween(LocalDate startDate, LocalDate endDate) {
        return rwaRepository.findByCalculationDateBetween(startDate, endDate);
    }

    @Override
    public Optional<CreditRiskExposure> findExposureBySource(String sourceSystemId, String sourceReferenceId, LocalDate baseDate) {
        return exposureRepository.findBySourceSystemIdAndSourceReferenceIdAndBaseDate(sourceSystemId, sourceReferenceId, baseDate);
    }

    @Override
    public Optional<BigDecimal> findParameterValue(String category, String assetClass, String approach, LocalDate baseDate) {
        return riskParameterRepository.findActiveParameter(category, assetClass, approach, baseDate)
                .map(com.ho.account.risk.domain.RiskParameter::getParameterValue);
    }
}
