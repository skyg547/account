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
public class ExchangeRateResponse {

    private Long id;
    private LocalDate baseDate;
    private CurrencyCode baseCurrency;
    private CurrencyCode quoteCurrency;
    private BigDecimal rate;
    private BigDecimal bidRate;
    private BigDecimal askRate;
    private BigDecimal changeAmount;
    private BigDecimal changePercent;
    private String source;
}

