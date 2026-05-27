package com.ho.account.mart.core.domain.marketdata;

import com.ho.account.shared.finance.enums.CurrencyCode;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [순수 도메인 모델] 시장 금리 (Market Rate)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class MarketRate {
    private String rateCode;
    private LocalDate baseDate;
    private String rateName;
    private String rateType;
    private CurrencyCode currency;
    private Integer tenorMonths;
    private String tenorLabel;
    private BigDecimal rate;
}
