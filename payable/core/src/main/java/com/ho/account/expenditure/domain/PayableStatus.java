package com.ho.account.expenditure.domain;

/**
 * 매입채무의 상태를 정의하는 Enum.
 */
public enum PayableStatus {
    OPEN,          // 미지급금 존재
    PARTIAL_PAID,  // 부분 지급됨
    PAID,          // 전액 지급됨
    OVERDUE,       // 만기일 경과
    WRITTEN_OFF    // 상각 처리됨 (대손과 유사)
}
