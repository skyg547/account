package com.risk.credit.core.application.port.out;

import com.risk.common.entity.IntegratedRiskPosition;
import java.time.LocalDate;
import java.util.List;

/**
 * [Port] 통합 리스크 포지션 데이터소스 인터페이스.
 */
public interface IntegratedRiskPositionRepository {
    List<IntegratedRiskPosition> findByBaseDt(LocalDate baseDt);
}
