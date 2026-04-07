package com.ho.account.income.domain;

/**
 * 수금(입금)의 상태를 정의하는 Enum.
 */
public enum CollectionStatus {
    RECEIVED,         // 수신됨 (아직 매칭되지 않음)
    MATCHED,          // 완전히 매칭됨
    PARTIAL_MATCHED,  // 부분적으로 매칭됨 (잔액이 남음)
    UNMATCHED,        // 매칭되지 않음 (수동 처리 필요)
    CANCELLED         // 취소됨
}
