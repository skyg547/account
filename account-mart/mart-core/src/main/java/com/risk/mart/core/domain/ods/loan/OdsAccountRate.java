package com.risk.mart.core.domain.ods.loan;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class OdsAccountRate {
    private String accountNo;
    private String rateType;
    private String baseRateCode;
    private BigDecimal spread;
    private BigDecimal appliedRate;
    private BigDecimal interestRateCap;
    private BigDecimal interestRateFloor;
    private String refIndexCode;
    private Integer paymentFreq;
    private Integer rateResetCycle;
    private LocalDate lastResetDate;
    private LocalDate nextResetDate;
}
