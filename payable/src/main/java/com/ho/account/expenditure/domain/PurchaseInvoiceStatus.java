package com.ho.account.expenditure.domain;

/**
 * 매입 인보이스의 상태를 정의하는 Enum.
 */
public enum PurchaseInvoiceStatus {
    RECEIVED,      // 수신됨 (지급 대기)
    APPROVED,      // 승인됨 (지급 준비 완료)
    PAID,          // 전액 지급됨
    PARTIAL_PAID,  // 부분 지급됨
    OVERDUE,       // 만기일 경과
    CANCELLED      // 취소됨
}
