package com.ho.account.receivable.domain;

/**
 * 매출 인보이스의 상태를 정의하는 Enum.
 */
public enum SalesInvoiceStatus {
    ISSUED,        // 발행됨
    PAID,          // 전액 수금됨
    PARTIAL_PAID,  // 부분 수금됨
    OVERDUE,       // 연체됨
    CANCELLED      // 취소됨
}
