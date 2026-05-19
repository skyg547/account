package com.ho.account.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.service.journal.JournalEntryService;
import com.ho.account.journalledger.application.service.journal.JournalRuleEngine;
import com.ho.account.journalledger.application.service.journal.validator.BalanceValidationFilter;
import com.ho.account.journalledger.application.service.journal.validator.JournalValidationEngine;
import com.ho.account.journalledger.application.service.ledger.LedgerService;
import com.ho.account.journalledger.application.service.ledger.PostingService;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.repository.JournalEntryRepository;
import com.ho.account.journalledger.domain.ledger.domain.GlEntry;
import com.ho.account.journalledger.domain.ledger.domain.SlEntry;
import com.ho.account.journalledger.domain.ledger.repository.GlEntryRepository;
import com.ho.account.journalledger.domain.ledger.repository.SlEntryRepository;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.infrastructure.persistence.DeferredItemRepository;
import com.ho.account.loan.infrastructure.persistence.DeferredItemTypeRepository;
import com.ho.account.loan.infrastructure.persistence.EIRAmortizationScheduleRepository;
import com.ho.account.loan.infrastructure.persistence.LoanDisbursalRepository;
import com.ho.account.loan.infrastructure.persistence.LoanEventRepository;
import com.ho.account.loan.infrastructure.persistence.LoanRepository;
import com.ho.account.loan.infrastructure.persistence.RecalculationRunRepository;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
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
    void loanDisbursementFlowsThroughJournalLedgerPostingService() {
        InMemoryJournalPersistencePort journalStore = new InMemoryJournalPersistencePort();
        JournalEntryRepository journalEntryRepository = mock(JournalEntryRepository.class);
        when(journalEntryRepository.findById(anyLong()))
                .thenAnswer(invocation -> journalStore.findById(invocation.getArgument(0)));
        when(journalEntryRepository.save(any(JournalEntry.class)))
                .thenAnswer(invocation -> journalStore.save(invocation.getArgument(0)));

        GlEntryRepository glEntryRepository = mock(GlEntryRepository.class);
        SlEntryRepository slEntryRepository = mock(SlEntryRepository.class);
        LedgerService ledgerService = mock(LedgerService.class);
        PostingService postingService = new PostingService(
                journalEntryRepository,
                glEntryRepository,
                slEntryRepository,
                ledgerService);
        JournalEntryService journalUseCase = new JournalEntryService(
                journalStore,
                mock(JournalRuleEngine.class),
                postingService,
                new JournalValidationEngine(List.of(new BalanceValidationFilter())));

        LoanRepository loanRepository = mock(LoanRepository.class);
        LoanDisbursalRepository loanDisbursalRepository = mock(LoanDisbursalRepository.class);
        AccountSubjectPersistencePort accountSubjectPersistencePort = mock(AccountSubjectPersistencePort.class);

        LoanAccountingProperties accountingProperties = new LoanAccountingProperties();
        accountingProperties.setCashAccountCode("101900");
        accountingProperties.setLoanReceivableAccountCode("131900");

        LoanService loanService = new LoanService(
                loanRepository,
                loanDisbursalRepository,
                mock(LoanEventRepository.class),
                mock(DeferredItemTypeRepository.class),
                mock(DeferredItemRepository.class),
                mock(EIRAmortizationScheduleRepository.class),
                mock(RecalculationRunRepository.class),
                mock(EIRCalculator.class),
                mock(BusinessPartnerPersistencePort.class),
                mock(CurrencyPersistencePort.class),
                accountSubjectPersistencePort,
                accountingProperties,
                journalUseCase);

        Loan loan = new Loan();
        loan.setId(7L);
        loan.setLoanNumber("LN-E2E-001");
        loan.setCurrency(currency("KRW"));

        when(loanRepository.findById(7L)).thenReturn(Optional.of(loan));
        when(accountSubjectPersistencePort.findByCode("101900")).thenReturn(Optional.of(account("101900")));
        when(accountSubjectPersistencePort.findByCode("131900")).thenReturn(Optional.of(account("131900")));
        when(loanDisbursalRepository.save(any(LoanDisbursal.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanDisbursal disbursal = loanService.disburseLoan(
                7L,
                LocalDate.of(2026, 5, 18),
                new BigDecimal("2500000.00"),
                "loan-e2e");

        JournalEntry postedEntry = disbursal.getJournalEntry();
        assertThat(postedEntry.getStatus()).isEqualTo(JournalEntryStatus.POSTED);
        assertThat(postedEntry.getCurrencyCode()).isEqualTo("KRW");
        assertThat(postedEntry.getLineageSourceType()).isEqualTo("LOAN_DISBURSAL");
        assertThat(postedEntry.getLineageSourceId()).isEqualTo("7");

        ArgumentCaptor<Iterable<GlEntry>> glCaptor = iterableCaptor();
        verify(glEntryRepository).saveAll(glCaptor.capture());
        List<GlEntry> glEntries = toList(glCaptor.getValue());
        assertThat(glEntries).hasSize(2);
        assertThat(glEntries).anySatisfy(entry -> {
            assertThat(entry.getAccountCode()).isEqualTo("131900");
            assertThat(entry.getCurrencyCode()).isEqualTo("KRW");
            assertThat(entry.getDrAmount()).isEqualByComparingTo("2500000.00");
            assertThat(entry.getLineageSourceType()).isEqualTo("LOAN_DISBURSAL");
        });
        assertThat(glEntries).anySatisfy(entry -> {
            assertThat(entry.getAccountCode()).isEqualTo("101900");
            assertThat(entry.getCurrencyCode()).isEqualTo("KRW");
            assertThat(entry.getCrAmount()).isEqualByComparingTo("2500000.00");
            assertThat(entry.getLineageSourceId()).isEqualTo("7");
        });

        ArgumentCaptor<Iterable<SlEntry>> slCaptor = iterableCaptor();
        verify(slEntryRepository).saveAll(slCaptor.capture());
        assertThat(toList(slCaptor.getValue())).hasSize(2);
        verify(ledgerService).updateLedgerBalancesBulk(postedEntry.getDetails());
    }

    private AccountSubject account(String code) {
        AccountSubject account = new AccountSubject();
        account.setCode(code);
        account.setName("ACCOUNT-" + code);
        return account;
    }

    private Currency currency(String code) {
        Currency currency = new Currency();
        currency.setCurrencyCode(code);
        return currency;
    }

    private static <T> List<T> toList(Iterable<T> iterable) {
        List<T> values = new ArrayList<>();
        iterable.forEach(values::add);
        return values;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static <T> ArgumentCaptor<Iterable<T>> iterableCaptor() {
        return ArgumentCaptor.forClass((Class) Iterable.class);
    }

    private static class InMemoryJournalPersistencePort implements JournalPersistencePort {
        private final AtomicLong sequence = new AtomicLong(100);
        private final Map<Long, JournalEntry> entries = new LinkedHashMap<>();

        @Override
        public JournalEntry save(JournalEntry journalEntry) {
            if (journalEntry.getId() == null) {
                journalEntry.setId(sequence.incrementAndGet());
            }
            entries.put(journalEntry.getId(), journalEntry);
            return journalEntry;
        }

        @Override
        public Optional<JournalEntry> findById(Long id) {
            return Optional.ofNullable(entries.get(id));
        }

        @Override
        public Optional<JournalEntry> findByIdWithDetails(Long id) {
            return findById(id);
        }

        @Override
        public Optional<JournalEntry> findBySlipNo(String slipNo) {
            return entries.values().stream()
                    .filter(entry -> slipNo.equals(entry.getSlipNo()))
                    .findFirst();
        }

        @Override
        public List<JournalEntry> findByAccountingDateBetween(LocalDate startDate, LocalDate endDate) {
            return entries.values().stream()
                    .filter(entry -> !entry.getAccountingDate().isBefore(startDate))
                    .filter(entry -> !entry.getAccountingDate().isAfter(endDate))
                    .toList();
        }

        @Override
        public List<JournalEntry> findBySource(String sourceType, String sourceId) {
            return entries.values().stream()
                    .filter(entry -> sourceType.equals(entry.getLineageSourceType()))
                    .filter(entry -> sourceId.equals(entry.getLineageSourceId()))
                    .toList();
        }
    }
}
