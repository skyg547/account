package com.ho.account.mart.core.domain.ods.common;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [순수 도메인 모델] 총계정원장 (General Ledger)
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OdsGeneralLedger {
    private LocalDate baseDate;
    private String glCode;
    private String currency;
    private BigDecimal balance;
    private String branchCode;
}
