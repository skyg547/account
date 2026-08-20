package com.ho.account.shared;

/**
 * [BoundedContext] 도메인 주도 설계(DDD) 기반 바운디드 컨텍스트 식별자.
 *
 * 🐣 [초보자를 위한 개념 설명]
 * 도메인 주도 설계(DDD)에서 '바운디드 컨텍스트(Bounded Context)'는 특정 비즈니스 도메인의 모델과 용어(Ubiquitous Language)가
 * 유효한 논리적/물리적 경계를 의미합니다.
 * 이 열거형은 각 마이크로서비스 모듈이 어떤 핵심 도메인 영역을 소유하고 있는지 명시적으로 식별합니다.
 */
public enum BoundedContext {
    MASTER_DATA,
    GOVERNANCE,
    JOURNAL_LEDGER,
    RECEIVABLE,
    PAYABLE,
    ASSET_LEASE,
    LOAN,
    CLOSING,
    RECONCILIATION,
    REPORTING,
    TAX,
    EXPENDITURE_RESOLUTION
}
