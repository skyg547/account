package com.ho.account.mart.core.domain.ods.loan;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * [ODS 담보 상세] 아파트 담보 세부 속성.
 *
 * <p>초보자 설명: 담보 마스터가 "담보가 있다"는 큰 정보라면, 이 객체는 아파트 담보의 지역,
 * KB 시세, 전용면적처럼 LGD 선행 산출에 필요한 세부 입력값을 담습니다. DQ 단계는 이 값들이
 * 비어 있거나 0 이하인지 먼저 확인해 이후 ECL 산출에서 담보 인정가액이 왜곡되지 않게 막습니다.</p>
 */
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

    public boolean hasRequiredLgdInputs() {
        return missingLgdInputReasons().isEmpty();
    }

    public List<String> missingLgdInputReasons() {
        List<String> reasons = new ArrayList<>();
        if (isBlank(collateralId)) {
            reasons.add("담보 ID 누락");
        }
        if (isBlank(districtCode)) {
            reasons.add("지역 코드 누락");
        }
        if (!isPositive(kbMarketPrice)) {
            reasons.add("KB 시세 누락 또는 0 이하");
        }
        if (!isPositive(exclusiveArea)) {
            reasons.add("전용면적 누락 또는 0 이하");
        }
        return reasons;
    }

    public BigDecimal recognizedMarketPrice() {
        return isPositive(kbMarketPrice) ? kbMarketPrice : BigDecimal.ZERO;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean isPositive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}
