package com.ho.account.closing.application.service;

import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AnnualClosingServiceTest {

    @Test
    void closesOnlyPostedIncomeStatementBaseBalancesInStableOrder() {
        JournalQueryPort queryPort = mock(JournalQueryPort.class);
        JournalPostingPort postingPort = mock(JournalPostingPort.class);
        AnnualClosingService service = new AnnualClosingService(queryPort, postingPort);
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        JournalSummary posted = summary(1L, "POSTED");
        JournalSummary draft = summary(2L, "DRAFT");
        when(queryPort.getJournalSummaries(startDate, endDate)).thenReturn(List.of(draft, posted));
        when(queryPort.getJournalDetails(1L)).thenReturn(List.of(
                detail("51000", "EXPENSES", JournalSide.DEBIT, "300.00"),
                detail("41000", "REVENUE", JournalSide.CREDIT, "1000.00")));

        service.performIncomeStatementClosing(2026, "35000");

        verify(queryPort, never()).getJournalDetails(2L);
        ArgumentCaptor<JournalEntryCommand> captor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(postingPort).createDraftEntry(captor.capture());
        JournalEntryCommand command = captor.getValue();
        assertThat(command.slipDate()).isEqualTo(endDate);
        assertThat(command.slipNo()).startsWith("ACL20261231").hasSize(20);
        assertThat(command.lines()).extracting(line -> line.accountCode())
                .containsExactly("41000", "51000", "35000");
        assertThat(command.lines().get(0).drcrType()).isEqualTo("DEBIT");
        assertThat(command.lines().get(1).drcrType()).isEqualTo("CREDIT");
        assertThat(command.lines().get(2).drcrType()).isEqualTo("CREDIT");
        assertThat(command.lines().get(2).amount()).isEqualByComparingTo("700.00");
    }

    @Test
    void repeatedAnnualClosingReturnsExistingDeterministicDraft() {
        JournalQueryPort queryPort = mock(JournalQueryPort.class);
        JournalPostingPort postingPort = mock(JournalPostingPort.class);
        AnnualClosingService service = new AnnualClosingService(queryPort, postingPort);
        LocalDate startDate = LocalDate.of(2026, 1, 1);
        LocalDate endDate = LocalDate.of(2026, 12, 31);
        JournalSummary existing = summary(7L, "DRAFT");
        existing.setSlipNo(ClosingSlipNoFactory.annualClosing(endDate, 2026, "35000"));
        existing.setAccountingDate(endDate);
        existing.setDescription("2026년 손익 대체 분개");
        existing.setEntryType("TRANSFER");
        when(queryPort.getJournalSummaries(startDate, endDate)).thenReturn(List.of(existing));

        service.performIncomeStatementClosing(2026, "35000");

        verify(queryPort, never()).getJournalDetails(7L);
        verifyNoInteractions(postingPort);
    }

    private JournalSummary summary(Long id, String status) {
        JournalSummary summary = new JournalSummary();
        summary.setId(id);
        summary.setStatus(status);
        return summary;
    }

    private JournalDetailSummary detail(
            String accountCode,
            String category,
            JournalSide side,
            String baseAmount) {
        JournalDetailSummary detail = new JournalDetailSummary();
        detail.setAccountCode(accountCode);
        detail.setAccountCategory(category);
        detail.setSide(side);
        detail.setAmount(new BigDecimal(baseAmount));
        detail.setBaseAmount(new BigDecimal(baseAmount));
        return detail;
    }
}
