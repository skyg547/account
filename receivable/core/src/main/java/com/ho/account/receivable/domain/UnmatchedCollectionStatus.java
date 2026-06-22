package com.ho.account.receivable.domain;

/**
 * 미매칭 수금의 상태를 정의하는 Enum.
 */
public enum UnmatchedCollectionStatus {
    PENDING,  // 처리 대기 중
    RESOLVED, // 해결됨 (수동 매칭 또는 기타 처리 완료)
    IGNORED   // 무시됨 (더 이상 처리 불필요)
}
