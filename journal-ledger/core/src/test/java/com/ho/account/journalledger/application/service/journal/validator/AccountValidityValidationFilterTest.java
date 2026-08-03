package com.ho.account.journalledger.application.service.journal.validator;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AccountValidityValidationFilterTest {

    @Test
    void validatesAccountAtJournalAccountingDate() {
        MasterDataQueryPort masterDataQueryPort = mock(MasterDataQueryPort.class);
        LocalDate accountingDate = LocalDate.of(2024, 12, 31);
        when(masterDataQueryPort.findAccountSubjectAt("1100", accountingDate))
                .thenReturn(Optional.of(new AccountSubjectRef(
                        "1100", "Historical cash", false, false, "DEBIT", "ASSETS")));
        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(accountingDate);
        JournalDetail detail = new JournalDetail();
        detail.setAccountCode("1100");
        entry.addDetail(detail);

        new AccountValidityValidationFilter(masterDataQueryPort).validate(entry);

        verify(masterDataQueryPort).findAccountSubjectAt("1100", accountingDate);
    }
}
