package com.ho.account.mart.core.domain.marketdata;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

/**
 * [순수 도메인 모델] 수익률 곡선 (Yield Curve)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class YieldCurve {
    private String curveName;
    private LocalDate baseDate;
    private String currency;
    private String curveDescription;
    private List<YieldCurvePoint> points;
}
