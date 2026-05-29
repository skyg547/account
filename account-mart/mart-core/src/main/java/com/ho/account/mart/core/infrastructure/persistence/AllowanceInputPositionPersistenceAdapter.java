package com.ho.account.mart.core.infrastructure.persistence;

import com.ho.account.shared.finance.entity.AllowanceInputPosition;
import com.ho.account.mart.core.application.port.out.AllowanceInputPositionRepository;
import com.ho.account.mart.core.infrastructure.persistence.jpa.JpaAllowanceInputPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * [Adapter] AllowanceInputPositionRepository 포트의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class AllowanceInputPositionPersistenceAdapter implements AllowanceInputPositionRepository {

    private final JpaAllowanceInputPositionRepository jpaRepository;

    @Override
    public void deleteByBaseDt(LocalDate baseDt) {
        jpaRepository.deleteByBaseDt(baseDt);
    }

    @Override
    public Page<AllowanceInputPosition> findByBaseDt(LocalDate baseDt, Pageable pageable) {
        return jpaRepository.findByBaseDt(baseDt, pageable);
    }

    @Override
    public List<AllowanceInputPosition> findByBaseDt(LocalDate baseDt) {
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
    public AllowanceInputPosition save(AllowanceInputPosition position) {
        return jpaRepository.save(position);
    }

    @Override
    public void saveAll(Iterable<AllowanceInputPosition> positions) {
        jpaRepository.saveAll(positions);
    }
}

