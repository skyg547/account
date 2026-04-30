package com.ho.account.journalledger.domain.journal.domain;

/**
 * 분개의 차변(借邊) / 대변(貸邊) 구분을 나타내는 열거형.
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 회계의 핵심 원리인 복식부기(Double-Entry Bookkeeping)에서
 * 모든 거래는 반드시 차변과 대변 양쪽에 동일한 금액으로 기록됩니다.
 *
 *   DEBIT  (차변, 借邊, Dr) — 왼쪽. 자산 증가 / 부채·자본 감소 / 비용 발생
 *   CREDIT (대변, 貸邊, Cr) — 오른쪽. 자산 감소 / 부채·자본 증가 / 수익 발생
 *
 * 예시) 현금 100만 원으로 비품 구입:
 *   Dr. 비품(자산)     1,000,000
 *   Cr.   현금(자산)             1,000,000
 *
 * 차변 합계 = 대변 합계 → 이 규칙이 깨지면 전표가 승인되지 않습니다.
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - JournalDetail.side 필드에 @Enumerated(EnumType.STRING)으로 저장됩니다.
 * - JournalEntry.validateBalance()에서 DEBIT 합계 = CREDIT 합계를 검증합니다.
 * - GlBalance.addDebit() / addCredit() 에서도 이 값을 기준으로 잔액을 갱신합니다.
 * ─────────────────────────────────────────────────
 */
public enum JournalSide {

    /** 차변 (Dr) — 자산 증가, 부채/자본 감소, 비용 발생 */
    DEBIT,

    /** 대변 (Cr) — 자산 감소, 부채/자본 증가, 수익 발생 */
    CREDIT
}
