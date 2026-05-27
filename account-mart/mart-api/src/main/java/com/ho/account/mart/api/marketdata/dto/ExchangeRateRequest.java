package com.ho.account.mart.api.marketdata.dto;

import com.ho.account.shared.finance.enums.CurrencyCode;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRateRequest {

    @NotNull(message = "Base date is required")
    private LocalDate baseDate;

    @NotNull(message = "Base currency is required")
    private CurrencyCode baseCurrency;

    @NotNull(message = "Quote currency is required")
    private CurrencyCode quoteCurrency;

    @NotNull(message = "Rate is required")
    private BigDecimal rate;

    private BigDecimal bidRate;
    private BigDecimal askRate;
    private BigDecimal changeAmount;
    private BigDecimal changePercent;
    private String source;
}

