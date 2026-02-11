package com.ho.account.reconciliation.domain;

public enum VarianceStatus {
    OPEN,
    ADJUSTED,   // 조정 전표 생성 완료
    IGNORED,    // 특정 사유로 무시됨
    REPROCESSED // 재처리로 인해 해소됨
}
