package com.risk.mart.api.marketdata.dto;

import com.risk.common.enums.CurrencyCode;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketRateResponse {

    private Long id;
    private LocalDate baseDate;
    private String rateName;
    private String rateType;
    private CurrencyCode currency;
    private Integer tenor;
    private String tenorLabel;
    private BigDecimal rate;
    private BigDecimal changeBp;
    private String source;
    private Boolean isActive;
}

