package com.risk.credit.core.infrastructure.adapter.persistence.jpa;

import com.risk.common.entity.IntegratedRiskPosition;
import com.risk.common.entity.IntegratedRiskPositionId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

/**
 * [Infrastructure] JPA를 사용한 통합 리스크 포지션 데이터 접근 인터페이스.
 */
public interface JpaIntegratedRiskPositionRepository
        extends JpaRepository<IntegratedRiskPosition, IntegratedRiskPositionId> {
    List<IntegratedRiskPosition> findByBaseDt(LocalDate baseDt);
}
