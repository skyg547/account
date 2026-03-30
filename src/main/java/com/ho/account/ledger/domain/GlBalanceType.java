package com.ho.account.ledger.domain;

/**
 * GL 잔액 타입을 정의하는 Enum.
 * 차변(DEBIT) 잔액 또는 대변(CREDIT) 잔액을 나타냅니다.
 */
public enum GlBalanceType {
    DEBIT,  // 차변 잔액
    CREDIT  // 대변 잔액
}
