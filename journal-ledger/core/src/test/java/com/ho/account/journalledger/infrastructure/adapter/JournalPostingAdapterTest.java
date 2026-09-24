package com.ho.account.journalledger.infrastructure.adapter;

import com.ho.account.contracts.journal.JournalEntryCommand;
import com.ho.account.contracts.journal.JournalLineCommand;
import com.ho.account.contracts.journal.JournalPostingResult;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JournalPostingAdapterTest {

    @Mock
    private JournalUseCase journalUseCase;

    private JournalPostingAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new JournalPostingAdapter(journalUseCase);
    }

    @Test
    void createDraftEntryMapsCommandFieldsAndBaseAmount() {
        when(journalUseCase.createJournalEntry(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            entry.setId(77L);
            entry.setSlipNo("JE-20260511-0001");
            entry.initializeDraft();
            return entry;
        });

        JournalEntryCommand command = new JournalEntryCommand(
                LocalDate.of(2026, 5, 12),
                LocalDate.of(2026, 5, 11),
                "reconciliation adjustment",
                "ADJUSTMENT",
                "KRW",
                new BigDecimal("1.00"),
                "tester",
                "tester",
                "RECONCILIATION",
                "RECON_ADJ-1",
                List.of(
                        new JournalLineCommand("DEBIT", "131000", new BigDecimal("50.00"), new BigDecimal("50.00"), null, null, "debit"),
                        new JournalLineCommand("CREDIT", "211000", new BigDecimal("50.00"), new BigDecimal("50.00"), null, null, "credit")
                )
        );

        JournalPostingResult result = adapter.createDraftEntry(command);

        assertThat(result.journalEntryId()).isEqualTo(77L);
        assertThat(result.slipNo()).isEqualTo("JE-20260511-0001");
        assertThat(result.status()).isEqualTo("DRAFT");

        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalUseCase).createJournalEntry(entryCaptor.capture());
        JournalEntry entry = entryCaptor.getValue();

        assertThat(entry.getSlipDate()).isEqualTo(LocalDate.of(2026, 5, 12));
        assertThat(entry.getAccountingDate()).isEqualTo(LocalDate.of(2026, 5, 11));
        assertThat(entry.getEntryType()).isEqualTo("ADJUSTMENT");
        assertThat(entry.getCurrencyCode()).isEqualTo("KRW");
        assertThat(entry.getExchangeRate()).isEqualByComparingTo("1.00");
        assertThat(entry.getCreatedBy()).isEqualTo("tester");
        assertThat(entry.getAuditUser()).isEqualTo("tester");
        assertThat(entry.getLineageSourceType()).isEqualTo("RECONCILIATION");
        assertThat(entry.getLineageSourceId()).isEqualTo("RECON_ADJ-1");
        assertThat(entry.getDetails()).hasSize(2);
        assertThat(entry.getDetails()).allSatisfy(detail ->
                assertThat(detail.getAuditUser()).isEqualTo("tester"));
        assertThat(entry.getDetails()).anySatisfy(detail -> {
            assertThat(detail.getSide()).isEqualTo(JournalSide.DEBIT);
            assertThat(detail.getAccountCode()).isEqualTo("131000");
            assertThat(detail.getBaseAmount()).isEqualByComparingTo("50.00");
        });
        assertThat(entry.getDetails()).anySatisfy(detail -> {
            assertThat(detail.getSide()).isEqualTo(JournalSide.CREDIT);
            assertThat(detail.getAccountCode()).isEqualTo("211000");
            assertThat(detail.getBaseAmount()).isEqualByComparingTo("50.00");
        });
    }

    @Test
    void machineDraftUsesControlledSubmissionBeforeDistinctCheckerApprovalAndPosting() {
        JournalEntry entry = new JournalEntry();
        entry.setCreatedBy("Service:Closing-Maker");
        entry.initializeDraft();
        when(journalUseCase.getJournalEntry(77L)).thenReturn(java.util.Optional.of(entry));

        adapter.approveAndPost(77L, " Service:Closing-Checker ");

        var order = inOrder(journalUseCase);
        order.verify(journalUseCase).getJournalEntry(77L);
        order.verify(journalUseCase).requestJournalEntryApproval(77L, "service:closing-maker");
        order.verify(journalUseCase).approveJournalEntry(77L, "service:closing-checker");
        order.verify(journalUseCase).postJournalEntry(77L, "service:closing-checker");
    }

    @Test
    void machineDraftRejectsSameCanonicalMakerAndCheckerBeforeAnyTransition() {
        JournalEntry entry = new JournalEntry();
        entry.setCreatedBy("SYSTEM");
        entry.initializeDraft();
        when(journalUseCase.getJournalEntry(77L)).thenReturn(java.util.Optional.of(entry));

        assertThatThrownBy(() -> adapter.approveAndPost(77L, " system "))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("distinct");

        verify(journalUseCase, never()).requestJournalEntryApproval(any(), any());
        verify(journalUseCase, never()).approveJournalEntry(any(), any());
        verify(journalUseCase, never()).postJournalEntry(any(), any());
    }
}
