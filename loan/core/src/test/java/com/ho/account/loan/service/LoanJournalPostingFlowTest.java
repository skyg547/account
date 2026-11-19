package com.ho.account.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.out.LedgerEntryPersistencePort;
import com.ho.account.journalledger.application.service.journal.JournalEntryService;
import com.ho.account.journalledger.application.service.journal.JournalRuleEngine;
import com.ho.account.journalledger.application.service.journal.validator.BalanceValidationFilter;
import com.ho.account.journalledger.application.service.journal.validator.ClosingLockValidationFilter;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.ledger.domain.GeneralLedger;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.loan.application.port.out.LoanPersistencePort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort.AccountReference;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.infrastructure.adapter.LoanJournalAdapter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LoanJournalPostingFlowTest {

    @Test
    void closedPeriodDoesNotSaveLoanDisbursalOrJournalLineage() {
        assertDisbursalJournalFailure(true);
    }

    @Test
    void ledgerWriteFailureDoesNotSaveLoanDisbursalOrJournalLineage() {
        assertDisbursalJournalFailure(false);
    }

    private void assertDisbursalJournalFailure(boolean closedPeriod) {
        InMemoryJournalPersistencePort journalStore = new InMemoryJournalPersistencePort();
        LedgerEntryPersistencePort ledgerEntries = mock(LedgerEntryPersistencePort.class);
        LedgerService ledgerService = mock(LedgerService.class);
        AccountingPeriodStatusPort periodStatus = mock(AccountingPeriodStatusPort.class);
        LocalDate date = LocalDate.of(2026, 5, 18);
        when(periodStatus.isClosed(date)).thenReturn(closedPeriod);
        if (!closedPeriod) {
            org.mockito.Mockito.doThrow(new IllegalStateException("ledger write failed"))
                    .when(ledgerEntries).save(any());
        }
        PostingService posting = new PostingService(journalStore, ledgerEntries,
                ledgerService, new ClosingLockValidationFilter(periodStatus));
        JournalEntryService journal = new JournalEntryService(journalStore,
                mock(JournalRuleEngine.class), posting,
                new JournalValidationEngine(List.of(new BalanceValidationFilter())));
        LoanPersistencePort persistence = mock(LoanPersistencePort.class);
        LoanReferenceDataPort references = mock(LoanReferenceDataPort.class);
        LoanAccountingProperties properties = new LoanAccountingProperties();
        properties.setCashAccountCode("101900");
        properties.setLoanReceivableAccountCode("131900");
        properties.setJournalApproverActor("service:loan-checker");
        LoanService loanService = new LoanService(persistence, mock(EIRCalculator.class),
                references, properties, new LoanJournalAdapter(journal, properties));
        Loan loan = Loan.create("LN-FAIL-001", 100L, "KRW", Loan.LoanType.TERM_LOAN,
                new BigDecimal("2500000.00"), new BigDecimal("0.0450"), date,
                LocalDate.of(2027, 5, 18), Loan.PaymentFrequency.MONTHLY, "loan-maker");
        loan.setId(7L);
        when(persistence.findLoanForUpdate(7L)).thenReturn(Optional.of(loan));
        when(references.requireAccount("101900", date)).thenReturn(new AccountReference("101900", "Cash"));
        when(references.requireAccount("131900", date))
                .thenReturn(new AccountReference("131900", "Loan receivable"));

        // 채번 등 앞선 실패를 통과로 오인하지 않도록 의도한 거부 사유와 실제 호출을 함께 확인합니다.
        assertThatThrownBy(() -> loanService.disburseLoan(7L, date,
                new BigDecimal("2500000.00"), "loan-maker"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage(closedPeriod
                        ? "해당 회계 반영일(" + date + ")은 이미 마감된 기간입니다."
                        : "ledger write failed");
        verify(periodStatus).isClosed(date);
        verifyNoMoreInteractions(periodStatus);
        verify(persistence, never()).saveLoan(any());
        verify(persistence, never()).saveDisbursal(any());
        verify(ledgerService, never()).updateLedgerBalancesBulk(any());
        if (closedPeriod) {
            verify(ledgerEntries, never()).save(any());
            assertThat(journalStore.findBySlipNo("JE-20260518-00000001").orElseThrow().getStatus().name())
                    .isEqualTo("APPROVED");
        } else {
            verify(ledgerEntries).save(any());
        }
    }

    @Test
    void loanDisbursementFlowsThroughLoanJournalAdapterToLedgerPosting() {
        InMemoryJournalPersistencePort journalStore = new InMemoryJournalPersistencePort();
        LedgerEntryPersistencePort ledgerEntryPersistencePort = mock(LedgerEntryPersistencePort.class);
        LedgerService ledgerService = mock(LedgerService.class);
        AccountingPeriodStatusPort periodStatusPort = mock(AccountingPeriodStatusPort.class);
        // 기간 조회만 대체하고 실제 필터를 연결하여 Loan 경로도 전기 직전 검증을 거치게 합니다.
        PostingService postingService = new PostingService(
                journalStore,
                ledgerEntryPersistencePort,
                ledgerService,
                new ClosingLockValidationFilter(periodStatusPort));
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
        properties.setJournalApproverActor("service:loan-checker");

        LoanService loanService = new LoanService(
                persistencePort,
                mock(EIRCalculator.class),
                referenceDataPort,
                properties,
                new LoanJournalAdapter(journalUseCase, properties));

        LocalDate disbursalDate = LocalDate.of(2026, 5, 18);
        when(periodStatusPort.isClosed(disbursalDate)).thenReturn(false);
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
        assertThat(postedEntry.getCreatedBy()).isEqualTo("loan-e2e");
        assertThat(postedEntry.getApprovedBy()).isEqualTo("service:loan-checker");
        assertThat(postedEntry.getSlipNo()).isEqualTo("JE-20260518-00000001");
        assertThat(disbursal.getJournalEntrySlipNo()).isEqualTo(postedEntry.getSlipNo());
        verify(periodStatusPort).isClosed(disbursalDate);
        verifyNoMoreInteractions(periodStatusPort);

        ArgumentCaptor<GeneralLedger> ledgerCaptor = ArgumentCaptor.forClass(GeneralLedger.class);
        verify(ledgerEntryPersistencePort).save(ledgerCaptor.capture());
        assertThat(ledgerCaptor.getValue().postings()).hasSize(2);
        verify(ledgerService).updateLedgerBalancesBulk(postedEntry.getDetails());
    }

    private static class InMemoryJournalPersistencePort implements JournalPersistencePort {
        private final AtomicLong sequence = new AtomicLong(100);
        // 상세 PK 발급 횟수가 전표번호에 영향을 주지 않도록 실제 채번 계약을 별도로 흉내 냅니다.
        private final AtomicLong slipNumberSequence = new AtomicLong();
        private final Map<Long, JournalEntry> entries = new LinkedHashMap<>();

        @Override
        public long nextSlipNumber() {
            return slipNumberSequence.incrementAndGet();
        }

        @Override
        public JournalEntry save(JournalEntry entry) {
            if (entry.getId() == null) entry.setId(sequence.incrementAndGet());
            // 실제 JPA cascade 저장은 상세 PK도 발급합니다. 이 테스트 저장소도 같은 계약을
            // 흉내 내야 GeneralLedger가 source lineage 없는 transient 라인을 허용하지 않습니다.
            entry.getDetails().stream()
                    .filter(detail -> detail.getId() == null)
                    .forEach(detail -> detail.setId(sequence.incrementAndGet()));
            if (entry.getStatus() == null) entry.initializeDraft();
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
