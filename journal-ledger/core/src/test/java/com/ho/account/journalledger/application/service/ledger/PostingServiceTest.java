package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.domain.SlEntry;
import com.ho.account.journalledger.domain.ledger.repository.GlEntryRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlEntryRepository;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostingServiceTest {

    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private GlEntryRepository glEntryRepository;
    @Mock
    private SlEntryRepository slEntryRepository;
    @Mock
    private LedgerService ledgerService;

    private PostingService service;

    @BeforeEach
    void setUp() {
        service = new PostingService(journalEntryRepository, glEntryRepository, slEntryRepository, ledgerService);
    }

    @Test
    @DisplayName("승인된 전표를 POSTED로 전환하고 GL/SL 엔트리와 잔액을 생성한다.")
    void postsApprovedJournalEntry() {
        JournalEntry entry = approvedEntry();
        when(journalEntryRepository.findById(1L)).thenReturn(Optional.of(entry));

        service.postJournalEntry(1L, "poster-1");

        assertThat(entry.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(entry.getAuditUser()).isEqualTo("poster-1");
        verify(journalEntryRepository).save(entry);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<GlEntry>> glEntriesCaptor = ArgumentCaptor.forClass(List.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SlEntry>> slEntriesCaptor = ArgumentCaptor.forClass(List.class);

        verify(glEntryRepository).saveAll(glEntriesCaptor.capture());
        verify(slEntryRepository).saveAll(slEntriesCaptor.capture());
        verify(ledgerService).updateLedgerBalancesBulk(entry.getDetails());

        List<GlEntry> glEntries = glEntriesCaptor.getValue();
        List<SlEntry> slEntries = slEntriesCaptor.getValue();

        assertThat(glEntries).hasSize(2);
        assertThat(glEntries.get(0).getDrAmount()).isEqualByComparingTo("100.00");
        assertThat(glEntries.get(0).getCrAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(glEntries.get(1).getDrAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(glEntries.get(1).getCrAmount()).isEqualByComparingTo("100.00");
        assertThat(glEntries).allSatisfy(glEntry -> {
            assertThat(glEntry.getPostingDate()).isEqualTo(LocalDate.of(2026, 5, 10));
            assertThat(glEntry.getFiscalYear()).isEqualTo("2026");
            assertThat(glEntry.getFiscalPeriod()).isEqualTo("05");
            assertThat(glEntry.getLineageSourceType()).isEqualTo("UNIT_TEST");
            assertThat(glEntry.getLineageSourceId()).isEqualTo("SRC-1");
        });

        assertThat(slEntries).hasSize(2);
        assertThat(slEntries.get(0).getBaseDrAmount()).isEqualByComparingTo("100.00");
        assertThat(slEntries.get(1).getBaseCrAmount()).isEqualByComparingTo("100.00");
    }

    private JournalEntry approvedEntry() {
        Currency currency = new Currency();
        currency.setCurrencyCode("KRW");

        JournalEntry entry = new JournalEntry();
        entry.setId(1L);
        entry.setSlipNo("JE-20260510-0001");
        entry.setSlipDate(LocalDate.of(2026, 5, 10));
        entry.setAccountingDate(LocalDate.of(2026, 5, 10));
        entry.setStatus(JournalEntryStatus.APPROVED);
        entry.setCurrency(currency);
        entry.setLineageSourceType("UNIT_TEST");
        entry.setLineageSourceId("SRC-1");
        entry.addDetail(detail(JournalSide.DEBIT, "10100"));
        entry.addDetail(detail(JournalSide.CREDIT, "40100"));
        return entry;
    }

    private JournalDetail detail(JournalSide side, String accountCode) {
        AccountSubject account = new AccountSubject();
        account.setCode(accountCode);

        JournalDetail detail = new JournalDetail();
        detail.setSide(side);
        detail.setAccountSubject(account);
        detail.setAmount(new BigDecimal("100.00"));
        detail.setBaseAmount(new BigDecimal("100.00"));
        return detail;
    }
}
