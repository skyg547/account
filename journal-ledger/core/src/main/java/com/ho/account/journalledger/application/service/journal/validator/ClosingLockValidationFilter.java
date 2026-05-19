package com.ho.account.journalledger.application.service.journal.validator;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * [ClosingLockValidationFilter]
 * 회계 기간 마감 여부를 검증합니다.
 */
@Component
@Order(20)
@RequiredArgsConstructor
public class ClosingLockValidationFilter implements JournalValidationFilter {

    private final AccountingPeriodStatusPort accountingPeriodStatusPort;

    @Override
    public void validate(JournalEntry journalEntry) {
        if (accountingPeriodStatusPort.isClosed(journalEntry.getAccountingDate())) {
            throw new IllegalStateException("해당 회계 반영일(" + journalEntry.getAccountingDate() + ")은 이미 마감된 기간입니다.");
        }
    }
}
