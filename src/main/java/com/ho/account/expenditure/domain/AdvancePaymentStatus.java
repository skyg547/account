package com.ho.account.expenditure.domain;

/**
 * 선급금의 상태를 정의하는 Enum.
 */
public enum AdvancePaymentStatus {
    ACTIVE,  // 활성 상태 (상계 가능)
    OFFSET,  // 전액 또는 부분 상계됨
    REFUNDED // 전액 환불됨
}
