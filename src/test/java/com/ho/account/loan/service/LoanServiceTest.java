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
import com.ho.account.loan.domain.*;
import com.ho.account.loan.domain.DeferredItemType.DeferralMethod;
import com.ho.account.loan.domain.Loan.LoanStatus;
import com.ho.account.loan.domain.Loan.PaymentFrequency;
import com.ho.account.loan.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

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
        testLoan.setPrincipalAmount(BigDecimal.valueOf(1000000));
        testLoan.setInterestRate(BigDecimal.valueOf(0.05));
        testLoan.setDisbursalDate(LocalDate.of(2023, 1, 1));
        testLoan.setMaturityDate(LocalDate.of(2024, 1, 1));
        testLoan.setPaymentFrequency(PaymentFrequency.MONTHLY);
        testLoan.setInitialEIR(BigDecimal.valueOf(0.05));
        testLoan.setCurrentEIR(BigDecimal.valueOf(0.05));
        testLoan.setStatus(LoanStatus.ACTIVE);

        cashAccount = new AccountSubject();
        cashAccount.setCode("101000");
        cashAccount.setName("보통예금");

        loanReceivableAccount = new AccountSubject();
        loanReceivableAccount.setCode("131000");
        loanReceivableAccount.setName("대출채권");

        interestIncomeAccount = new AccountSubject();
        interestIncomeAccount.setCode("401000");
        interestIncomeAccount.setName("이자수익");

        deferredAssetAccount = new AccountSubject();
        deferredAssetAccount.setCode("171000");
        deferredAssetAccount.setName("이연대출부대손익");

        defaultDeferredItemType = new DeferredItemType();
        defaultDeferredItemType.setId(100L);
        defaultDeferredItemType.setCode("LOAN_ORIGINATION_FEE");
        defaultDeferredItemType.setName("대출 실행 수수료");
        defaultDeferredItemType.setDeferralMethod(DeferralMethod.EIR_METHOD);
        defaultDeferredItemType.setDeferredAssetAccount(deferredAssetAccount);
        defaultDeferredItemType.setRecognizedIncomeAccount(interestIncomeAccount);
        defaultDeferredItemType.setActive(true);
    }

    @Test
    void testCreateLoan() {
        when(businessPartnerRepository.findById(anyLong())).thenReturn(Optional.of(testBp));
        when(currencyRepository.findById(anyString())).thenReturn(Optional.of(testCurrency));
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);

        Loan createdLoan = loanService.createLoan(testLoan);

        assertNotNull(createdLoan);
        assertEquals("LN001", createdLoan.getLoanNumber());
        assertEquals(LoanStatus.ACTIVE, createdLoan.getStatus());
        verify(loanRepository, times(1)).save(testLoan);
    }

    @Test
    void testDisburseLoan() {
        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(testLoan));
        when(accountSubjectRepository.findById("101000")).thenReturn(Optional.of(cashAccount));
        when(accountSubjectRepository.findById("131000")).thenReturn(Optional.of(loanReceivableAccount));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(i -> {
            JournalEntry je = i.getArgument(0);
            je.setId(10L); // Simulate ID generation
            return je;
        });
        when(journalEntryRepository.count()).thenReturn(1L);
        when(loanDisbursalRepository.save(any(LoanDisbursal.class))).thenAnswer(i -> {
            LoanDisbursal ld = i.getArgument(0);
            ld.setId(1L);
            return ld;
        });

        LoanDisbursal disbursal = loanService.disburseLoan(1L, LocalDate.of(2023, 1, 1), BigDecimal.valueOf(1000000), "user");

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
        when(accountSubjectRepository.findById("171000")).thenReturn(Optional.of(deferredAssetAccount));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(i -> {
            JournalEntry je = i.getArgument(0);
            je.setId(11L); // Simulate ID
            return je;
        });
        when(journalEntryRepository.count()).thenReturn(2L);
        when(deferredItemRepository.save(any(DeferredItem.class))).thenAnswer(i -> {
            DeferredItem di = i.getArgument(0);
            di.setId(2L);
            return di;
        });

        DeferredItem deferredItem = loanService.createDeferredItem(1L, 100L, BigDecimal.valueOf(10000), LocalDate.of(2023, 1, 1), LocalDate.of(2024, 1, 1), "user");

        assertNotNull(deferredItem);
        assertEquals(BigDecimal.valueOf(10000), deferredItem.getAmount());
        assertNotNull(deferredItem.getInitialJournalEntry());
        verify(deferredItemRepository, times(1)).save(any(DeferredItem.class));
        verify(journalEntryRepository, times(1)).save(any(JournalEntry.class));
    }

    @Test
    void testGenerateAmortizationSchedule() {
        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(testLoan));
        when(accountSubjectRepository.findById("131000")).thenReturn(Optional.of(loanReceivableAccount));
        when(accountSubjectRepository.findById("401000")).thenReturn(Optional.of(interestIncomeAccount));
        when(accountSubjectRepository.findById("171000")).thenReturn(Optional.of(deferredAssetAccount));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(i -> {
            JournalEntry je = i.getArgument(0);
            je.setId(System.currentTimeMillis()); // Unique ID
            return je;
        });
        when(journalEntryRepository.count()).thenReturn(3L); // For slipNo
        when(deferredItemRepository.findByLoan(any(Loan.class))).thenReturn(Arrays.asList(new DeferredItem())); // For deferred item amortization
        when(eirAmortizationScheduleRepository.save(any(EIRAmortizationSchedule.class))).thenAnswer(i -> {
            EIRAmortizationSchedule schedule = i.getArgument(0);
            schedule.setId(System.currentTimeMillis() + 1000);
            return schedule;
        });
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);


        List<EIRAmortizationSchedule> schedule = loanService.generateAmortizationSchedule(1L, LocalDate.of(2023, 1, 1), BigDecimal.valueOf(0.05), "user");

        assertNotNull(schedule);
        assertFalse(schedule.isEmpty());
        assertEquals(3, schedule.size());
        verify(eirAmortizationScheduleRepository, times(3)).save(any(EIRAmortizationSchedule.class));
        verify(journalEntryRepository, times(6)).save(any(JournalEntry.class)); // 3 for principal/interest, 3 for deferred
    }

    @Test
    void testRecalculateLoan() {
        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(testLoan));
        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan);
        when(recalculationRunRepository.save(any(RecalculationRun.class))).thenAnswer(i -> {
            RecalculationRun rr = i.getArgument(0);
            rr.setId(3L);
            return rr;
        });
        // Mock for generateAmortizationSchedule internal call
        when(accountSubjectRepository.findById("131000")).thenReturn(Optional.of(loanReceivableAccount));
        when(accountSubjectRepository.findById("401000")).thenReturn(Optional.of(interestIncomeAccount));
        when(accountSubjectRepository.findById("171000")).thenReturn(Optional.of(deferredAssetAccount));
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(i -> {
            JournalEntry je = i.getArgument(0);
            je.setId(System.currentTimeMillis());
            return je;
        });
        when(journalEntryRepository.count()).thenReturn(10L);
        when(deferredItemRepository.findByLoan(any(Loan.class))).thenReturn(Arrays.asList(new DeferredItem()));
        when(eirAmortizationScheduleRepository.save(any(EIRAmortizationSchedule.class))).thenAnswer(i -> {
            EIRAmortizationSchedule schedule = i.getArgument(0);
            schedule.setId(System.currentTimeMillis() + 2000);
            return schedule;
        });

        RecalculationRun run = loanService.recalculateLoan(1L, LocalDate.of(2023, 4, 1), RecalculationRun.RecalculationReason.EARLY_REPAYMENT, "user",
                Optional.of(BigDecimal.valueOf(950000)), Optional.empty());

        assertNotNull(run);
        assertEquals(BigDecimal.valueOf(0.05).add(BigDecimal.valueOf(0.001)), run.getNewEIR());
        verify(recalculationRunRepository, times(1)).save(any(RecalculationRun.class));
        verify(loanRepository, times(1)).save(any(Loan.class)); // loan updated with new EIR
    }

    @Test
    void testReproduceDoDScenario() {
        when(loanRepository.findById(anyLong())).thenReturn(Optional.of(testLoan));
        when(deferredItemTypeRepository.findByCode(anyString())).thenReturn(Optional.of(defaultDeferredItemType));
        when(deferredItemTypeRepository.save(any(DeferredItemType.class))).thenReturn(defaultDeferredItemType); // for createDefaultDeferredItemType

        when(accountSubjectRepository.findById(anyString())).thenReturn(Optional.of(cashAccount)); // general mock for accounts
        when(journalEntryRepository.save(any(JournalEntry.class))).thenAnswer(i -> {
            JournalEntry je = i.getArgument(0);
            je.setId(System.currentTimeMillis());
            return je;
        });
        when(journalEntryRepository.count()).thenReturn(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L); // Multiple calls for various JE creations

        when(deferredItemRepository.save(any(DeferredItem.class))).thenAnswer(i -> {
            DeferredItem di = i.getArgument(0);
            di.setId(10L);
            return di;
        });
        when(deferredItemRepository.findByLoan(any(Loan.class))).thenReturn(Arrays.asList(new DeferredItem()));


        when(loanEventRepository.save(any(LoanEvent.class))).thenAnswer(i -> {
            LoanEvent le = i.getArgument(0);
            le.setId(20L);
            return le;
        });

        when(eirAmortizationScheduleRepository.save(any(EIRAmortizationSchedule.class))).thenAnswer(i -> {
            EIRAmortizationSchedule sch = i.getArgument(0);
            sch.setId(System.currentTimeMillis());
            return sch;
        });

        when(loanRepository.save(any(Loan.class))).thenReturn(testLoan); // For loan updates within schedule generation/recalculation
        when(recalculationRunRepository.save(any(RecalculationRun.class))).thenAnswer(i -> {
            RecalculationRun rr = i.getArgument(0);
            rr.setId(30L);
            return rr;
        });


        RecalculationRun finalRun = loanService.reproduceDoDScenario(1L, "dod_tester");

        assertNotNull(finalRun);
        assertEquals(testLoan.getLoanNumber(), finalRun.getLoan().getLoanNumber());
        assertEquals(RecalculationRun.RecalculationReason.EARLY_REPAYMENT, finalRun.getReason());

        // Verify key interactions
        verify(loanService, times(1)).createDeferredItem(anyLong(), anyLong(), any(BigDecimal.class), any(LocalDate.class), any(LocalDate.class), anyString());
        verify(loanService, times(1)).generateAmortizationSchedule(anyLong(), any(LocalDate.class), any(BigDecimal.class), anyString());
        verify(loanEventRepository, times(1)).save(any(LoanEvent.class));
        verify(loanService, times(1)).recalculateLoan(anyLong(), any(LocalDate.class), any(RecalculationRun.RecalculationReason.class), anyString(), any(Optional.class), any(Optional.class));
    }
}
