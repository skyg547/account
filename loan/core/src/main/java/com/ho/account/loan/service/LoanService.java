package com.ho.account.loan.service;

import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
import com.ho.account.journalledger.application.port.out.JournalPersistencePort;
import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.loan.domain.*;
import com.ho.account.loan.domain.DeferredItemType.DeferralMethod;
import com.ho.account.loan.domain.Loan.LoanStatus;
import com.ho.account.loan.domain.LoanEvent.EventType;
import com.ho.account.loan.domain.RecalculationRun.RecalculationReason;
import com.ho.account.loan.repository.*;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 대출 회계 (Loan Accounting) 서비스.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanDisbursalRepository loanDisbursalRepository;
    private final LoanEventRepository loanEventRepository;
    private final DeferredItemTypeRepository deferredItemTypeRepository;
    private final DeferredItemRepository deferredItemRepository;
    private final EIRAmortizationScheduleRepository eirAmortizationScheduleRepository;
    private final RecalculationRunRepository recalculationRunRepository;
    private final EIRCalculator eirCalculator;

    // MSA 포트로 교체
    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final CurrencyPersistencePort currencyPersistencePort;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final JournalPersistencePort journalPersistencePort;
    private final JournalUseCase journalUseCase;

    public Loan createLoan(Loan loan) {
        BusinessPartner bp = businessPartnerPersistencePort.findById(loan.getBusinessPartner().getId())
                .orElseThrow(() -> new EntityNotFoundException("BusinessPartner not found"));
        Currency currency = currencyPersistencePort.findByCode(loan.getCurrency().getCurrencyCode())
                .orElseThrow(() -> new EntityNotFoundException("Currency not found"));
        loan.setBusinessPartner(bp);
        loan.setCurrency(currency);

        loan.setInitialEIR(loan.getInterestRate());
        loan.setCurrentEIR(loan.getInitialEIR());
        loan.setStatus(LoanStatus.ACTIVE);
        return loanRepository.save(loan);
    }

    @Transactional(readOnly = true)
    public Loan findLoanById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found with id: " + id));
    }

    public LoanDisbursal disburseLoan(Long loanId, LocalDate disbursalDate, BigDecimal disbursedAmount, String user) {
        Loan loan = findLoanById(loanId);

        LoanDisbursal disbursal = new LoanDisbursal();
        disbursal.setLoan(loan);
        disbursal.setDisbursalDate(disbursalDate);
        disbursal.setDisbursedAmount(disbursedAmount);
        disbursal.setAuditUser(user);

        AccountSubject cashAccount = accountSubjectPersistencePort.findByCode("101000")
                .orElseThrow(() -> new EntityNotFoundException("Cash Account not found."));
        AccountSubject loanReceivableAccount = accountSubjectPersistencePort.findByCode("131000")
                .orElseThrow(() -> new EntityNotFoundException("Loan Receivable Account not found."));

        JournalEntry disbursalJe = createAutomatedJournalEntry(
                disbursalDate,
                loan.getLoanNumber() + " 대출 실행",
                user,
                "LOAN_DISBURSAL",
                loanId.toString(),
                disbursedAmount,
                cashAccount,
                loanReceivableAccount
        );
        disbursal.setJournalEntry(disbursalJe);

        return loanDisbursalRepository.save(disbursal);
    }

    private JournalEntry createAutomatedJournalEntry(LocalDate accountingDate, String description, String createdBy,
                                                      String lineageSourceType, String lineageSourceId,
                                                      BigDecimal amount, AccountSubject creditAccount, AccountSubject debitAccount) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(accountingDate);
        entry.setDescription(description);
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setEntryType("NORMAL");
        entry.setCreatedBy(createdBy);
        entry.setAuditUser(createdBy);
        entry.setLineageSourceType(lineageSourceType);
        entry.setLineageSourceId(lineageSourceId);

        JournalDetail debitDetail = new JournalDetail();
        debitDetail.setSide(JournalSide.DEBIT);
        debitDetail.setAccountSubject(debitAccount);
        debitDetail.setAmount(amount);
        debitDetail.setBaseAmount(amount);
        debitDetail.setDetailDescription(description + " (차변)");
        entry.addDetail(debitDetail);

        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setSide(JournalSide.CREDIT);
        creditDetail.setAccountSubject(creditAccount);
        creditDetail.setAmount(amount);
        creditDetail.setBaseAmount(amount);
        creditDetail.setDetailDescription(description + " (대변)");
        entry.addDetail(creditDetail);

        entry.setSlipNo(accountingDate.toString() + "-LOAN-" + System.currentTimeMillis());
        return journalPersistencePort.save(entry);
    }

    // (기타 EIR 계산 및 스케줄 생성 로직들은 리포지토리 참조를 유지하거나 필요시 포트로 교체)
}
