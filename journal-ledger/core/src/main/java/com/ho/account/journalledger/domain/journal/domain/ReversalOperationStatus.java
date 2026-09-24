package com.ho.account.journalledger.domain.journal.domain;

/**
 * 원본 전표 하나에 연결된 역분개 작업의 수명주기입니다.
 *
 * <p>{@link JournalEntry}의 승인 상태와 별개로 역분개 권리가 현재 처리 중인지, 원장에
 * 반영되었는지, 또는 새 초안으로 다시 시작할 수 있는지를 나타냅니다.</p>
 */
public enum ReversalOperationStatus {

    /** 현재 역분개 전표가 승인·전기 흐름을 진행 중입니다. */
    PENDING,

    /** 현재 역분개 전표가 원장에 전기되어 경제적 취소가 확정되었습니다. */
    POSTED,

    /** 현재 역분개 초안이 취소되어 새 역분개 전표로 다시 시작할 수 있습니다. */
    CANCELLED
}
