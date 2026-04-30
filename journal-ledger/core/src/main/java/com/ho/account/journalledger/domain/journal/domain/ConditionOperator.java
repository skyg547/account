package com.ho.account.journalledger.domain.journal.domain;

/**
 * 자동 분개 규칙 조건의 비교 연산자 열거형.
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 자동 분개 규칙(JournalRule)이 특정 이벤트에 적용될지 판단할 때
 * JournalRuleCondition에 정의된 조건을 평가합니다.
 * 이 열거형은 조건 평가에 사용하는 비교 방식을 정의합니다.
 *
 * 예시:
 *   - 거래유형이 "PURCHASE"와 같으면    → EQUALS
 *   - 부서코드가 "D0"으로 시작하면       → STARTS_WITH
 *   - 금액이 1,000,000 초과이면          → GREATER_THAN
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - JournalRuleCondition.operator 필드에 @Enumerated(EnumType.STRING)으로 저장됩니다.
 * - JournalRuleEngine에서 이벤트 데이터(Map)의 특정 필드 값과 비교하는 데 사용합니다.
 * ─────────────────────────────────────────────────
 */
public enum ConditionOperator {

    /** 동일 — 값이 정확히 일치하는 경우 (예: transactionType == "PURCHASE") */
    EQUALS,

    /** 불일치 — 값이 다른 경우 (예: status != "CANCELLED") */
    NOT_EQUALS,

    /** 시작 문자 일치 — 값이 특정 문자열로 시작하는 경우 (예: deptCode.startsWith("D0")) */
    STARTS_WITH,

    /** 종료 문자 일치 — 값이 특정 문자열로 끝나는 경우 (예: accountCode.endsWith("00")) */
    ENDS_WITH,

    /** 포함 — 값에 특정 문자열이 포함된 경우 (예: description.contains("반환")) */
    CONTAINS,

    /** 초과 — 숫자 값이 기준값보다 큰 경우 (예: amount > 1000000) */
    GREATER_THAN,

    /** 이상 — 숫자 값이 기준값 이상인 경우 (예: amount >= 1000000) */
    GREATER_THAN_OR_EQUAL,

    /** 미만 — 숫자 값이 기준값보다 작은 경우 (예: amount < 100000) */
    LESS_THAN,

    /** 이하 — 숫자 값이 기준값 이하인 경우 (예: amount <= 100000) */
    LESS_THAN_OR_EQUAL
}
