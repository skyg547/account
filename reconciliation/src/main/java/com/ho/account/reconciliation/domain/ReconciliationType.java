package com.ho.account.reconciliation.domain;

public enum ReconciliationType {
    SOURCE_STANDARD,    // 원천-표준-전표-원장 대사
    ACCOUNT_TOTALS,     // 계정 대사 (계정 합계)
    BANK_ACCOUNT        // 은행 계좌 대사 (통장 vs 장부)
}
