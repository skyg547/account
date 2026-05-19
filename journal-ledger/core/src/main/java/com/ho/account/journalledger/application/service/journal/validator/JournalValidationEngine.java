package com.ho.account.journalledger.application.service.journal.validator;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * [JournalValidationEngine]
 * 등록된 모든 검증 필터를 순차적으로 실행하는 엔진입니다.
 */
@Component
@RequiredArgsConstructor
public class JournalValidationEngine {

    private final List<JournalValidationFilter> filters;

    /**
     * 모든 필터를 실행하여 전표를 검증합니다.
     * @param journalEntry 검증할 전표
     */
    public void validate(JournalEntry journalEntry) {
        for (JournalValidationFilter filter : filters) {
            filter.validate(journalEntry);
        }
    }
}
