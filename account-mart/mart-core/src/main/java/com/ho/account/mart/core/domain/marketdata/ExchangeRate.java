package com.ho.account.mart.core.domain.marketdata;

import com.ho.account.shared.finance.enums.CurrencyCode;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [순수 도메인 모델] 환율 (Exchange Rate)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class ExchangeRate {
    private Long id;
    private LocalDate baseDate;
    private CurrencyCode baseCurrency;
    private CurrencyCode quoteCurrency;
    private BigDecimal baseRate;
}
