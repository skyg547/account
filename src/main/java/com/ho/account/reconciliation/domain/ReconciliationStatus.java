package com.ho.account.reconciliation.domain;

public enum ReconciliationStatus {
    PENDING,
    IN_PROGRESS,
    SUCCESS,
    PARTIAL_SUCCESS, // 분개는 일치하나, 차이가 있는 경우 (조정 필요)
    FAILURE,         // 심각한 오류로 대사 실패
    VARIANCE_FOUND   // 차이가 발견되어 조정이 필요한 상태
}
