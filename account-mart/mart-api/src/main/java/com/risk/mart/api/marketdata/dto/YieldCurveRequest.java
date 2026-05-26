package com.risk.mart.api.marketdata.dto;

import com.risk.common.enums.CurrencyCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class YieldCurveRequest {

    @NotBlank(message = "Curve name is required")
    private String curveName;

    @NotNull(message = "Base date is required")
    private LocalDate baseDate;

    @NotNull(message = "Currency is required")
    private CurrencyCode currency;

    private String curveType;
    private String description;

    private List<YieldCurvePointRequest> points;
}

