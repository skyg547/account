package com.risk.credit.core.infrastructure.adapter.persistence;

import com.risk.common.entity.IntegratedRiskPosition;
import com.risk.credit.core.application.port.out.IntegratedRiskPositionRepository;
import com.risk.credit.core.infrastructure.adapter.persistence.jpa.JpaIntegratedRiskPositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

/**
 * [Adapter] IntegratedRiskPositionRepository 포트의 JPA 구현체.
 */
@Repository
@RequiredArgsConstructor
public class IntegratedRiskPositionPersistenceAdapter implements IntegratedRiskPositionRepository {

    private final JpaIntegratedRiskPositionRepository jpaRepository;

    @Override
    public List<IntegratedRiskPosition> findByBaseDt(LocalDate baseDt) {
        return jpaRepository.findByBaseDt(baseDt);
    }
}
