package com.ho.account.expenditure.domain;

/**
 * 지급 실행 (Payment Run)의 상태를 정의하는 Enum.
 */
public enum PaymentRunStatus {
    INITIATED,  // 시작됨 (지급 대상 선정 완료)
    PROCESSING, // 지급 처리 중
    COMPLETED,  // 지급 실행 완료
    FAILED      // 지급 실행 실패
}
