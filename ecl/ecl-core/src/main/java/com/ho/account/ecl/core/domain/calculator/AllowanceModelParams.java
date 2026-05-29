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
}
