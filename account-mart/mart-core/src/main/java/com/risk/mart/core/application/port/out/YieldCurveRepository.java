package com.risk.mart.core.application.port.out;

import com.risk.mart.core.domain.marketdata.YieldCurve;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * [Outbound Port] 수익률 곡선 데이터 접근 인터페이스
 */
public interface YieldCurveRepository {
    Optional<YieldCurve> findByCurveNameAndBaseDate(String curveName, LocalDate baseDate);
    List<YieldCurve> findByBaseDate(LocalDate baseDate);
    YieldCurve save(YieldCurve yieldCurve);
}
