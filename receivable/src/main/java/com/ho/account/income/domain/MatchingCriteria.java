package com.ho.account.income.domain;

/**
 * 수금과 매출채권을 매칭하기 위한 기준을 정의하는 Enum.
 */
public enum MatchingCriteria {
    REFERENCE_NO_EXACT, // 참조 번호 정확히 일치
    CUSTOMER_CODE,      // 고객 코드 일치
    AMOUNT_EXACT,       // 금액 정확히 일치
    AMOUNT_FUZZY,       // 금액 허용 오차 범위 내 일치
    VIRTUAL_ACCOUNT     // 가상 계좌 일치
}
