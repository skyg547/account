package com.ho.account.ecl.core.infrastructure.adapter.persistence;

import com.ho.account.shared.finance.entity.IntegratedRiskPosition;
import com.ho.account.ecl.core.application.port.out.IntegratedRiskPositionRepository;
import com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa.JpaIntegratedRiskPositionRepository;
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
