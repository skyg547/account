package com.ho.account.journalledger.application.service.journal.validator;

import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * [BalanceValidationFilter]
 * 차대변 합계 일치 여부를 검증합니다.
 */
@Component
@Order(10)
public class BalanceValidationFilter implements JournalValidationFilter {
    @Override
    public void validate(JournalEntry journalEntry) {
        // 도메인 엔티티의 핵심 로직을 재사용합니다.
        journalEntry.validateBalance();
    }
}
