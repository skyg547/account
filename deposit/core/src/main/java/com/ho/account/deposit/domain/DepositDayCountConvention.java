package com.ho.account.deposit.domain;

/**
 * [도메인 열거형] 예금 이자 일할 계산 일수 계산 기준 (Day-Count Convention).
 *
 * 💡 [초보자를 위한 금융 회계 설명]
 * 은행에서 하루 단위로 예금 이자를 입혀줄 때, 1년을 몇 일로 볼지에 대한 규정입니다.
 * - ACTUAL_365: 실제 경과일수 / 365일 (원화 예금 표준)
 * - ACTUAL_360: 실제 경과일수 / 360일 (외화 예금 및 일부 유가증권 표준)
 */
public enum DepositDayCountConvention {
    /** 1년 365일 기준 (원화 예금 기본) */
    ACTUAL_365(365),

    /** 1년 360일 기준 (외화 예금 기본) */
    ACTUAL_360(360);

    private final int daysPerYear;

    DepositDayCountConvention(int daysPerYear) {
        this.daysPerYear = daysPerYear;
    }

    public int getDaysPerYear() {
        return daysPerYear;
    }
}
