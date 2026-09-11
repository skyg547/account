package com.ho.account.loan.service;

import com.ho.account.loan.application.port.out.LoanAccrualPersistencePort;
import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.application.port.out.LoanRepaymentPersistencePort;
import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanAccrualLog;
import com.ho.account.loan.domain.LoanEvent;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Reserve durably before remote posting, then atomically apply payment and journal lineage.
 * A pending reservation is never retried automatically, including timeouts and process crashes.
 * Recovery must reconcile the remote journal and complete or cancel the reservation explicitly.
 */
@Service
public class ScheduledRepaymentService {
    private static final String ACTOR = "SYSTEM";
    private static final String LINEAGE = "LOAN_SCHEDULED_REPAYMENT";
    private final LoanAccrualPersistencePort accrualPort;
    private final LoanRepaymentPersistencePort repaymentPort;
    private final LoanJournalPort journalPort;
    private final LoanReferenceDataPort referencePort;
    private final LoanAccountingProperties properties;
    private final TransactionTemplate transaction;

    public ScheduledRepaymentService(LoanAccrualPersistencePort accrualPort,
            LoanRepaymentPersistencePort repaymentPort, LoanJournalPort journalPort,
            LoanReferenceDataPort referencePort, LoanAccountingProperties properties,
            PlatformTransactionManager transactionManager) {
        this.accrualPort = accrualPort;
        this.repaymentPort = repaymentPort;
        this.journalPort = journalPort;
        this.referencePort = referencePort;
        this.properties = properties;
        this.transaction = new TransactionTemplate(transactionManager);
        this.transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public enum RepaymentResult { SUCCESS, ALREADY_SUCCESSFUL, NOT_DUE }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public RepaymentResult processIndividualRepayment(Long loanId, LocalDate repaymentDate) {
        if (loanId == null || loanId < 1 || repaymentDate == null) {
            throw new IllegalArgumentException("Positive loanId and repaymentDate are required.");
        }
        Reservation reservation = Objects.requireNonNull(
                transaction.execute(status -> reserve(loanId, repaymentDate)));
        if (reservation.result() != null) {
            return reservation.result();
        }
        // Do not catch and erase the durable reservation: the remote result may be ambiguous.
        LoanJournalPort.PostedJournal journal = journalPort.post(reservation.command());
        transaction.executeWithoutResult(status -> complete(loanId, repaymentDate, reservation, journal));
        return RepaymentResult.SUCCESS;
    }

    private Reservation reserve(Long loanId, LocalDate date) {
        Loan loan = lockedLoan(loanId);
        var previous = repaymentPort.findRepayment(loanId, date);
        if (previous.isPresent()) {
            if (previous.get().getEventType() == LoanEvent.EventType.SCHEDULED_REPAYMENT
                    && previous.get().getRelatedJournalEntryId() != null
                    && previous.get().getRelatedJournalEntryId() > 0
                    && previous.get().getRelatedJournalEntrySlipNo() != null
                    && !previous.get().getRelatedJournalEntrySlipNo().isBlank()) {
                return new Reservation(RepaymentResult.ALREADY_SUCCESSFUL, null, null, null);
            }
            throw pending();
        }
        if (repaymentPort.hasPendingRepayment(loanId)) {
            throw pending();
        }
        var schedule = accrualPort.findSchedule(loanId, date);
        if (schedule.isEmpty()) {
            return new Reservation(RepaymentResult.NOT_DUE, null, null, null);
        }
        EIRAmortizationSchedule due = schedule.get();
        loan.validateScheduledRepayment(due);
        requireAccrual(loanId, date, due.getInterestIncome());
        LoanJournalPort.LoanJournalCommand command = command(loan, due);
        LoanEvent event = LoanEvent.record(loan, LoanEvent.EventType.SCHEDULED_REPAYMENT_PENDING,
                date, "Scheduled repayment principal=" + due.getPrincipalRepayment().toPlainString()
                        + ", interest=" + due.getInterestIncome().toPlainString()
                        + ", ending=" + due.getEndingBalance().toPlainString(), null, ACTOR);
        LoanEvent saved = repaymentPort.saveEvent(event);
        return new Reservation(null, saved.getId(), command, snapshot(due));
    }

    private void complete(Long loanId, LocalDate date, Reservation reserved,
            LoanJournalPort.PostedJournal journal) {
        Loan loan = lockedLoan(loanId);
        LoanEvent event = repaymentPort.findRepayment(loanId, date).orElseThrow(ScheduledRepaymentService::pending);
        if (!Objects.equals(event.getId(), reserved.eventId())
                || event.getEventType() != LoanEvent.EventType.SCHEDULED_REPAYMENT_PENDING) {
            throw pending();
        }
        EIRAmortizationSchedule schedule = accrualPort.findSchedule(loanId, date)
                .orElseThrow(ScheduledRepaymentService::pending);
        if (!snapshot(schedule).equals(reserved.scheduleSnapshot())) {
            throw new IllegalStateException("Reserved repayment schedule changed; reconcile posted journal.");
        }
        loan.applyScheduledRepayment(schedule, ACTOR);
        event.completeScheduledRepayment(journal.journalEntryId(), journal.slipNo());
        repaymentPort.saveLoan(loan);
        repaymentPort.saveEvent(event);
    }

    private Loan lockedLoan(Long loanId) {
        return accrualPort.findLoanForUpdate(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found: " + loanId));
    }

    private void requireAccrual(Long loanId, LocalDate date, BigDecimal interest) {
        if (interest.signum() == 0) {
            return;
        }
        LoanAccrualLog accrued = accrualPort.findAccrualLog(loanId, date)
                .orElseThrow(() -> new IllegalStateException("Successful interest accrual is required before repayment."));
        if (accrued.getStatus() != LoanAccrualLog.AccrualStatus.SUCCESS
                || accrued.getJournalEntryId() == null || accrued.getJournalEntryId() < 1
                || accrued.getJournalNo() == null || accrued.getJournalNo().isBlank()
                || accrued.getAccruedAmount() == null || accrued.getAccruedAmount().compareTo(interest) != 0) {
            throw new IllegalStateException("Repayment interest must match a successfully posted accrual.");
        }
    }

    private LoanJournalPort.LoanJournalCommand command(Loan loan, EIRAmortizationSchedule schedule) {
        LocalDate date = schedule.getScheduleDate();
        String cash = referencePort.requireAccount(properties.getCashAccountCode(), date).code();
        String principal = referencePort.requireAccount(properties.getLoanReceivableAccountCode(), date).code();
        var lines = new ArrayList<LoanJournalPort.LoanJournalLine>();
        lines.add(new LoanJournalPort.LoanJournalLine("DEBIT", cash, schedule.getCashFlow(), "Scheduled payment received"));
        lines.add(new LoanJournalPort.LoanJournalLine("CREDIT", principal,
                schedule.getPrincipalRepayment(), "Scheduled principal repayment"));
        if (schedule.getInterestIncome().signum() > 0) {
            String interest = referencePort.requireAccount(properties.getAccruedInterestReceivableAccountCode(), date).code();
            lines.add(new LoanJournalPort.LoanJournalLine("CREDIT", interest,
                    schedule.getInterestIncome(), "Settlement of accrued interest"));
        }
        return new LoanJournalPort.LoanJournalCommand(date, "Scheduled loan repayment: " + loan.getLoanNumber(),
                ACTOR, LINEAGE, loan.getId() + ":" + date, loan.getCurrencyCode(), lines);
    }

    private static String snapshot(EIRAmortizationSchedule schedule) {
        return schedule.getBeginningBalance().stripTrailingZeros().toPlainString() + "/"
                + schedule.getPrincipalRepayment().stripTrailingZeros().toPlainString() + "/"
                + schedule.getInterestIncome().stripTrailingZeros().toPlainString() + "/"
                + schedule.getEndingBalance().stripTrailingZeros().toPlainString() + "/"
                + schedule.getCashFlow().stripTrailingZeros().toPlainString();
    }

    private static IllegalStateException pending() {
        return new IllegalStateException("Pending scheduled repayment requires manual journal reconciliation before retry.");
    }

    private record Reservation(RepaymentResult result, Long eventId,
            LoanJournalPort.LoanJournalCommand command, String scheduleSnapshot) { }
}
