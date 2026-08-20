package com.ho.account.contracts.journal;

/**
 * [JournalPostingPort] 전표 생성 및 전기(Posting) 아웃바운드 계약 포트.
 *
 * 🐣 [초보자를 위한 개념 설명]
 * 복식부기 회계에서 '전표(Journal Entry)'는 모든 재무 거래의 최초 기록 단위입니다.
 * 타 도메인 서비스(예금 입출금, 대출 실행/이자, 지출결의, 고정자산 감가상각, 결산 조정 등)에서 회계적 사건이
 * 발생했을 때, 직접 회계 테이블에 손을 대는 대신 이 포트를 통해 전표를 발행하고 승인/전기합니다.
 *
 * 🏛️ [헥사고날 아키텍처 및 트랜잭션 경계 이점]
 * 1. 서비스 간 느슨한 결합(Loose Coupling): 각 비즈니스 마이크로서비스는 `journal-ledger`의 내부 엔티티를 알 필요 없이
 *    표준화된 `JournalEntryCommand`를 통해서만 회계 원장에 전표를 요청합니다.
 * 2. 2단계 확정(Two-Phase Commitment): `createDraftEntry`로 임시 초안(Draft)을 생성한 후,
 *    업무 검증/결재 완료 시 `approveAndPost`를 호출하여 총계정원장(GL/SL)에 확정 반영합니다.
 * 3. 멱등성 및 트랜잭셔널 아웃박스(Transactional Outbox): MSA 환경에서 메시지 유실 없는 안전한 분산 회계 처리를 지원합니다.
 */
public interface JournalPostingPort {

    /**
     * 회계 거래에 대한 초안(DRAFT) 전표를 생성합니다.
     *
     * @param command 전표 헤더, 차대변 라인 정보 및 트레이스 메타데이터를 담은 커맨드
     * @return 생성된 전표 ID 및 임시 전표 번호 결과를 담은 불변 객체
     */
    JournalPostingResult createDraftEntry(JournalEntryCommand command);

    /**
     * 지정된 초안 전표를 최종 승인하고 총계정원장(GL)에 공식 전기(POSTED)합니다.
     *
     * @param journalEntryId 승인할 대상 전표의 고유 식별자
     * @param actor 전기를 실행/승인한 작업자 계정 식별자
     */
    void approveAndPost(Long journalEntryId, String actor);
}
