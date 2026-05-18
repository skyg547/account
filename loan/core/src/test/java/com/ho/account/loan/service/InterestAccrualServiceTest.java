package com.ho.account.loan.service;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.loan.domain.LoanAccrualLog;
import com.ho.account.loan.domain.LoanAmortizationScheduleEntry;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.infrastructure.persistence.LoanAccrualLogRepository;
import com.ho.account.loan.infrastructure.persistence.LoanAmortizationScheduleEntryRepository;
import com.ho.account.loan.infrastructure.persistence.LoanRepository;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InterestAccrualServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanAmortizationScheduleEntryRepository amortizationRepository;

    @Mock
    private LoanAccrualLogRepository accrualLogRepository;

    @Mock
    private JournalUseCase journalUseCase;

    @Mock
    private AccountSubjectPersistencePort accountSubjectPersistencePort;

    private InterestAccrualService service;
    private LoanAccountingProperties accountingProperties;

    @BeforeEach
    void setUp() {
        accountingProperties = new LoanAccountingProperties();
        accountingProperties.setAccruedInterestReceivableAccountCode("11599");
        accountingProperties.setInterestIncomeAccountCode("41199");

        service = new InterestAccrualService(
                loanRepository,
                amortizationRepository,
                accrualLogRepository,
                journalUseCase,
                accountSubjectPersistencePort,
                accountingProperties
        );
    }

    @Test
    void processDailyAccrualCreatesAndPostsJournal() {
        LocalDate accrualDate = LocalDate.of(2026, 5, 12);
        Loan loan = new Loan();
        loan.setId(5L);
        loan.setLoanNumber("LC-001");
        loan.setCurrency(currency("KRW"));

        LoanAmortizationScheduleEntry scheduleEntry = new LoanAmortizationScheduleEntry();
        scheduleEntry.setInterestAmount(new BigDecimal("123.45"));

        JournalEntry savedJournal = new JournalEntry();
        savedJournal.setId(91L);
        savedJournal.setSlipNo("JE-ACCRUAL-91");
        JournalEntry postedJournal = new JournalEntry();
        postedJournal.setId(91L);
        postedJournal.setSlipNo("JE-ACCRUAL-91");
        postedJournal.setStatus(JournalEntryStatus.POSTED);

        when(loanRepository.findByStatus(Loan.LoanStatus.ACTIVE)).thenReturn(List.of(loan));
        when(accrualLogRepository.findByLoanIdAndAccrualDate(5L, accrualDate))
                .thenReturn(Optional.empty());
        when(amortizationRepository.findByLoanIdAndPaymentDate(5L, accrualDate))
                .thenReturn(Optional.of(scheduleEntry));
        when(accountSubjectPersistencePort.findByCode("11599")).thenReturn(Optional.of(account("11599")));
        when(accountSubjectPersistencePort.findByCode("41199")).thenReturn(Optional.of(account("41199")));
        when(journalUseCase.createJournalEntry(any(JournalEntry.class))).thenReturn(savedJournal);
        when(journalUseCase.getJournalEntry(91L)).thenReturn(Optional.of(postedJournal));
        when(accrualLogRepository.save(any(LoanAccrualLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processDailyAccrual(accrualDate);

        ArgumentCaptor<JournalEntry> journalCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journalUseCase).createJournalEntry(journalCaptor.capture());
        JournalEntry journal = journalCaptor.getValue();
        assertThat(journal.getStatus()).isEqualTo(JournalEntryStatus.DRAFT);
        assertThat(journal.getLineageSourceType()).isEqualTo("LOAN");
        assertThat(journal.getLineageSourceId()).isEqualTo("5");
        assertThat(journal.getCurrencyCode()).isEqualTo("KRW");
        assertThat(journal.getDetails()).hasSize(2);

        JournalDetail debit = journal.getDetails().get(0);
        JournalDetail credit = journal.getDetails().get(1);
        assertThat(debit.getSide()).isEqualTo(JournalSide.DEBIT);
        assertThat(debit.getAccountCode()).isEqualTo("11599");
        assertThat(debit.getAmount()).isEqualByComparingTo("123.45");
        assertThat(credit.getSide()).isEqualTo(JournalSide.CREDIT);
        assertThat(credit.getAccountCode()).isEqualTo("41199");
        assertThat(credit.getAmount()).isEqualByComparingTo("123.45");

        InOrder journalOrder = inOrder(journalUseCase);
        journalOrder.verify(journalUseCase).createJournalEntry(any(JournalEntry.class));
        journalOrder.verify(journalUseCase).approveJournalEntry(91L, "SYSTEM");
        journalOrder.verify(journalUseCase).postJournalEntry(91L, "SYSTEM");

        ArgumentCaptor<LoanAccrualLog> logCaptor = ArgumentCaptor.forClass(LoanAccrualLog.class);
        verify(accrualLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getJournalNo()).isEqualTo("JE-ACCRUAL-91");
        assertThat(logCaptor.getValue().getStatus()).isEqualTo("SUCCESS");
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
}
