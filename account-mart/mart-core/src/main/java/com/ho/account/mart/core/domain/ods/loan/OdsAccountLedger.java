package com.ho.account.mart.core.domain.ods.loan;

import com.ho.account.shared.finance.enums.CrStaging;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * [여신 도메인] 계정계 원장(Ledger) 도메인 모델
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OdsAccountLedger {
    private String accountNo;
    private String customerCode;
    private String productCode;
    private String currency;
    private BigDecimal outstandingAmount;
    private BigDecimal limitAmount;
    private BigDecimal interestRate;
    private String baseRateCode;
    private BigDecimal spread;
    private LocalDate nextResetDate;
    private LocalDate openDate;
    private LocalDate maturityDate;
    private Integer delinquentDays;
    private String repaymentMethod;
    private Integer gracePeriod;
    private Integer repaymentFreq;
    private String branchCd;
    private String bizUnitCd;
    private Boolean isActive;

    public CrStaging determineStaging() {
        int days = delinquentDays != null ? delinquentDays : 0;
        if (days >= 90) {
            return CrStaging.STAGE3;
        }
        if (days >= 30) {
            return CrStaging.STAGE2;
        }
        return CrStaging.STAGE1;
    }
}
