package com.risk.mart.core.domain.ods.loan;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Builder
public class OdsBehavioralHistory {
    private Long id;
    private LocalDate baseDate;
    private String accountNo;
    private String eventType;
    private BigDecimal balanceAmount;
    private BigDecimal eventAmount;
    private LocalDateTime createdAt;
}
