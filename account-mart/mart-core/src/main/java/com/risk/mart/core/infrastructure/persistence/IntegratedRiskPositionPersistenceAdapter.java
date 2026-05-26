package com.risk.mart.core.infrastructure.persistence;

import com.risk.common.entity.IntegratedRiskPosition;
import com.risk.mart.core.application.port.out.IntegratedRiskPositionRepository;
import com.risk.mart.core.infrastructure.persistence.jpa.JpaIntegratedRiskPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * [Adapter] IntegratedRiskPositionRepository 포트의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class IntegratedRiskPositionPersistenceAdapter implements IntegratedRiskPositionRepository {

    private final JpaIntegratedRiskPositionRepository jpaRepository;

    @Override
    public void deleteByBaseDt(LocalDate baseDt) {
        jpaRepository.deleteByBaseDt(baseDt);
    }

    @Override
    public Page<IntegratedRiskPosition> findByBaseDt(LocalDate baseDt, Pageable pageable) {
        return jpaRepository.findByBaseDt(baseDt, pageable);
    }

    @Override
    public List<IntegratedRiskPosition> findByBaseDt(LocalDate baseDt) {
        return jpaRepository.findByBaseDt(baseDt);
    }

    @Override
    public List<ProductCurrencyBalanceSummary> getBalanceSummaryByBaseDate(LocalDate baseDt) {
        return jpaRepository.getBalanceSummaryByBaseDate(baseDt);
    }

    @Override
    public List<StagingDistribution> getStagingDistribution(LocalDate baseDt) {
        return jpaRepository.getStagingDistribution(baseDt);
    }

    @Override
    public List<SectorDistribution> getSectorDistribution(LocalDate baseDt) {
        return jpaRepository.getSectorDistribution(baseDt);
    }

    @Override
    public Map<String, Object> getSummaryMetrics(LocalDate baseDt) {
        return jpaRepository.getSummaryMetrics(baseDt);
    }

    @Override
    public List<RatingDistribution> getRatingDistribution(LocalDate baseDt) {
        return jpaRepository.getRatingDistribution(baseDt);
    }

    @Override
    public IntegratedRiskPosition save(IntegratedRiskPosition position) {
        return jpaRepository.save(position);
    }

    @Override
    public void saveAll(Iterable<IntegratedRiskPosition> positions) {
        jpaRepository.saveAll(positions);
    }
}
