package com.ho.account.ecl.core.domain.calculator;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * [Value Object] IFRS 9 대손충당금 산출 모델 파라미터.
 */
@Getter
@Builder
public class AllowanceModelParams {

    /**
     * PD 하한선.
     */
    @Builder.Default
    private final BigDecimal pdFloor = new BigDecimal("0.0005");

    /**
     * 담보부 익스포저 LGD 하한선.
     */
    @Builder.Default
    private final BigDecimal securedLgdFloor = new BigDecimal("0.20");

    /**
     * 무담보 익스포저 LGD 기준값.
     */
    @Builder.Default
    private final BigDecimal unsecuredLgdFloor = new BigDecimal("0.45");

    /**
     * ECL 산출 시 사용할 기본 할인율.
     */
    @Builder.Default
    private final BigDecimal defaultDiscountRate = new BigDecimal("0.05");

    /**
     * 상품별 CCF 마스터가 없을 때 적용하는 보수적 기본 신용전환계수.
     * 서비스 코드에 숫자를 직접 두지 않고 모델 정책으로 이름을 부여합니다.
     */
    @Builder.Default
    private final BigDecimal defaultCcfRate = new BigDecimal("0.75");
}
