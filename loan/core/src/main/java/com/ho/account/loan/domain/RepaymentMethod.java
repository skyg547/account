package com.ho.account.loan.domain;

/**
 * [도메인 열거형] 대출 원리금 상환 방식.
 *
 * 💡 [초보자를 위한 개념 설명]
 * 은행 대출은 상환 방식에 따라 매월 내는 원금과 이자의 비율이 달라집니다.
 * 1. EQUAL_PRINCIPAL_AND_INTEREST (원리금 균등상환): 매월 원금+이자 합계액(PMT)이 동일함.
 * 2. EQUAL_PRINCIPAL (원금 균등상환): 매월 상환하는 원금이 동일하며, 이자는 잔액 감소에 따라 줄어듦.
 * 3. BULLET_MATURITY (만기 일시상환): 대출 기간 동안 이자만 납부하다가 만기일에 원금 전액을 한 번에 상환함.
 */
public enum RepaymentMethod {
    /** 원리금 균등상환 (매월 원금 + 이자 합계 일정) */
    EQUAL_PRINCIPAL_AND_INTEREST,

    /** 원금 균등상환 (매월 상환 원금 일정) */
    EQUAL_PRINCIPAL,

    /** 만기 일시상환 (기간 중 이자만, 만기 시 원금 일시 상환) */
    BULLET_MATURITY
}
