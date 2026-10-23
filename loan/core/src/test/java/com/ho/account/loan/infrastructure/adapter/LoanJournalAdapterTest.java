package com.ho.account.loan.infrastructure.adapter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.outbox.OutboxPort;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.loan.application.port.out.LoanJournalPort.LoanJournalCommand;
import com.ho.account.loan.application.port.out.LoanJournalPort.LoanJournalLine;
import com.ho.account.loan.service.LoanAccountingProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

class LoanJournalAdapterTest {

    private final JournalUseCase journal = mock(JournalUseCase.class);
    private final OutboxPort outbox = mock(OutboxPort.class);
    private final LoanAccountingProperties properties = new LoanAccountingProperties();

    @Test
    void rejectsMissingOrMalformedMachineCheckerBeforeAnyWrite() {
        for (String actor : new String[] {null, " ", "checker", "service:", "service:bad actor",
                "service:" + "a".repeat(50)}) {
            properties.setJournalApproverActor(actor);
            assertThatThrownBy(() -> adapter().post(command("loan-maker")))
                    .as("checker=%s", actor)
                    .isInstanceOf(RuntimeException.class);
        }
        verifyNoInteractions(outbox, journal);
    }

    @Test
    void rejectsCanonicalSameActorBeforeAnyWrite() {
        properties.setJournalApproverActor(" SERVICE:LOAN-CHECKER ");
        assertThatThrownBy(() -> adapter().post(command(" service:loan-checker ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must differ");
        verifyNoInteractions(outbox, journal);
    }

    @Test
    void requestsApprovalAsMakerThenApprovesWithConfiguredCheckerBeforePosting() {
        properties.setJournalApproverActor(" SERVICE:LOAN-CHECKER ");
        JournalEntry saved = savedEntry();
        when(journal.createJournalEntry(any())).thenReturn(saved);
        when(journal.getJournalEntryWithDetails(17L)).thenReturn(java.util.Optional.of(saved));

        adapter().post(command("loan-maker"));

        InOrder order = inOrder(journal);
        order.verify(journal).createJournalEntry(any());
        order.verify(journal).requestJournalEntryApproval(17L, "loan-maker");
        order.verify(journal).approveJournalEntry(17L, "service:loan-checker");
        order.verify(journal).postJournalEntry(17L, "loan-maker");
    }

    @Test
    void approvalFailureNeverPostsOrPublishes() {
        properties.setJournalApproverActor("service:loan-checker");
        when(journal.createJournalEntry(any())).thenReturn(savedEntry());
        org.mockito.Mockito.doThrow(new IllegalStateException("approval rejected"))
                .when(journal).approveJournalEntry(17L, "service:loan-checker");

        assertThatThrownBy(() -> adapter().post(command("loan-maker")))
                .hasMessage("approval rejected");
        verify(journal, never()).postJournalEntry(any(), any());
        verify(outbox, never()).markJournalEventAsPublished(any(), any());
    }

    @Test
    void postingFailureNeverPublishesOrReportsSuccess() {
        properties.setJournalApproverActor("service:loan-checker");
        when(journal.createJournalEntry(any())).thenReturn(savedEntry());
        org.mockito.Mockito.doThrow(new IllegalStateException("ledger write failed"))
                .when(journal).postJournalEntry(17L, "loan-maker");

        assertThatThrownBy(() -> adapter().post(command("loan-maker")))
                .hasMessage("ledger write failed");
        verify(outbox, never()).markJournalEventAsPublished(any(), any());
    }

    private LoanJournalAdapter adapter() {
        return new LoanJournalAdapter(journal, outbox, properties);
    }

    private static JournalEntry savedEntry() {
        JournalEntry entry = new JournalEntry();
        entry.setId(17L);
        entry.setSlipNo("LOAN-17");
        return entry;
    }

    private static LoanJournalCommand command(String maker) {
        return new LoanJournalCommand(LocalDate.of(2026, 5, 18), "Loan disbursal", maker,
                "LOAN_DISBURSAL", "7", "KRW", List.of(
                new LoanJournalLine("DEBIT", "131900", new BigDecimal("1000"), "Receivable"),
                new LoanJournalLine("CREDIT", "101900", new BigDecimal("1000"), "Cash")));
    }
}
