package com.ho.account.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.loan.application.port.out.LoanAccrualPersistencePort;
import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort.AccountReference;
import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanAccrualLog;
import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Mock private LoanAccrualPersistencePort persistencePort;
    @Mock private LoanJournalPort journalPort;
    @Mock private LoanReferenceDataPort referenceDataPort;

    private InterestAccrualService service;

    @BeforeEach
    void setUp() {
        LoanAccountingProperties properties = new LoanAccountingProperties();
        properties.setAccruedInterestReceivableAccountCode("11599");
        properties.setInterestIncomeAccountCode("41199");
        service = new InterestAccrualService(
                persistencePort, journalPort, referenceDataPort, properties);
    }

    @Test
    void processIndividualAccrualPostsBalancedJournalAndMarksSuccess() {
        LocalDate accrualDate = LocalDate.of(2026, 5, 12);
        Loan loan = activeLoan();
        EIRAmortizationSchedule schedule = schedule(loan, accrualDate, "123.45");
        when(persistencePort.findLoanForUpdate(5L)).thenReturn(Optional.of(loan));
        when(persistencePort.findAccrualLog(5L, accrualDate)).thenReturn(Optional.empty());
        when(persistencePort.findSchedule(5L, accrualDate)).thenReturn(Optional.of(schedule));
        when(referenceDataPort.requireAccount("11599", accrualDate))
                .thenReturn(new AccountReference("11599", "Accrued interest receivable"));
        when(referenceDataPort.requireAccount("41199", accrualDate))
                .thenReturn(new AccountReference("41199", "Interest income"));
        when(journalPort.post(any())).thenReturn(new LoanJournalPort.PostedJournal(91L, "JE-ACCRUAL-91"));
        when(persistencePort.saveAccrualLog(any())).thenAnswer(invocation -> invocation.getArgument(0));

        InterestAccrualService.AccrualResult result =
                service.processIndividualAccrual(5L, accrualDate);

        assertThat(result).isEqualTo(InterestAccrualService.AccrualResult.SUCCESS);
        ArgumentCaptor<LoanJournalPort.LoanJournalCommand> commandCaptor =
                ArgumentCaptor.forClass(LoanJournalPort.LoanJournalCommand.class);
        verify(journalPort).post(commandCaptor.capture());
        LoanJournalPort.LoanJournalCommand command = commandCaptor.getValue();
        assertThat(command.accountingDate()).isEqualTo(accrualDate);
        assertThat(command.lineageSourceType()).isEqualTo("LOAN");
        assertThat(command.lineageSourceId()).isEqualTo("5");
        assertThat(command.currencyCode()).isEqualTo("KRW");
        assertThat(command.lines())
                .extracting(
                        LoanJournalPort.LoanJournalLine::side,
                        LoanJournalPort.LoanJournalLine::accountCode,
                        LoanJournalPort.LoanJournalLine::amount)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("DEBIT", "11599", new BigDecimal("123")),
                        org.assertj.core.groups.Tuple.tuple("CREDIT", "41199", new BigDecimal("123")));

        InOrder businessOrder = inOrder(referenceDataPort, journalPort);
        businessOrder.verify(referenceDataPort).requireAccount("11599", accrualDate);
        businessOrder.verify(referenceDataPort).requireAccount("41199", accrualDate);
        businessOrder.verify(journalPort).post(any());

        ArgumentCaptor<LoanAccrualLog> logCaptor = ArgumentCaptor.forClass(LoanAccrualLog.class);
        verify(persistencePort).saveAccrualLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(LoanAccrualLog.AccrualStatus.SUCCESS);
        assertThat(logCaptor.getValue().getJournalEntryId()).isEqualTo(91L);
        assertThat(logCaptor.getValue().getJournalNo()).isEqualTo("JE-ACCRUAL-91");
    }

    @Test
    void successfulAccrualIsIdempotentlySkipped() {
        LocalDate accrualDate = LocalDate.of(2026, 5, 12);
        Loan loan = activeLoan();
        LoanAccrualLog successful = LoanAccrualLog.start(
                loan, accrualDate, new BigDecimal("123.45"), "SYSTEM");
        successful.markSuccess(91L, "JE-ACCRUAL-91");
        when(persistencePort.findLoanForUpdate(5L)).thenReturn(Optional.of(loan));
        when(persistencePort.findAccrualLog(5L, accrualDate)).thenReturn(Optional.of(successful));

        assertThat(service.processIndividualAccrual(5L, accrualDate))
                .isEqualTo(InterestAccrualService.AccrualResult.ALREADY_SUCCESSFUL);

        verify(persistencePort, never()).findSchedule(any(), any());
        verify(journalPort, never()).post(any());
    }

    @Test
    void failedAccrualPersistsRetryableFailureInsteadOfReportingSuccess() {
        LocalDate accrualDate = LocalDate.of(2026, 5, 12);
        Loan loan = activeLoan();
        LoanAccrualLog failed = LoanAccrualLog.start(
                loan, accrualDate, new BigDecimal("100.00"), "SYSTEM");
        failed.markFailed(new IllegalStateException("previous failure"));
        when(persistencePort.findLoanForUpdate(5L)).thenReturn(Optional.of(loan));
        when(persistencePort.findAccrualLog(5L, accrualDate)).thenReturn(Optional.of(failed));
        when(persistencePort.findSchedule(5L, accrualDate))
                .thenReturn(Optional.of(schedule(loan, accrualDate, "123.45")));
        when(referenceDataPort.requireAccount("11599", accrualDate))
                .thenReturn(new AccountReference("11599", "Accrued interest receivable"));
        when(referenceDataPort.requireAccount("41199", accrualDate))
                .thenReturn(new AccountReference("41199", "Interest income"));
        when(journalPort.post(any())).thenThrow(new IllegalStateException("journal unavailable"));
        when(persistencePort.saveAccrualLog(any())).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.processIndividualAccrual(5L, accrualDate))
                .isEqualTo(InterestAccrualService.AccrualResult.FAILED);

        ArgumentCaptor<LoanAccrualLog> logCaptor = ArgumentCaptor.forClass(LoanAccrualLog.class);
        verify(persistencePort).saveAccrualLog(logCaptor.capture());
        assertThat(logCaptor.getValue().getStatus()).isEqualTo(LoanAccrualLog.AccrualStatus.FAILED);
        assertThat(logCaptor.getValue().getErrorMessage()).contains("journal unavailable");
    }

    private Loan activeLoan() {
        Loan loan = Loan.create(
                "LC-001",
                100L,
                "KRW",
                Loan.LoanType.TERM_LOAN,
                new BigDecimal("5000000.00"),
                new BigDecimal("0.0450"),
                LocalDate.of(2026, 5, 10),
                LocalDate.of(2027, 5, 10),
                Loan.PaymentFrequency.MONTHLY,
                "loan-user");
        loan.setId(5L);
        loan.activateAfterDisbursal(
                loan.getDisbursalDate(), loan.getPrincipalAmount(), "loan-user");
        return loan;
    }

    private EIRAmortizationSchedule schedule(
            Loan loan, LocalDate scheduleDate, String interestAmount) {
        EIRAmortizationSchedule schedule = new EIRAmortizationSchedule();
        schedule.setLoan(loan);
        schedule.setScheduleDate(scheduleDate);
        schedule.setInterestIncome(new BigDecimal(interestAmount));
        return schedule;
    }
}
