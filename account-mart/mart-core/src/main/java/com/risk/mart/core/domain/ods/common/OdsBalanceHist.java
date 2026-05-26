package com.risk.mart.core.domain.ods.common;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [순수 도메인 모델] 계좌별 잔액 이력 (Balance History)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OdsBalanceHist {
    private LocalDate baseDate;
    private String accountNo;
    private String currency;
    private BigDecimal balance;
}
