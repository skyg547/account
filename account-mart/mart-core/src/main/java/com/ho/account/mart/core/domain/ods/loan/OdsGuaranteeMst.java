package com.ho.account.mart.core.domain.ods.loan;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
public class OdsGuaranteeMst {
    private String guaranteeNo;
    private String accountNo;
    private String guarantorId;
    private String guarantorName;
    private BigDecimal guaranteeAmount;
    private BigDecimal guaranteeRatio;
    private LocalDate startDate;
    private LocalDate endDate;
    private String guarantorRating;
}
