package com.ho.account.closing.application.port.out;

/**
 * 결산 조정 전표 생성을 journal-ledger 기술 구현으로부터 분리하는 포트.
 */
public interface ClosingJournalEntryPort {

    default java.util.Map<String, String> preflightEclLineage(java.util.List<ClosingJournalLineage> expected) {
        throw new UnsupportedOperationException("ECL lineage preflight is required");
    }

    ClosingJournalEntryResult createDraftAdjustment(ClosingJournalEntryCommand command);

    void approveAndPost(Long journalEntryId, String actor);
}
