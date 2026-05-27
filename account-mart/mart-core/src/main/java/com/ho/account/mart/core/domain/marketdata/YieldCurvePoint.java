package com.ho.account.mart.core.domain.marketdata;

import lombok.*;
import java.math.BigDecimal;

/**
 * [순수 도메인 모델] 수익률 곡선 상세 점 (Yield Curve Point)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class YieldCurvePoint {
    private String curveName;
    private java.time.LocalDate baseDate;
    private String tenorCode;
    private Double tenorYear;
    private BigDecimal rateValue;
}
