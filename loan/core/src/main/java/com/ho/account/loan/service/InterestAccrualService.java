package com.ho.account.loan.service;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.loan.domain.LoanAccrualLog;
import com.ho.account.loan.domain.LoanContract;
import com.ho.account.loan.infrastructure.persistence.LoanAccrualLogRepository;
import com.ho.account.loan.infrastructure.persistence.LoanAmortizationScheduleEntryRepository;
import com.ho.account.loan.infrastructure.persistence.LoanContractRepository;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily interest accrual service for active loan contracts.
 */
@Service
public class InterestAccrualService {

    private final LoanContractRepository loanContractRepository;
    private final LoanAmortizationScheduleEntryRepository amortizationRepository;
    private final LoanAccrualLogRepository accrualLogRepository;
    private final JournalUseCase journalUseCase;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final LoanAccountingProperties accountingProperties;

    public InterestAccrualService(
            LoanContractRepository loanContractRepository,
            LoanAmortizationScheduleEntryRepository amortizationRepository,
            LoanAccrualLogRepository accrualLogRepository,
            JournalUseCase journalUseCase,
            AccountSubjectPersistencePort accountSubjectPersistencePort,
            LoanAccountingProperties accountingProperties) {
        this.loanContractRepository = loanContractRepository;
        this.amortizationRepository = amortizationRepository;
        this.accrualLogRepository = accrualLogRepository;
        this.journalUseCase = journalUseCase;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.accountingProperties = accountingProperties;
    }

    @Transactional
    public void processDailyAccrual(LocalDate accrualDate) {
        List<LoanContract> activeLoans = loanContractRepository.findByStatus("ACTIVE");
        for (LoanContract loan : activeLoans) {
            processIndividualAccrual(loan, accrualDate);
        }
    }

    private void processIndividualAccrual(LoanContract loan, LocalDate accrualDate) {
        if (accrualLogRepository.findByLoanContractIdAndAccrualDate(loan.getId(), accrualDate).isPresent()) {
            return;
        }

        amortizationRepository.findByLoanContractIdAndPaymentDate(loan.getId(), accrualDate)
                .ifPresent(scheduleEntry -> {
                    BigDecimal interestAmount = scheduleEntry.getInterestAmount();

                    LoanAccrualLog log = new LoanAccrualLog();
                    log.setLoanContract(loan);
                    log.setAccrualDate(accrualDate);
                    log.setAccruedAmount(interestAmount);
                    log.setAuditUser("SYSTEM");

                    try {
                        JournalEntry journalEntry = createAccrualJournal(loan, interestAmount, accrualDate);
                        JournalEntry savedJournal = createAndPostJournal(journalEntry);
                        log.setJournalNo(savedJournal.getSlipNo());
                        log.setStatus("SUCCESS");
                    } catch (Exception e) {
                        log.setStatus("FAILED");
                        log.setErrorMessage(e.getMessage());
                    }

                    accrualLogRepository.save(log);
                });
    }

    private JournalEntry createAccrualJournal(LoanContract loan, BigDecimal amount, LocalDate date) {
        AccountSubject accruedInterestReceivable = resolveAccount(
                accountingProperties.getAccruedInterestReceivableAccountCode());
        AccountSubject interestIncome = resolveAccount(
                accountingProperties.getInterestIncomeAccountCode());

        JournalEntry entry = new JournalEntry();
        entry.setSlipNo(date + "-LOAN-ACCRUAL-" + System.currentTimeMillis());
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(date);
        entry.setDescription("Loan daily interest accrual: " + loan.getLoanContractNo());
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setEntryType("NORMAL");
        entry.setCreatedBy("SYSTEM");
        entry.setAuditUser("SYSTEM");
        entry.setLineageSourceType("LOAN");
        if (loan.getId() != null) {
            entry.setLineageSourceId(loan.getId().toString());
        }

        JournalDetail debit = new JournalDetail();
        debit.setSide(JournalSide.DEBIT);
        debit.setAmount(amount);
        debit.setBaseAmount(amount);
        debit.setDetailDescription("Accrued interest receivable");
        debit.setAccountCode(accruedInterestReceivable.getCode());

        JournalDetail credit = new JournalDetail();
        credit.setSide(JournalSide.CREDIT);
        credit.setAmount(amount);
        credit.setBaseAmount(amount);
        credit.setDetailDescription("Interest income accrual");
        credit.setAccountCode(interestIncome.getCode());

        entry.addDetail(debit);
        entry.addDetail(credit);

        return entry;
    }

    private JournalEntry createAndPostJournal(JournalEntry journalEntry) {
        JournalEntry savedEntry = journalUseCase.createJournalEntry(journalEntry);
        if (savedEntry == null || savedEntry.getId() == null) {
            throw new IllegalStateException("Loan accrual journal entry was not persisted with an id.");
        }
        journalUseCase.approveJournalEntry(savedEntry.getId(), "SYSTEM");
        journalUseCase.postJournalEntry(savedEntry.getId(), "SYSTEM");
        return journalUseCase.getJournalEntry(savedEntry.getId()).orElse(savedEntry);
    }

    private AccountSubject resolveAccount(String accountCode) {
        return accountSubjectPersistencePort.findByCode(accountCode)
                .orElseThrow(() -> new IllegalStateException("Account not found: " + accountCode));
    }
}
