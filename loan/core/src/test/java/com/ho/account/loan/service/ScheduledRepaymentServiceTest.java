package com.ho.account.loan.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.ho.account.loan.application.port.out.*;
import com.ho.account.loan.domain.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class ScheduledRepaymentServiceTest {
    private static final LocalDate DATE = LocalDate.of(2090, 1, 15);
    @Mock LoanAccrualPersistencePort accrual;
    @Mock LoanRepaymentPersistencePort repayment;
    @Mock LoanJournalPort journal;
    @Mock LoanReferenceDataPort reference;
    @Mock PlatformTransactionManager transactions;
    ScheduledRepaymentService service;
    Loan loan;
    EIRAmortizationSchedule schedule;
    AtomicReference<LoanEvent> event;

    @BeforeEach
    void setup() {
        var properties = new LoanAccountingProperties();
        properties.setCashAccountCode("cash");
        properties.setLoanReceivableAccountCode("principal");
        properties.setAccruedInterestReceivableAccountCode("accrued");
        service = new ScheduledRepaymentService(accrual, repayment, journal, reference, properties, transactions);
        when(transactions.getTransaction(any())).thenAnswer(invocation -> {
            assertThat(((TransactionDefinition) invocation.getArgument(0)).getPropagationBehavior())
                    .isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return new SimpleTransactionStatus();
        });
        loan = Loan.create("LOAN-TEST", 1L, "KRW", Loan.LoanType.TERM_LOAN,
                new BigDecimal("12000000"), new BigDecimal("0.06"), DATE.minusMonths(1),
                DATE.plusMonths(11), Loan.PaymentFrequency.MONTHLY, "SYSTEM");
        loan.setId(9L);
        loan.activateAfterDisbursal(loan.getDisbursalDate(), loan.getPrincipalAmount(), "SYSTEM");
        schedule = new EIRAmortizationSchedule();
        schedule.setLoan(loan);
        schedule.setScheduleDate(DATE);
        schedule.setBeginningBalance(new BigDecimal("12000000"));
        schedule.setPrincipalRepayment(new BigDecimal("1000000"));
        schedule.setInterestIncome(new BigDecimal("60000"));
        schedule.setCashFlow(new BigDecimal("1060000"));
        schedule.setEndingBalance(new BigDecimal("11000000"));
        event = new AtomicReference<>();
        when(accrual.findLoanForUpdate(9L)).thenReturn(Optional.of(loan));
        when(repayment.findRepayment(9L, DATE)).thenAnswer(invocation -> Optional.ofNullable(event.get()));
    }

    private void due() {
        when(accrual.findSchedule(9L, DATE)).thenReturn(Optional.of(schedule));
    }

    private void accrued() {
        LoanAccrualLog log = LoanAccrualLog.start(loan, DATE, schedule.getInterestIncome(), "SYSTEM");
        log.markSuccess(20L, "ACCRUAL-20");
        when(accrual.findAccrualLog(9L, DATE)).thenReturn(Optional.of(log));
    }

    private void posting() {
        when(reference.requireAccount(any(), any())).thenAnswer(invocation ->
                new LoanReferenceDataPort.AccountReference(invocation.getArgument(0), "Account"));
        when(repayment.saveEvent(any())).thenAnswer(invocation -> {
            LoanEvent saved = invocation.getArgument(0);
            saved.setId(30L);
            event.set(saved);
            return saved;
        });
    }

    @Test
    void commitsReservationBeforePostingAndSettlesAccruedInterestWithoutChangingEir() {
        due(); accrued(); posting();
        when(journal.post(any())).thenAnswer(invocation -> {
            verify(transactions).commit(any());
            assertThat(event.get().getEventType()).isEqualTo(LoanEvent.EventType.SCHEDULED_REPAYMENT_PENDING);
            assertThat(loan.getOutstandingPrincipal()).isEqualByComparingTo("12000000");
            return new LoanJournalPort.PostedJournal(40L, "REPAYMENT-40");
        });
        assertThat(service.processIndividualRepayment(9L, DATE))
                .isEqualTo(ScheduledRepaymentService.RepaymentResult.SUCCESS);
        assertThat(loan.getOutstandingPrincipal()).isEqualByComparingTo("11000000");
        assertThat(loan.getTotalPrincipalPaid()).isEqualByComparingTo("1000000");
        assertThat(loan.getTotalInterestPaid()).isEqualByComparingTo("60000");
        assertThat(loan.getCurrentEIR()).isEqualByComparingTo("0.06");
        assertThat(loan.getMaturityDate()).isEqualTo(DATE.plusMonths(11));
        assertThat(event.get().getEventType()).isEqualTo(LoanEvent.EventType.SCHEDULED_REPAYMENT);
        assertThat(event.get().getRelatedJournalEntryId()).isEqualTo(40L);
        var command = ArgumentCaptor.forClass(LoanJournalPort.LoanJournalCommand.class);
        verify(journal).post(command.capture());
        assertThat(command.getValue().lineageSourceType()).isEqualTo("LOAN_SCHEDULED_REPAYMENT");
        assertThat(command.getValue().lineageSourceId()).isEqualTo("9:2090-01-15");
        assertThat(command.getValue().lines()).extracting(LoanJournalPort.LoanJournalLine::accountCode)
                .containsExactly("cash", "principal", "accrued");
        assertThat(command.getValue().lines()).extracting(LoanJournalPort.LoanJournalLine::amount)
                .containsExactly(new BigDecimal("1060000"), new BigDecimal("1000000"), new BigDecimal("60000"));
        verify(transactions, times(2)).commit(any());
        assertThat(service.processIndividualRepayment(9L, DATE))
                .isEqualTo(ScheduledRepaymentService.RepaymentResult.ALREADY_SUCCESSFUL);
        verify(journal, times(1)).post(any());
        assertThat(loan.getTotalPrincipalPaid()).isEqualByComparingTo("1000000");
    }

    @Test
    void finalPaymentClosesLoanAndRepeatSkipsEvenThoughLoanIsRepaid() {
        schedule.setPrincipalRepayment(new BigDecimal("12000000"));
        schedule.setCashFlow(new BigDecimal("12060000"));
        schedule.setEndingBalance(BigDecimal.ZERO);
        due(); accrued(); posting();
        when(journal.post(any())).thenReturn(new LoanJournalPort.PostedJournal(40L, "REPAYMENT-40"));
        service.processIndividualRepayment(9L, DATE);
        assertThat(loan.getStatus()).isEqualTo(Loan.LoanStatus.REPAID);
        assertThat(loan.getOutstandingPrincipal()).isZero();
        assertThat(service.processIndividualRepayment(9L, DATE))
                .isEqualTo(ScheduledRepaymentService.RepaymentResult.ALREADY_SUCCESSFUL);
        verify(journal, times(1)).post(any());
    }

    @Test
    void noScheduleIsNotDueAndDoesNotPost() {
        assertThat(service.processIndividualRepayment(9L, DATE))
                .isEqualTo(ScheduledRepaymentService.RepaymentResult.NOT_DUE);
        verifyNoInteractions(journal, reference);
        verify(repayment, never()).saveEvent(any());
    }

    @Test
    void invalidScheduleFailsBeforeReservationOrPosting() {
        due();
        schedule.setEndingBalance(new BigDecimal("11000001"));
        assertThatThrownBy(() -> service.processIndividualRepayment(9L, DATE))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("reconcile");
        verify(repayment, never()).saveEvent(any());
        verifyNoInteractions(journal, reference);
        verify(transactions).rollback(any());
    }

    @Test
    void missingAccrualRejectsPaymentBeforeReservation() {
        due();
        assertThatThrownBy(() -> service.processIndividualRepayment(9L, DATE))
                .hasMessageContaining("Successful interest accrual");
        verify(repayment, never()).saveEvent(any());
        verifyNoInteractions(journal, reference);
    }

    @Test
    void pendingReservationBlocksRemoteRetry() {
        event.set(LoanEvent.record(loan, LoanEvent.EventType.SCHEDULED_REPAYMENT_PENDING,
                DATE, "Pending", null, "SYSTEM"));
        assertThatThrownBy(() -> service.processIndividualRepayment(9L, DATE))
                .hasMessageContaining("manual journal reconciliation");
        verifyNoInteractions(journal, reference);
    }

    @Test
    void pendingDifferentDateBlocksAnotherPayment() {
        when(repayment.hasPendingRepayment(9L)).thenReturn(true);
        assertThatThrownBy(() -> service.processIndividualRepayment(9L, DATE))
                .hasMessageContaining("manual journal reconciliation");
        verifyNoInteractions(journal, reference);
    }

    @Test
    void remoteFailureLeavesCommittedReservationAndUnchangedBalanceAndBlocksRetry() {
        due(); accrued(); posting();
        when(journal.post(any())).thenThrow(new IllegalStateException("Ambiguous remote outcome"));
        assertThatThrownBy(() -> service.processIndividualRepayment(9L, DATE))
                .hasMessageContaining("Ambiguous remote outcome");
        assertThat(event.get().getEventType()).isEqualTo(LoanEvent.EventType.SCHEDULED_REPAYMENT_PENDING);
        assertThat(loan.getOutstandingPrincipal()).isEqualByComparingTo("12000000");
        verify(transactions).commit(any());
        verify(repayment, never()).saveLoan(any());
        assertThatThrownBy(() -> service.processIndividualRepayment(9L, DATE))
                .hasMessageContaining("manual journal reconciliation");
        verify(journal, times(1)).post(any());
    }

    @Test
    void changedScheduleAfterRemotePostingFailsClosed() {
        due(); accrued(); posting();
        when(journal.post(any())).thenAnswer(invocation -> {
            schedule.setInterestIncome(new BigDecimal("70000"));
            schedule.setCashFlow(new BigDecimal("1070000"));
            return new LoanJournalPort.PostedJournal(40L, "REPAYMENT-40");
        });
        assertThatThrownBy(() -> service.processIndividualRepayment(9L, DATE))
                .hasMessageContaining("schedule changed");
        assertThat(event.get().getEventType()).isEqualTo(LoanEvent.EventType.SCHEDULED_REPAYMENT_PENDING);
        assertThat(loan.getOutstandingPrincipal()).isEqualByComparingTo("12000000");
        verify(transactions).rollback(any());
    }
}
