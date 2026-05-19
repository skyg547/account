package com.ho.account.journalledger.application.service.journal.validator;

import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * [AccountValidityValidationFilter]
 * 전표에 사용된 계정코드의 유효성을 검증합니다.
 */
@Component
@Order(30)
@RequiredArgsConstructor
public class AccountValidityValidationFilter implements JournalValidationFilter {

    private final MasterDataQueryPort masterDataQueryPort;

    @Override
    public void validate(JournalEntry journalEntry) {
        for (JournalDetail detail : journalEntry.getDetails()) {
            String accountCode = detail.getAccountCode();
            masterDataQueryPort.findAccountSubject(accountCode)
                    .orElseThrow(() -> new IllegalStateException("유효하지 않은 계정코드입니다: " + accountCode));
            
            // 추가로 활성화 여부(active) 등을 체크할 수 있습니다.
            // if (!account.active()) throw ...
        }
    }
}
