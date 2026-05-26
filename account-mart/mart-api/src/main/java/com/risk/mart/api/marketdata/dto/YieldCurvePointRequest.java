package com.risk.mart.api.marketdata.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YieldCurvePointRequest {

    @NotNull(message = "Tenor is required")
    private Integer tenor;

    private String tenorLabel;

    @NotNull(message = "Rate is required")
    private BigDecimal rate;

    private BigDecimal discountFactor;
}

