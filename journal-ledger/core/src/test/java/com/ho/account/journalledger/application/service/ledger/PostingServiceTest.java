package com.ho.account.journalledger.application.service.ledger;

import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.domain.ledger.domain.GeneralLedger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostingServiceTest {

    @Mock
    private JournalPersistencePort journalPersistencePort;
    @Mock
    private LedgerEntryPersistencePort ledgerEntryPersistencePort;
    @Mock
    private LedgerService ledgerService;

    private PostingService service;

    @BeforeEach
    void setUp() {
        service = new PostingService(journalPersistencePort, ledgerEntryPersistencePort, ledgerService);
    }

    @Test
    @DisplayName("승인된 전표를 POSTED로 전환하고 GL/SL 엔트리와 잔액을 생성한다.")
    void postsApprovedJournalEntry() {
        JournalEntry entry = approvedEntry();
        when(journalPersistencePort.findByIdWithDetails(1L)).thenReturn(Optional.of(entry));

        service.postJournalEntry(1L, "poster-1");

        assertThat(entry.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(entry.getAuditUser()).isEqualTo("poster-1");
        verify(journalPersistencePort).save(entry);

        ArgumentCaptor<GeneralLedger> ledgerCaptor = ArgumentCaptor.forClass(GeneralLedger.class);
        verify(ledgerEntryPersistencePort).save(ledgerCaptor.capture());
        verify(ledgerService).updateLedgerBalancesBulk(entry.getDetails());

        GeneralLedger ledger = ledgerCaptor.getValue();
        assertThat(ledger.journalEntryId()).isEqualTo(1L);
        assertThat(ledger.postings()).hasSize(2);
        assertThat(ledger.postings().get(0).debit().amount()).isEqualByComparingTo("100.00");
        assertThat(ledger.postings().get(0).credit().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ledger.postings().get(1).debit().amount()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ledger.postings().get(1).credit().amount()).isEqualByComparingTo("100.00");
        assertThat(ledger.postings()).allSatisfy(posting -> {
            assertThat(posting.postingDate()).isEqualTo(LocalDate.of(2026, 5, 10));
            assertThat(posting.fiscalYear()).isEqualTo("2026");
            assertThat(posting.fiscalPeriod()).isEqualTo("05");
            assertThat(posting.lineageSourceType()).isEqualTo("UNIT_TEST");
            assertThat(posting.lineageSourceId()).isEqualTo("SRC-1");
        });
        assertThat(ledger.postings().get(0).baseDebit().amount()).isEqualByComparingTo("100.00");
        assertThat(ledger.postings().get(1).baseCredit().amount()).isEqualByComparingTo("100.00");
    }

    private JournalEntry approvedEntry() {
        JournalEntry entry = new JournalEntry();
        entry.setId(1L);
        entry.setSlipNo("JE-20260510-0001");
        entry.setSlipDate(LocalDate.of(2026, 5, 10));
        entry.setAccountingDate(LocalDate.of(2026, 5, 10));
        entry.setCurrencyCode("KRW");
        entry.setLineageSourceType("UNIT_TEST");
        entry.setLineageSourceId("SRC-1");
        entry.addDetail(detail(11L, JournalSide.DEBIT, "10100"));
        entry.addDetail(detail(12L, JournalSide.CREDIT, "40100"));
        entry.initializeDraft();
        entry.approve("approver-1");
        return entry;
    }

    private JournalDetail detail(Long id, JournalSide side, String accountCode) {
        JournalDetail detail = new JournalDetail();
        detail.setId(id);
        detail.setSide(side);
        detail.setAccountCode(accountCode);
        detail.setAmount(new BigDecimal("100.00"));
        detail.setBaseAmount(new BigDecimal("100.00"));
        return detail;
    }
}
