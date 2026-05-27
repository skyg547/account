package com.ho.account.mart.api.marketdata.dto;

import com.ho.account.shared.finance.enums.CurrencyCode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YieldCurveResponse {

    private Long id;
    private String curveName;
    private LocalDate baseDate;
    private CurrencyCode currency;
    private String curveType;
    private String description;
    private Boolean isActive;
    private List<PointResponse> points;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PointResponse {
        private Long id;
        private Integer tenor;
        private String tenorLabel;
        private BigDecimal rate;
        private BigDecimal discountFactor;
    }
}

