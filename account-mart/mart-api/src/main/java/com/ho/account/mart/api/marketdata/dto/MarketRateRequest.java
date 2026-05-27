package com.ho.account.mart.api.marketdata.dto;

import com.ho.account.shared.finance.enums.CurrencyCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketRateRequest {

    @NotNull(message = "Base date is required")
    private LocalDate baseDate;

    @NotBlank(message = "Rate name is required")
    private String rateName;

    @NotBlank(message = "Rate type is required")
    private String rateType;

    @NotNull(message = "Currency is required")
    private CurrencyCode currency;

    @NotNull(message = "Tenor is required")
    private Integer tenor;

    private String tenorLabel;

    @NotNull(message = "Rate is required")
    private BigDecimal rate;

    private BigDecimal changeBp;
    private String source;
}

