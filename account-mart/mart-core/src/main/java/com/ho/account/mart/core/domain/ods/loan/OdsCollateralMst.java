package com.ho.account.mart.core.domain.ods.loan;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OdsCollateralMst {
    private String collateralNo;
    private String customerCode;
    private String collateralType;
    private String currency;
    private BigDecimal appraisedValue;
    private BigDecimal pledgedAmount;
    private LocalDate valuationDate;
    private Boolean isActive;
}
