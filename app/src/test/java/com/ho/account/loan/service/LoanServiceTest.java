package com.ho.account.loan.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Currency;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.CurrencyRepository;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.repository.JournalDetailRepository;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.domain.LoanEvent;
import com.ho.account.loan.domain.RecalculationRun;
import com.ho.account.loan.repository.DeferredItemRepository;
import com.ho.account.loan.repository.DeferredItemTypeRepository;
import com.ho.account.loan.repository.EIRAmortizationScheduleRepository;
import com.ho.account.loan.repository.LoanDisbursalRepository;
import com.ho.account.loan.repository.LoanEventRepository;
import com.ho.account.loan.repository.LoanRepository;
import com.ho.account.loan.repository.RecalculationRunRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private LoanDisbursalRepository loanDisbursalRepository;
    @Mock private LoanEventRepository loanEventRepository;
    @Mock private DeferredItemTypeRepository deferredItemTypeRepository;
    @Mock private DeferredItemRepository deferredItemRepository;
    @Mock private EIRAmortizationScheduleRepository eirAmortizationScheduleRepository;
    @Mock private RecalculationRunRepository recalculationRunRepository;
    @Mock private BusinessPartnerRepository businessPartnerRepository;
    @Mock private CurrencyRepository currencyRepository;
    @Mock private AccountSubjectRepository accountSubjectRepository;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private JournalDetailRepository journalDetailRepository;
    @Mock private EIRCalculator eirCalculator;

    @InjectMocks
    private LoanService loanService;

    private Loan testLoan;
    private BusinessPartner testBp;
    private Currency testCurrency;
    private AccountSubject cashAccount;
    private AccountSubject loanReceivableAccount;
    private AccountSubject interestIncomeAccount;
    private AccountSubject deferredAssetAccount;
    private DeferredItemType defaultDeferredItemType;

    @BeforeEach
    void setUp() {
        testBp = new BusinessPartner();
        testBp.setId(1L);
        testBp.setBusinessPartnerName("Test Borrower");

        testCurrency = new Currency();
        testCurrency.setCurrencyCode("KRW");
        testCurrency.setCurrencyName("Korean Won");

        testLoan = new Loan();
        testLoan.setId(1L);
        testLoan.setLoanNumber("LN001");
        testLoan.setBusinessPartner(testBp);
        testLoan.setCurrency(testCurrency);
        testLoan.setLoanType(Loan.LoanType.TERM_LOAN);
        testLoan.setPrincipalAmount(BigDecimal.valueOf(1_000_000));
        testLoan.setInterestRate(BigDecimal.valueOf(0.05));
        testLoan.setDisbursalDate(LocalDate.of(2023, 1, 1));
        testLoan.setMaturityDate(LocalDate.of(2023, 4, 1));
        testLoan.setPaymentFrequency(Loan.PaymentFrequency.MONTHLY);
        testLoan.setInitialEIR(BigDecimal.valueOf(0.05));
        testLoan.setCurrentEIR(BigDecimal.valueOf(0.05));
        testLoan.setStatus(Loan.LoanStatus.ACTIVE);

        cashAccount = new AccountSubject();
        cashAccount.setCode("101000");
        cashAccount.setName("Cash");

        loanReceivableAccount = new AccountSubject();
        loanReceivableAccount.setCode("131000");
        loanReceivableAccount.setName("Loan Receivable");

        interestIncomeAccount = new AccountSubject();
        interestIncomeAccount.setCode("401000");
        interestIncomeAccount.setName("Interest Income");

        deferredAssetAccount = new AccountSubject();
        deferredAssetAccount.setCode("171000");
        deferredAssetAccount.setName("Deferred Asset");

        defaultDeferredItemType = new DeferredItemType();
        defaultDeferredItemType.setId(100L);
        defaultDeferredItemType.setCode("LOAN_ORIGINATION_FEE");
        defaultDeferredItemType.setName("Origination Fee");
        defaultDeferredItemType.setDeferralMethod(DeferredItemType.DeferralMethod.EIR_METHOD);
        defaultDeferredItemType.setDeferredAssetAccount(deferredAssetAccount);
        defaultDeferredItemType.setRecognizedIncomeAccount(interestIncomeAccount);
        defaultDeferredItemType.setActive(true);
    }

    @Test
    void testCreateLoan() {
        when(businessPartnerRepository.findById(anyLong())).thenReturn(Optional.of(testBp));
        when(currencyRepository.findById(anyString())).thenReturn(Optional.of(testCurrency));
        when(loanRepository.save(any(Loan.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Loan createdLoan = loanService.createLoan(testLoan);

        assertNotNull(createdLoan);
        assertEquals("LN001", createdLoan.getLoanNumber());
        assertEquals(Loan.LoanStatus.ACTIVE, createdLoan.getStatus());
        assertEquals(BigDecimal.valueOf(0.05), createdLoan.getInitialEIR());
        verify(loanRepository, times(1)).save(testLoan);
    }

    @Test
    void testDisburseLoan() {
        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(testLoan));
        when(accountSubjectRepository.findById("101000")).thenReturn(Optional.of(cashAccount));
        when(accountSubjectRepository.findById("131000")).thenReturn(Optional.of(loanReceivableAccount));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            entry.setId(10L);
            return entry;
        });
        when(journalEntryRepository.count()).thenReturn(1L);
        when(loanDisbursalRepository.save(any(LoanDisbursal.class))).thenAnswer(invocation -> {
            LoanDisbursal disbursal = invocation.getArgument(0);
            disbursal.setId(1L);
            return disbursal;
        });

        LoanDisbursal disbursal = loanService.disburseLoan(1L, LocalDate.of(2023, 1, 1), BigDecimal.valueOf(1_000_000), "user");

        assertNotNull(disbursal);
        assertNotNull(disbursal.getJournalEntry());
        verify(loanDisbursalRepository, times(1)).save(any(LoanDisbursal.class));
        verify(journalEntryRepository, times(1)).save(any(JournalEntry.class));
    }

    @Test
    void testCreateDeferredItem() {
        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(testLoan));
        when(deferredItemTypeRepository.findById(anyLong())).thenReturn(Optional.of(defaultDeferredItemType));
        when(accountSubjectRepository.findById("101000")).thenReturn(Optional.of(cashAccount));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            entry.setId(11L);
            return entry;
        });
        when(journalEntryRepository.count()).thenReturn(2L);
        when(deferredItemRepository.save(any(DeferredItem.class))).thenAnswer(invocation -> {
            DeferredItem deferredItem = invocation.getArgument(0);
            deferredItem.setId(2L);
            return deferredItem;
        });

        DeferredItem deferredItem = loanService.createDeferredItem(
                1L,
                100L,
                BigDecimal.valueOf(10_000),
                LocalDate.of(2023, 1, 1),
                LocalDate.of(2023, 4, 1),
                "user");

        assertNotNull(deferredItem);
        assertEquals(BigDecimal.valueOf(10_000), deferredItem.getAmount());
        assertNotNull(deferredItem.getInitialJournalEntry());
        verify(deferredItemRepository, times(1)).save(any(DeferredItem.class));
    }

    @Test
    void testGenerateAmortizationSchedule() {
        DeferredItem deferredItem = new DeferredItem();
        deferredItem.setRemainingAmount(BigDecimal.valueOf(10_000));
        deferredItem.setStatus(DeferredItem.DeferredItemStatus.AMORTIZING);

        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(testLoan));
        when(accountSubjectRepository.findById("131000")).thenReturn(Optional.of(loanReceivableAccount));
        when(accountSubjectRepository.findById("401000")).thenReturn(Optional.of(interestIncomeAccount));
        when(accountSubjectRepository.findById("171000")).thenReturn(Optional.of(deferredAssetAccount));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            entry.setId(System.nanoTime());
            return entry;
        });
        when(journalEntryRepository.count()).thenReturn(3L);
        when(deferredItemRepository.findByLoan(any(Loan.class))).thenReturn(List.of(deferredItem));
        when(eirAmortizationScheduleRepository.findByLoan(any(Loan.class))).thenReturn(List.of());
        when(eirAmortizationScheduleRepository.save(any(EIRAmortizationSchedule.class))).thenAnswer(invocation -> {
            EIRAmortizationSchedule schedule = invocation.getArgument(0);
            schedule.setId(System.nanoTime());
            return schedule;
        });
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        List<EIRAmortizationSchedule> schedule = loanService.generateAmortizationSchedule(
                1L,
                LocalDate.of(2023, 1, 1),
                BigDecimal.valueOf(0.05),
                "user");

        assertNotNull(schedule);
        assertFalse(schedule.isEmpty());
        assertEquals(3, schedule.size());
        verify(eirAmortizationScheduleRepository, times(3)).save(any(EIRAmortizationSchedule.class));
        verify(journalEntryRepository, times(3)).save(any(JournalEntry.class));
    }

    @Test
    void testRecalculateLoan() {
        DeferredItem deferredItem = new DeferredItem();
        deferredItem.setRemainingAmount(BigDecimal.valueOf(8_000));
        deferredItem.setStatus(DeferredItem.DeferredItemStatus.AMORTIZING);

        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);
        when(recalculationRunRepository.save(any(RecalculationRun.class))).thenAnswer(invocation -> {
            RecalculationRun run = invocation.getArgument(0);
            run.setId(3L);
            return run;
        });
        when(accountSubjectRepository.findById("101000")).thenReturn(Optional.of(cashAccount));
        when(accountSubjectRepository.findById("131000")).thenReturn(Optional.of(loanReceivableAccount));
        when(accountSubjectRepository.findById("401000")).thenReturn(Optional.of(interestIncomeAccount));
        when(accountSubjectRepository.findById("171000")).thenReturn(Optional.of(deferredAssetAccount));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            entry.setId(System.nanoTime());
            return entry;
        });
        when(journalEntryRepository.count()).thenReturn(10L);
        when(deferredItemRepository.findByLoan(any(Loan.class))).thenReturn(List.of(deferredItem));
        when(eirAmortizationScheduleRepository.findByLoan(any(Loan.class))).thenReturn(List.of());
        when(eirAmortizationScheduleRepository.save(any(EIRAmortizationSchedule.class))).thenAnswer(invocation -> {
            EIRAmortizationSchedule schedule = invocation.getArgument(0);
            schedule.setId(System.nanoTime());
            return schedule;
        });
        when(eirCalculator.calculateEIR(any(Loan.class), any())).thenReturn(BigDecimal.valueOf(0.0510));

        RecalculationRun run = loanService.recalculateLoan(
                1L,
                LocalDate.of(2023, 2, 1),
                RecalculationRun.RecalculationReason.EARLY_REPAYMENT,
                "user",
                Optional.of(BigDecimal.valueOf(950_000)),
                Optional.empty());

        assertNotNull(run);
        assertEquals(BigDecimal.valueOf(0.0510), run.getNewEIR());
        assertNotNull(run.getAdjustmentJournalEntry());
        verify(recalculationRunRepository, times(1)).save(any(RecalculationRun.class));
    }

    @Test
    void testReproduceDoDScenario() {
        DeferredItem deferredItem = new DeferredItem();
        deferredItem.setRemainingAmount(BigDecimal.valueOf(7_000));
        deferredItem.setStatus(DeferredItem.DeferredItemStatus.AMORTIZING);

        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(testLoan));
        when(deferredItemTypeRepository.findByCode(anyString())).thenReturn(Optional.of(defaultDeferredItemType));
        when(deferredItemTypeRepository.findById(anyLong())).thenReturn(Optional.of(defaultDeferredItemType));
        when(accountSubjectRepository.findById("101000")).thenReturn(Optional.of(cashAccount));
        when(accountSubjectRepository.findById("131000")).thenReturn(Optional.of(loanReceivableAccount));
        when(accountSubjectRepository.findById("401000")).thenReturn(Optional.of(interestIncomeAccount));
        when(accountSubjectRepository.findById("171000")).thenReturn(Optional.of(deferredAssetAccount));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(invocation -> {
            JournalEntry entry = invocation.getArgument(0);
            entry.setId(System.nanoTime());
            return entry;
        });
        when(journalEntryRepository.count()).thenReturn(1L);
        when(deferredItemRepository.save(any(DeferredItem.class))).thenAnswer(invocation -> {
            DeferredItem item = invocation.getArgument(0);
            item.setId(10L);
            return item;
        });
        when(deferredItemRepository.findByLoan(any(Loan.class))).thenReturn(List.of(deferredItem));
        when(loanEventRepository.save(any(LoanEvent.class))).thenAnswer(invocation -> {
            LoanEvent event = invocation.getArgument(0);
            event.setId(20L);
            return event;
        });
        when(eirAmortizationScheduleRepository.findByLoan(any(Loan.class))).thenReturn(List.of());
        when(eirAmortizationScheduleRepository.save(any(EIRAmortizationSchedule.class))).thenAnswer(invocation -> {
            EIRAmortizationSchedule schedule = invocation.getArgument(0);
            schedule.setId(System.nanoTime());
            return schedule;
        });
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);
        when(recalculationRunRepository.save(any(RecalculationRun.class))).thenAnswer(invocation -> {
            RecalculationRun run = invocation.getArgument(0);
            run.setId(30L);
            return run;
        });
        when(eirCalculator.calculateEIR(any(Loan.class), any())).thenReturn(BigDecimal.valueOf(0.0510));

        RecalculationRun finalRun = loanService.reproduceDoDScenario(1L, "dod_tester");

        assertNotNull(finalRun);
        assertEquals(testLoan.getLoanNumber(), finalRun.getLoan().getLoanNumber());
        assertEquals(RecalculationRun.RecalculationReason.EARLY_REPAYMENT, finalRun.getReason());
        verify(deferredItemRepository, atLeastOnce()).save(any(DeferredItem.class));
        verify(loanEventRepository, times(1)).save(any(LoanEvent.class));
        verify(recalculationRunRepository, times(1)).save(any(RecalculationRun.class));
    }
}
