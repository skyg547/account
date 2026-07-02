package com.ho.account.mart.core.domain.ods.loan;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

/**
 * [ODS 담보 상세] 아파트 담보 세부 속성.
 *
 * <p>초보자 설명: 현재는 원천 모델을 보존하는 domain 객체이며, 실제 LGD 담보 인정가액 산출 흐름에는
 * 아직 연결되어 있지 않다.</p>
 *
 * @todo account-mart LGD 선행 데이터 고도화 시 이 객체를 실제 담보 DQ/LGD 흐름에 연결하고,
 *       application port와 infrastructure adapter, seed/test를 함께 추가해야 한다.
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
}
