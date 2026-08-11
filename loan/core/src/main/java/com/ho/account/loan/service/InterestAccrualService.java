package com.ho.account.loan.service;

import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.loan.application.port.out.LoanJournalPort.PostedJournal;
import com.ho.account.loan.application.port.out.LoanAccrualPersistencePort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort.AccountReference;
import com.ho.account.loan.domain.CurrencyRoundingPolicy;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanAccrualLog;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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

    private final LoanAccrualPersistencePort persistencePort;
    private final LoanJournalPort journalPort;
    private final LoanReferenceDataPort referenceDataPort;
    private final LoanAccountingProperties accountingProperties;

    public InterestAccrualService(
            LoanAccrualPersistencePort persistencePort,
            LoanJournalPort journalPort,
            LoanReferenceDataPort referenceDataPort,
            LoanAccountingProperties accountingProperties) {
        this.persistencePort = persistencePort;
        this.journalPort = journalPort;
        this.referenceDataPort = referenceDataPort;
        this.accountingProperties = accountingProperties;
    }

    public enum AccrualResult {
        SUCCESS,
        FAILED,
        ALREADY_SUCCESSFUL,
        NOT_DUE,
        INACTIVE
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public AccrualResult processIndividualAccrual(Long loanId, LocalDate accrualDate) {
        if (loanId == null || loanId < 1) {
            throw new IllegalArgumentException("loanId must be positive.");
        }
        if (accrualDate == null) {
            throw new IllegalArgumentException("accrualDate is required.");
        }

        Loan loan = persistencePort.findLoanForUpdate(loanId)
                .orElseThrow(() -> new IllegalStateException("Loan not found for accrual: " + loanId));
        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            return AccrualResult.INACTIVE;
        }

        var existingLog = persistencePort.findAccrualLog(loanId, accrualDate);
        if (existingLog.filter(log -> log.getStatus() == LoanAccrualLog.AccrualStatus.SUCCESS).isPresent()) {
            return AccrualResult.ALREADY_SUCCESSFUL;
        }

        var schedule = persistencePort.findSchedule(loanId, accrualDate);
        if (schedule.isEmpty() || schedule.get().getInterestIncome().signum() <= 0) {
            return AccrualResult.NOT_DUE;
        }

        BigDecimal interestAmount = schedule.get().getInterestIncome();
        LoanAccrualLog log = existingLog.orElseGet(
                () -> LoanAccrualLog.start(loan, accrualDate, interestAmount, SYSTEM_ACTOR));
        if (existingLog.isPresent()) {
            log.prepareRetry(interestAmount, SYSTEM_ACTOR);
        }

        try {
            PostedJournal postedJournal = postAccrualJournal(loan, interestAmount, accrualDate);
            log.markSuccess(postedJournal.journalEntryId(), postedJournal.slipNo());
            persistencePort.saveAccrualLog(log);
            return AccrualResult.SUCCESS;
        } catch (RuntimeException failure) {
            log.markFailed(failure);
            persistencePort.saveAccrualLog(log);
            return AccrualResult.FAILED;
        }
    }

    private PostedJournal postAccrualJournal(Loan loan, BigDecimal amount, LocalDate date) {
        AccountReference accruedInterestReceivable = referenceDataPort.requireAccount(
                accountingProperties.getAccruedInterestReceivableAccountCode(), date);
        AccountReference interestIncome = referenceDataPort.requireAccount(
                accountingProperties.getInterestIncomeAccountCode(), date);

        // [금융 회계 통화 정책 적용] 이자 발생 전표 금액에 통화 규격 절사/반올림 적용 (KRW: 0자리/절사, USD: 2자리/반올림 등)
        CurrencyRoundingPolicy roundingPolicy = CurrencyRoundingPolicy.of(loan.getCurrencyCode());
        BigDecimal roundedAmount = roundingPolicy.applyRounding(amount);

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
                                accruedInterestReceivable.code(),
                                roundedAmount,
                                "Accrued interest receivable"),
                        new LoanJournalPort.LoanJournalLine(
                                "CREDIT",
                                interestIncome.code(),
                                roundedAmount,
                                "Interest income accrual"))));
    }

    private String requireLoanId(Loan loan) {
        if (loan.getId() == null) {
            throw new IllegalStateException("Loan id is required for accrual journal lineage.");
        }
        return loan.getId().toString();
    }

    private String resolveLoanCurrencyCode(Loan loan) {
        String currencyCode = loan.getCurrencyCode();
        if (currencyCode == null || currencyCode.isBlank()) {
            throw new IllegalStateException("Loan currencyCode is required for journal posting.");
        }
        return currencyCode;
    }
}
