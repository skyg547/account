package com.ho.account.closing.batch.adapter.out;

import com.ho.account.closing.application.port.out.ClosingJournalEntryCommand;
import com.ho.account.closing.application.port.out.ClosingJournalLineCommand;
import com.ho.account.closing.application.port.out.ClosingJournalSide;
import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalPostingPort;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class JournalLedgerClosingJournalEntryAdapterTest {

    @Test
    void mapsDeterministicSlipAndImmutableContractLines() {
        JournalPostingPort postingPort = mock(JournalPostingPort.class);
        JournalUseCase journalUseCase = mock(JournalUseCase.class);
        JournalLedgerClosingJournalEntryAdapter adapter =
                new JournalLedgerClosingJournalEntryAdapter(postingPort, journalUseCase);
        ClosingJournalEntryCommand command = command();
        when(postingPort.createDraftEntry(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new JournalPostingResult(77L, command.slipNo(), "DRAFT"));

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
        JournalUseCase journalUseCase = mock(JournalUseCase.class);
        JournalLedgerClosingJournalEntryAdapter adapter =
                new JournalLedgerClosingJournalEntryAdapter(postingPort, journalUseCase);
        ClosingJournalEntryCommand command = command();
        JournalEntry existing = existing(command, JournalEntryStatus.DRAFT);
        when(journalUseCase.getJournalEntryBySlipNo(command.slipNo()))
                .thenReturn(Optional.of(existing));
        when(journalUseCase.getJournalEntryWithDetails(77L)).thenReturn(Optional.of(existing));

        var result = adapter.createDraftAdjustment(command);

        assertThat(result.journalEntryId()).isEqualTo(77L);
        assertThat(result.slipNo()).isEqualTo(command.slipNo());
        verifyNoInteractions(postingPort);
    }

    @Test
    void existingSlipWithDifferentLineageFailsClosed() {
        JournalPostingPort postingPort = mock(JournalPostingPort.class);
        JournalUseCase journalUseCase = mock(JournalUseCase.class);
        JournalLedgerClosingJournalEntryAdapter adapter =
                new JournalLedgerClosingJournalEntryAdapter(postingPort, journalUseCase);
        ClosingJournalEntryCommand command = command();
        JournalEntry existing = existing(command, JournalEntryStatus.DRAFT);
        existing.setLineageSourceId("different-lineage");
        when(journalUseCase.getJournalEntryBySlipNo(command.slipNo()))
                .thenReturn(Optional.of(existing));
        when(journalUseCase.getJournalEntryWithDetails(77L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> adapter.createDraftAdjustment(command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("different business content");
        verifyNoInteractions(postingPort);
    }

    @Test
    void postingIsIdempotentAcrossDraftApprovedAndPostedStates() {
        JournalPostingPort postingPort = mock(JournalPostingPort.class);
        JournalUseCase journalUseCase = mock(JournalUseCase.class);
        JournalLedgerClosingJournalEntryAdapter adapter =
                new JournalLedgerClosingJournalEntryAdapter(postingPort, journalUseCase);
        ClosingJournalEntryCommand command = command();

        when(journalUseCase.getJournalEntry(77L))
                .thenReturn(Optional.of(existing(command, JournalEntryStatus.DRAFT)));
        adapter.approveAndPost(77L, "SYSTEM");
        verify(journalUseCase).approveJournalEntry(77L, "SYSTEM");
        verify(journalUseCase).postJournalEntry(77L, "SYSTEM");

        JournalUseCase postedUseCase = mock(JournalUseCase.class);
        JournalLedgerClosingJournalEntryAdapter postedAdapter =
                new JournalLedgerClosingJournalEntryAdapter(postingPort, postedUseCase);
        when(postedUseCase.getJournalEntry(77L))
                .thenReturn(Optional.of(existing(command, JournalEntryStatus.POSTED)));
        postedAdapter.approveAndPost(77L, "SYSTEM");
        verify(postedUseCase, never()).approveJournalEntry(77L, "SYSTEM");
        verify(postedUseCase, never()).postJournalEntry(77L, "SYSTEM");
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

    private JournalEntry existing(
            ClosingJournalEntryCommand command,
            JournalEntryStatus status) {
        JournalEntry entry = new JournalEntry();
        entry.setId(77L);
        entry.setSlipNo(command.slipNo());
        entry.setSlipDate(command.slipDate());
        entry.setAccountingDate(command.accountingDate());
        entry.setDescription(command.description());
        entry.setEntryType(command.entryType());
        entry.setCurrencyCode(command.currencyCode());
        entry.setLineageSourceType(command.lineageSourceType());
        entry.setLineageSourceId(command.lineageSourceId());
        entry.setStatus(status);
        entry.setDetails(command.lines().stream().map(line -> {
            JournalDetail detail = new JournalDetail();
            detail.setJournalEntry(entry);
            detail.setSide(line.side() == ClosingJournalSide.DEBIT
                    ? JournalSide.DEBIT
                    : JournalSide.CREDIT);
            detail.setAccountCode(line.accountCode());
            detail.setAmount(line.amount());
            detail.setBaseAmount(line.baseAmount());
            detail.setDetailDescription(line.description());
            return detail;
        }).toList());
        return entry;
    }
}
