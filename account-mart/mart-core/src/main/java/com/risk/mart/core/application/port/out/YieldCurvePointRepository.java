package com.risk.mart.core.application.port.out;

import com.risk.mart.core.domain.marketdata.YieldCurvePoint;
import java.util.List;

/**
 * [Outbound Port] 수익률 곡선 상세 점(Point) 데이터 접근 인터페이스
 */
public interface YieldCurvePointRepository {
    List<YieldCurvePoint> findAll();
    YieldCurvePoint save(YieldCurvePoint point);
    void saveAll(List<YieldCurvePoint> points);
}
