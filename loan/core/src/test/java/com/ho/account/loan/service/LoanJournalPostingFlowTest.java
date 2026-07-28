package com.ho.account.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.application.service.journal.JournalEntryService;
import com.ho.account.journalledger.application.service.journal.JournalRuleEngine;
import com.ho.account.journalledger.application.service.journal.validator.BalanceValidationFilter;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.domain.SlEntry;
import com.ho.account.loan.application.port.out.LoanPersistencePort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort.AccountReference;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.infrastructure.adapter.LoanJournalAdapter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LoanJournalPostingFlowTest {

    @Test
    void loanDisbursementFlowsThroughLoanJournalAdapterToLedgerPosting() {
        InMemoryJournalPersistencePort journalStore = new InMemoryJournalPersistencePort();
        LedgerEntryPersistencePort ledgerEntryPersistencePort = mock(LedgerEntryPersistencePort.class);
        LedgerService ledgerService = mock(LedgerService.class);
        PostingService postingService = new PostingService(
                journalStore,
                ledgerEntryPersistencePort,
                ledgerService);
        JournalEntryService journalUseCase = new JournalEntryService(
                journalStore,
                mock(JournalRuleEngine.class),
                postingService,
                new JournalValidationEngine(List.of(new BalanceValidationFilter())));

        LoanPersistencePort persistencePort = mock(LoanPersistencePort.class);
        LoanReferenceDataPort referenceDataPort = mock(LoanReferenceDataPort.class);
        LoanAccountingProperties properties = new LoanAccountingProperties();
        properties.setCashAccountCode("101900");
        properties.setLoanReceivableAccountCode("131900");

        LoanService loanService = new LoanService(
                persistencePort,
                mock(EIRCalculator.class),
                referenceDataPort,
                properties,
                new LoanJournalAdapter(journalUseCase));

        LocalDate disbursalDate = LocalDate.of(2026, 5, 18);
        Loan loan = Loan.create(
                "LN-E2E-001",
                100L,
                "KRW",
                Loan.LoanType.TERM_LOAN,
                new BigDecimal("2500000.00"),
                new BigDecimal("0.0450"),
                disbursalDate,
                LocalDate.of(2027, 5, 18),
                Loan.PaymentFrequency.MONTHLY,
                "loan-e2e");
        loan.setId(7L);
        when(persistencePort.findLoanForUpdate(7L)).thenReturn(Optional.of(loan));
        when(persistencePort.existsDisbursal(7L)).thenReturn(false);
        when(referenceDataPort.requireAccount("101900", disbursalDate))
                .thenReturn(new AccountReference("101900", "Cash"));
        when(referenceDataPort.requireAccount("131900", disbursalDate))
                .thenReturn(new AccountReference("131900", "Loan receivable"));
        when(persistencePort.saveLoan(loan)).thenReturn(loan);
        when(persistencePort.saveDisbursal(any())).thenAnswer(invocation -> invocation.getArgument(0));

        LoanDisbursal disbursal = loanService.disburseLoan(
                7L,
                disbursalDate,
                new BigDecimal("2500000.00"),
                "loan-e2e");

        JournalEntry postedEntry = journalStore.findById(disbursal.getJournalEntryId()).orElseThrow();
        assertThat(postedEntry.getStatus().name()).isEqualTo("POSTED");
        assertThat(postedEntry.getSlipDate()).isEqualTo(disbursalDate);
        assertThat(postedEntry.getAccountingDate()).isEqualTo(disbursalDate);
        assertThat(postedEntry.getLineageSourceType()).isEqualTo("LOAN_DISBURSAL");
        assertThat(disbursal.getJournalEntrySlipNo()).isEqualTo(postedEntry.getSlipNo());

        ArgumentCaptor<List<GlEntry>> glCaptor = listCaptor();
        verify(ledgerEntryPersistencePort).saveGlEntries(glCaptor.capture());
        assertThat(glCaptor.getValue()).hasSize(2);
        ArgumentCaptor<List<SlEntry>> slCaptor = listCaptor();
        verify(ledgerEntryPersistencePort).saveSlEntries(slCaptor.capture());
        assertThat(slCaptor.getValue()).hasSize(2);
        verify(ledgerService).updateLedgerBalancesBulk(postedEntry.getDetails());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ArgumentCaptor<List<T>> listCaptor() {
        return ArgumentCaptor.forClass((Class) List.class);
    }

    private static class InMemoryJournalPersistencePort implements JournalPersistencePort {
        private final AtomicLong sequence = new AtomicLong(100);
        private final Map<Long, JournalEntry> entries = new LinkedHashMap<>();

        @Override
        public JournalEntry save(JournalEntry entry) {
            if (entry.getId() == null) entry.setId(sequence.incrementAndGet());
            entries.put(entry.getId(), entry);
            return entry;
        }

        @Override public Optional<JournalEntry> findById(Long id) { return Optional.ofNullable(entries.get(id)); }
        @Override public Optional<JournalEntry> findByIdWithDetails(Long id) { return findById(id); }
        @Override public Optional<JournalEntry> findBySlipNo(String slipNo) {
            return entries.values().stream().filter(entry -> slipNo.equals(entry.getSlipNo())).findFirst();
        }
        @Override public List<JournalEntry> findByAccountingDateBetween(LocalDate startDate, LocalDate endDate) {
            return entries.values().stream()
                    .filter(entry -> !entry.getAccountingDate().isBefore(startDate))
                    .filter(entry -> !entry.getAccountingDate().isAfter(endDate))
                    .toList();
        }
        @Override public List<JournalEntry> findBySource(String sourceType, String sourceId) {
            return entries.values().stream()
                    .filter(entry -> sourceType.equals(entry.getLineageSourceType()))
                    .filter(entry -> sourceId.equals(entry.getLineageSourceId()))
                    .toList();
        }
    }
}
