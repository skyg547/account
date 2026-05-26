package com.risk.mart.core.domain.ods.loan;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class OdsEarlyWarning {
    private LocalDate baseDate;
    private String customerCode;
    private String warningLevel;
    private String warningReason;
    private Integer warningScore;
}
