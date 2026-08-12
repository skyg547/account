package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JournalLedgerClosingJournalEntryAdapterTest {

    @Test
    void mapsDeterministicSlipAndImmutableContractLines() {
        JournalPostingPort postingPort = mock(JournalPostingPort.class);
        JournalQueryPort queryPort = mock(JournalQueryPort.class);
        JournalLedgerClosingJournalEntryAdapter adapter =
                new JournalLedgerClosingJournalEntryAdapter(postingPort, queryPort);
        ClosingJournalEntryCommand command = command();
        when(postingPort.createDraftEntry(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new JournalPostingResult(77L, command.slipNo(), "DRAFT"));
        when(queryPort.findBySlipNo(command.slipNo())).thenReturn(Optional.empty());

        adapter.createDraftAdjustment(command);

        ArgumentCaptor<JournalEntryCommand> captor = ArgumentCaptor.forClass(JournalEntryCommand.class);
        verify(postingPort).createDraftEntry(captor.capture());
        assertThat(captor.getValue().slipNo()).isEqualTo(command.slipNo());
        assertThat(captor.getValue().auditUser()).isEqualTo("SYSTEM");
        assertThat(captor.getValue().lines()).hasSize(2);
    }

    @Test
    void exactExistingSlipIsReturnedWithoutCreatingAnotherJournal() {
        JournalPostingPort postingPort = mock(JournalPostingPort.class);
        JournalQueryPort queryPort = mock(JournalQueryPort.class);
        JournalLedgerClosingJournalEntryAdapter adapter =
                new JournalLedgerClosingJournalEntryAdapter(postingPort, queryPort);
        ClosingJournalEntryCommand command = command();
        JournalSummary summary = existingSummary(command, "DRAFT");
        List<JournalDetailSummary> details = existingDetails(command);

        when(queryPort.findBySlipNo(command.slipNo())).thenReturn(Optional.of(summary));
        when(queryPort.getJournalDetails(77L)).thenReturn(details);

        var result = adapter.createDraftAdjustment(command);

        assertThat(result.journalEntryId()).isEqualTo(77L);
        assertThat(result.slipNo()).isEqualTo(command.slipNo());
        verifyNoInteractions(postingPort);
    }

    @Test
    void existingSlipWithDifferentLineageFailsClosed() {
        JournalPostingPort postingPort = mock(JournalPostingPort.class);
        JournalQueryPort queryPort = mock(JournalQueryPort.class);
        JournalLedgerClosingJournalEntryAdapter adapter =
                new JournalLedgerClosingJournalEntryAdapter(postingPort, queryPort);
        ClosingJournalEntryCommand command = command();
        JournalSummary summary = existingSummary(command, "DRAFT");
        summary.setLineageSourceId("different-lineage");
        List<JournalDetailSummary> details = existingDetails(command);

        when(queryPort.findBySlipNo(command.slipNo())).thenReturn(Optional.of(summary));
        when(queryPort.getJournalDetails(77L)).thenReturn(details);

        assertThatThrownBy(() -> adapter.createDraftAdjustment(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different business content");
        verifyNoInteractions(postingPort);
    }

    @Test
    void postingDelegatesToPostingPortApproveAndPost() {
        JournalPostingPort postingPort = mock(JournalPostingPort.class);
        JournalQueryPort queryPort = mock(JournalQueryPort.class);
        JournalLedgerClosingJournalEntryAdapter adapter =
                new JournalLedgerClosingJournalEntryAdapter(postingPort, queryPort);

        adapter.approveAndPost(77L, "SYSTEM");

        verify(postingPort).approveAndPost(77L, "SYSTEM");
    }

    private ClosingJournalEntryCommand command() {
        LocalDate date = LocalDate.of(2026, 5, 31);
        return new ClosingJournalEntryCommand(
                date,
                date,
                "FX closing",
                "CLOSING_ADJUSTMENT",
                "BATCH",
                "SYSTEM",
                "FX_VALUATION",
                "77|11000|EUR",
                "KRW",
                "FXV20260531ABCDEF123",
                List.of(
                        new ClosingJournalLineCommand(
                                ClosingJournalSide.DEBIT,
                                "11000",
                                new BigDecimal("10.00"),
                                new BigDecimal("10.00"),
                                "debit"),
                        new ClosingJournalLineCommand(
                                ClosingJournalSide.CREDIT,
                                "72000",
                                new BigDecimal("10.00"),
                                new BigDecimal("10.00"),
                                "credit")));
    }

    private JournalSummary existingSummary(ClosingJournalEntryCommand command, String status) {
        JournalSummary summary = new JournalSummary();
        summary.setId(77L);
        summary.setSlipNo(command.slipNo());
        summary.setSlipDate(command.slipDate());
        summary.setAccountingDate(command.accountingDate());
        summary.setDescription(command.description());
        summary.setEntryType(command.entryType());
        summary.setCurrencyCode(command.currencyCode());
        summary.setLineageSourceType(command.lineageSourceType());
        summary.setLineageSourceId(command.lineageSourceId());
        summary.setStatus(status);
        return summary;
    }

    private List<JournalDetailSummary> existingDetails(ClosingJournalEntryCommand command) {
        return command.lines().stream().map(line -> {
            JournalDetailSummary detail = new JournalDetailSummary();
            detail.setSide(line.side() == ClosingJournalSide.DEBIT ? JournalSide.DEBIT : JournalSide.CREDIT);
            detail.setAccountCode(line.accountCode());
            detail.setAmount(line.amount());
            detail.setBaseAmount(line.baseAmount());
            detail.setDetailDescription(line.description());
            return detail;
        }).toList();
    }
}
