package com.ho.account.journalledger.domain.journal.domain;

/**
 * 전표(JournalEntry)의 생명주기 상태를 나타내는 열거형.
 *
 * ─────────────────────────────────────────────────
 * [업무 설명]
 * 회계 전표는 생성된 순간부터 원장에 반영(전기)되기까지 여러 단계를 거칩니다.
 * 각 상태는 전표가 현재 어느 단계에 있는지를 명확히 표현합니다.
 *
 * 상태 흐름:
 *   DRAFT → REQUESTED → APPROVED → POSTED
 *                    ↘ REJECTED (반려 → DRAFT/REQUESTED로 재작성)
 *   POSTED → REVERSED (역분개로 취소)
 *
 * 상태별 의미:
 *   DRAFT      - 초안. 작성 중인 전표. 아직 승인 요청 전 상태.
 *   REQUESTED  - 승인 요청됨. 결재자의 검토를 기다리는 상태.
 *   APPROVED   - 승인 완료. 전기(원장 반영)가 가능한 상태.
 *   REJECTED   - 반려. 결재자가 거부하여 수정이 필요한 상태.
 *   POSTED     - 전기 완료. GL/SL 원장에 실제로 반영된 최종 상태.
 *   REVERSED   - 역분개로 취소됨. POSTED 전표를 소급 수정할 때 사용.
 *
 * ─────────────────────────────────────────────────
 * [개발 설명]
 * - JournalEntry.status 필드에 @Enumerated(EnumType.STRING)으로 저장됩니다.
 * - 상태 전이 로직은 JournalEntry 도메인 엔티티의 requestApproval()/approve()/reject()/post() 메서드에서 관리합니다.
 * - POSTED와 REVERSED 상태의 전표는 수정·삭제가 불가합니다(불변성 보장).
 * ─────────────────────────────────────────────────
 */
public enum JournalEntryStatus {

    /** 초안 — 작성 중. 아직 승인 요청 전. */
    DRAFT,

    /** 승인 요청됨 — 결재자 검토 대기 중. */
    REQUESTED,

    /** 승인 완료 — 전기 가능 상태. */
    APPROVED,

    /** 반려 — 결재자가 거부. 수정 후 재요청 필요. */
    REJECTED,

    /** 전기 완료 — GL/SL 원장에 반영된 최종 확정 상태. */
    POSTED,

    /** 역분개 취소 — POSTED 전표를 소급 취소할 때 사용. */
    REVERSED
}
