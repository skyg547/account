package com.risk.mart.core.domain.ods.common;

import lombok.*;

/**
 * [순수 도메인 모델] 고객 마스터 (Customer Master)
 * 💡 [초보자를 위한 금융 가이드]
 * 리스크 산출 엔진이 사용하는 순수한 고객 정보 객체입니다. 
 * 특정 기술(JPA)에 종속되지 않아 리스크 모델러들이 비즈니스 로직에만 집중할 수 있게 합니다.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class OdsCustomerMst {
    private String customerCode;
    private String bizNo;
    private String customerName;
    private String customerType;
    private String countryCode;
    private String internalRating;
    private String ratingCode;
    private String externalRating;
    private String industryCode;
    private String industryName;
    private Boolean isSme;
    private String branchCode;
    private String creditStatusCd;
}
