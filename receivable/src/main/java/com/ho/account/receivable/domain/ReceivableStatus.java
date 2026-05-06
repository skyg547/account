package com.ho.account.receivable.domain;

/**
 * 매출채권의 상태를 정의하는 Enum.
 */
public enum ReceivableStatus {
    OPEN,          // 미수금 존재
    PARTIAL_PAID,  // 부분 수금됨
    PAID,          // 전액 수금됨
    OVERDUE,       // 만기일 경과
    WRITTEN_OFF    // 대손 처리됨
}
