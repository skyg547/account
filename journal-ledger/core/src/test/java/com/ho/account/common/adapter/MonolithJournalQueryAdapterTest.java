package com.ho.account.common.adapter;

import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.masterdata.AccountSubjectRef;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
class MonolithJournalQueryAdapterTest {

    @Mock
    private JournalUseCase journalUseCase;

    @Mock
    private JournalDetailRepository journalDetailRepository;

    @Mock
    private MasterDataQueryPort masterDataQueryPort;

    @Mock
    private JournalDetailRepository.JournalDetailAggregateProjection aggregateProjection;

    @Test
    void getJournalDetailAggregateUsesRepositorySummary() {
        LocalDate startDate = LocalDate.of(2026, 5, 1);
        LocalDate endDate = LocalDate.of(2026, 5, 31);
        MonolithJournalQueryAdapter adapter = new MonolithJournalQueryAdapter(journalUseCase, journalDetailRepository, masterDataQueryPort);

        when(journalDetailRepository.summarizeByAccountingDateBetweenAndSide(
                startDate,
                endDate,
                com.ho.account.journalledger.domain.journal.domain.JournalSide.DEBIT))
                .thenReturn(aggregateProjection);
        when(aggregateProjection.getDetailCount()).thenReturn(3L);
        when(aggregateProjection.getTotalAmount()).thenReturn(new BigDecimal("1250.00"));

        JournalDetailAggregateSummary summary = adapter.getJournalDetailAggregate(startDate, endDate, JournalSide.DEBIT);

        assertThat(summary.getDetailCount()).isEqualTo(3L);
        assertThat(summary.getTotalAmount()).isEqualByComparingTo("1250.00");
    }

    @Test
    void journalDetailUsesAccountingDateForHistoricalAccountCategory() {
        LocalDate accountingDate = LocalDate.of(2025, 12, 31);
        JournalEntry entry = new JournalEntry();
        entry.setAccountingDate(accountingDate);
        entry.setSlipNo("SLIP-1");
        entry.setDescription("historical entry");
        JournalDetail detail = new JournalDetail();
        detail.setJournalEntry(entry);
        detail.setAccountCode("41000");
        detail.setSide(com.ho.account.journalledger.domain.journal.domain.JournalSide.CREDIT);
        detail.setAmount(new BigDecimal("100.00"));
        detail.setBaseAmount(new BigDecimal("100.00"));
        when(journalDetailRepository.findByAccountCodesAndDateRange(
                List.of("41000"), accountingDate, accountingDate))
                .thenReturn(List.of(detail));
        when(masterDataQueryPort.findAccountSubjectAt("41000", accountingDate))
                .thenReturn(Optional.of(new AccountSubjectRef(
                        "41000", "Revenue", false, false, "CREDIT", "REVENUE")));
        MonolithJournalQueryAdapter adapter = new MonolithJournalQueryAdapter(
                journalUseCase, journalDetailRepository, masterDataQueryPort);

        List<JournalDetailSummary> result = adapter.getJournalDetailsByAccountCodes(
                accountingDate, accountingDate, List.of("41000"));

        assertThat(result).singleElement().satisfies(summary -> {
            assertThat(summary.getAccountCategory()).isEqualTo("REVENUE");
            assertThat(summary.getAccountingDate()).isEqualTo(accountingDate);
            assertThat(summary.getBaseAmount()).isEqualByComparingTo("100.00");
        });
        verify(masterDataQueryPort).findAccountSubjectAt("41000", accountingDate);
    }
}
