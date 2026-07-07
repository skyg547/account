package com.ho.account.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanAccrualLog;
import com.ho.account.loan.domain.LoanAmortizationScheduleEntry;
import com.ho.account.loan.infrastructure.persistence.LoanAccrualLogRepository;
import com.ho.account.loan.infrastructure.persistence.LoanAmortizationScheduleEntryRepository;
import com.ho.account.loan.infrastructure.persistence.LoanRepository;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.Currency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InterestAccrualServiceTest {

    @Mock
    private LoanRepository loanRepository;

    @Mock
    private LoanAmortizationScheduleEntryRepository amortizationRepository;

    @Mock
    private LoanAccrualLogRepository accrualLogRepository;

    @Mock
    private LoanJournalPort journalPort;

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
                journalPort,
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

        when(loanRepository.findByStatus(Loan.LoanStatus.ACTIVE)).thenReturn(List.of(loan));
        when(accrualLogRepository.findByLoanIdAndAccrualDate(5L, accrualDate))
                .thenReturn(Optional.empty());
        when(amortizationRepository.findByLoanIdAndPaymentDate(5L, accrualDate))
                .thenReturn(Optional.of(scheduleEntry));
        when(accountSubjectPersistencePort.findByCode("11599")).thenReturn(Optional.of(account("11599")));
        when(accountSubjectPersistencePort.findByCode("41199")).thenReturn(Optional.of(account("41199")));
        when(journalPort.post(any(LoanJournalPort.LoanJournalCommand.class)))
                .thenReturn(new LoanJournalPort.PostedJournal(91L, "JE-ACCRUAL-91"));
        when(accrualLogRepository.save(any(LoanAccrualLog.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processDailyAccrual(accrualDate);

        ArgumentCaptor<LoanJournalPort.LoanJournalCommand> commandCaptor =
                ArgumentCaptor.forClass(LoanJournalPort.LoanJournalCommand.class);
        verify(journalPort).post(commandCaptor.capture());
        LoanJournalPort.LoanJournalCommand command = commandCaptor.getValue();
        assertThat(command.accountingDate()).isEqualTo(accrualDate);
        assertThat(command.lineageSourceType()).isEqualTo("LOAN");
        assertThat(command.lineageSourceId()).isEqualTo("5");
        assertThat(command.currencyCode()).isEqualTo("KRW");
        assertThat(command.lines()).hasSize(2);

        LoanJournalPort.LoanJournalLine debit = command.lines().get(0);
        LoanJournalPort.LoanJournalLine credit = command.lines().get(1);
        assertThat(debit.side()).isEqualTo("DEBIT");
        assertThat(debit.accountCode()).isEqualTo("11599");
        assertThat(debit.amount()).isEqualByComparingTo("123.45");
        assertThat(credit.side()).isEqualTo("CREDIT");
        assertThat(credit.accountCode()).isEqualTo("41199");
        assertThat(credit.amount()).isEqualByComparingTo("123.45");

        InOrder businessOrder = inOrder(accountSubjectPersistencePort, journalPort);
        businessOrder.verify(accountSubjectPersistencePort).findByCode("11599");
        businessOrder.verify(accountSubjectPersistencePort).findByCode("41199");
        businessOrder.verify(journalPort).post(any(LoanJournalPort.LoanJournalCommand.class));

        ArgumentCaptor<LoanAccrualLog> logCaptor = ArgumentCaptor.forClass(LoanAccrualLog.class);
        verify(accrualLogRepository).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getJournalEntryId()).isEqualTo(91L);
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