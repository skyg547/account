package com.ho.account.expenditure.domain;

/**
 * 지급의 상태를 정의하는 Enum.
 */
public enum PaymentStatus {
    INITIATED,     // 지급 시작됨 (승인 대기)
    APPROVED,      // 지급 승인됨 (실행 대기)
    COMPLETED,     // 지급 완료됨
    FAILED,        // 지급 실패함
    CANCELLED      // 지급 취소됨
}
