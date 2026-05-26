package com.risk.mart.core.domain.ods.loan;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class OdsApartCollDetail {
    private String collateralId;
    private String districtCode;
    private BigDecimal kbMarketPrice;
    private String houseType;
    private BigDecimal exclusiveArea;
    private Integer floorNo;
    private Boolean isSpeculativeArea;
}
