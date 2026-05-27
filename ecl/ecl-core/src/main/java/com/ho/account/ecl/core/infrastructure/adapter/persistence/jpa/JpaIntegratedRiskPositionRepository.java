package com.ho.account.ecl.core.infrastructure.adapter.persistence.jpa;

import com.ho.account.shared.finance.entity.IntegratedRiskPosition;
import com.ho.account.shared.finance.entity.IntegratedRiskPositionId;
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
