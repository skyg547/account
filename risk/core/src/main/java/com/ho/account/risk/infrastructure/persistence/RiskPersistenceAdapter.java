package com.ho.account.risk.infrastructure.persistence;

import com.ho.account.risk.application.port.out.RiskPersistencePort;
import com.ho.account.risk.domain.CreditRiskExposure;
import com.ho.account.risk.domain.RiskWeightedAsset;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class RiskPersistenceAdapter implements RiskPersistencePort {

    private final CreditRiskExposureRepository exposureRepository;
    private final RiskWeightedAssetRepository rwaRepository;
    private final RiskParameterRepository riskParameterRepository;
    private final JdbcTemplate jdbcTemplate;

    public RiskPersistenceAdapter(CreditRiskExposureRepository exposureRepository, 
                                 RiskWeightedAssetRepository rwaRepository,
                                 RiskParameterRepository riskParameterRepository,
                                 JdbcTemplate jdbcTemplate) {
        this.exposureRepository = exposureRepository;
        this.rwaRepository = rwaRepository;
        this.riskParameterRepository = riskParameterRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void saveExposure(CreditRiskExposure exposure) {
        exposureRepository.save(exposure);
    }

    @Override
    @Transactional
    public void saveAllExposures(List<CreditRiskExposure> exposures) {
        String sql = "INSERT INTO credit_risk_exposures " +
                     "(base_date, source_system_id, source_reference_id, business_partner_id, " +
                     "ead_amount, pd_rate, lgd_rate, el_amount, asset_class) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, exposures, 1000, (PreparedStatement ps, CreditRiskExposure exposure) -> {
            ps.setDate(1, Date.valueOf(exposure.getBaseDate()));
            ps.setString(2, exposure.getSourceSystemId());
            ps.setString(3, exposure.getSourceReferenceId());
            ps.setLong(4, exposure.getBusinessPartner().getId());
            ps.setBigDecimal(5, exposure.getEadAmount());
            ps.setBigDecimal(6, exposure.getPdRate());
            ps.setBigDecimal(7, exposure.getLgdRate());
            ps.setBigDecimal(8, exposure.getElAmount());
            ps.setString(9, exposure.getAssetClass());
        });
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
