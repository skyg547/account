package com.ho.account.journalledger.application.service.journal.validator;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;

/**
 * [JournalValidationFilter]
 * 전표 검증을 위한 개별 필터 인터페이스.
 */
public interface JournalValidationFilter {
    /**
     * 전표의 유효성을 검증합니다.
     * @param journalEntry 검증할 전표
     * @throws RuntimeException 검증 실패 시 관련 예외 발생
     */
    void validate(JournalEntry journalEntry);
}
