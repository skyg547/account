package com.ho.account.loan.service;

import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.loan.application.port.out.LoanJournalPort.PostedJournal;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanAccrualLog;
import com.ho.account.loan.infrastructure.persistence.LoanAccrualLogRepository;
import com.ho.account.loan.infrastructure.persistence.LoanAmortizationScheduleEntryRepository;
import com.ho.account.loan.infrastructure.persistence.LoanRepository;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Daily interest accrual service for active loan contracts.
 *
 * <p>이 서비스는 일일 이자 발생이라는 업무 유즈케이스의 순서를 책임집니다.
 * 스케줄 조회, 중복 로그 확인, 계정 검증, 전표 요청, 처리 로그 저장을 한 트랜잭션 흐름으로 묶되,
 * journal-ledger의 엔티티 생성/승인/전기 방식은 {@link LoanJournalPort} 출력 포트 뒤로 숨깁니다.
 */
@Service
public class InterestAccrualService {

    private static final String SYSTEM_ACTOR = "SYSTEM";
    private static final String LINEAGE_SOURCE_TYPE = "LOAN";

    private final LoanRepository loanRepository;
    private final LoanAmortizationScheduleEntryRepository amortizationRepository;
    private final LoanAccrualLogRepository accrualLogRepository;
    private final LoanJournalPort journalPort;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final LoanAccountingProperties accountingProperties;

    public InterestAccrualService(
            LoanRepository loanRepository,
            LoanAmortizationScheduleEntryRepository amortizationRepository,
            LoanAccrualLogRepository accrualLogRepository,
            LoanJournalPort journalPort,
            AccountSubjectPersistencePort accountSubjectPersistencePort,
            LoanAccountingProperties accountingProperties) {
        this.loanRepository = loanRepository;
        this.amortizationRepository = amortizationRepository;
        this.accrualLogRepository = accrualLogRepository;
        this.journalPort = journalPort;
        this.accountSubjectPersistencePort = accountSubjectPersistencePort;
        this.accountingProperties = accountingProperties;
    }

    @Transactional
    public void processDailyAccrual(LocalDate accrualDate) {
        List<Loan> activeLoans = loanRepository.findByStatus(Loan.LoanStatus.ACTIVE);
        for (Loan loan : activeLoans) {
            processIndividualAccrual(loan, accrualDate);
        }
    }

    public void processIndividualAccrual(Loan loan, LocalDate accrualDate) {
        if (accrualLogRepository.findByLoanIdAndAccrualDate(loan.getId(), accrualDate).isPresent()) {
            return;
        }

        amortizationRepository.findByLoanIdAndPaymentDate(loan.getId(), accrualDate)
                .ifPresent(scheduleEntry -> {
                    BigDecimal interestAmount = scheduleEntry.getInterestAmount();

                    LoanAccrualLog log = new LoanAccrualLog();
                    log.setLoan(loan);
                    log.setAccrualDate(accrualDate);
                    log.setAccruedAmount(interestAmount);
                    log.setAuditUser(SYSTEM_ACTOR);

                    try {
                        PostedJournal postedJournal = postAccrualJournal(loan, interestAmount, accrualDate);
                        log.setJournalEntryId(postedJournal.journalEntryId());
                        log.setJournalNo(postedJournal.slipNo());
                        log.setStatus("SUCCESS");
                    } catch (Exception e) {
                        log.setStatus("FAILED");
                        log.setErrorMessage(e.getMessage());
                    }

                    accrualLogRepository.save(log);
                });
    }

    private PostedJournal postAccrualJournal(Loan loan, BigDecimal amount, LocalDate date) {
        AccountSubject accruedInterestReceivable = resolveAccount(
                accountingProperties.getAccruedInterestReceivableAccountCode());
        AccountSubject interestIncome = resolveAccount(
                accountingProperties.getInterestIncomeAccountCode());

        return journalPort.post(new LoanJournalPort.LoanJournalCommand(
                date,
                "Loan daily interest accrual: " + loan.getLoanNumber(),
                SYSTEM_ACTOR,
                LINEAGE_SOURCE_TYPE,
                requireLoanId(loan),
                resolveLoanCurrencyCode(loan),
                List.of(
                        new LoanJournalPort.LoanJournalLine(
                                "DEBIT",
                                accruedInterestReceivable.getCode(),
                                amount,
                                "Accrued interest receivable"),
                        new LoanJournalPort.LoanJournalLine(
                                "CREDIT",
                                interestIncome.getCode(),
                                amount,
                                "Interest income accrual"))));
    }

    private String requireLoanId(Loan loan) {
        if (loan.getId() == null) {
            throw new IllegalStateException("Loan id is required for accrual journal lineage.");
        }
        return loan.getId().toString();
    }

    private String resolveLoanCurrencyCode(Loan loan) {
        return Optional.ofNullable(loan.getCurrency())
                .map(currency -> currency.getCurrencyCode())
                .filter(code -> !code.isBlank())
                .orElseThrow(() -> new IllegalStateException("Loan currencyCode is required for journal posting."));
    }

    private AccountSubject resolveAccount(String accountCode) {
        return accountSubjectPersistencePort.findByCode(accountCode)
                .orElseThrow(() -> new IllegalStateException("Account not found: " + accountCode));
    }
}