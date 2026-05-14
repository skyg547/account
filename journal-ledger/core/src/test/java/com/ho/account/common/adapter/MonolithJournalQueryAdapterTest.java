package com.ho.account.common.adapter;

import com.ho.account.contracts.journal.JournalDetailAggregateSummary;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.masterdata.MasterDataQueryPort;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.repository.JournalDetailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
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
}