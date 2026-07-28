package com.ho.account.loan.application.port.in;

import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.domain.LoanEvent;
import com.ho.account.loan.domain.RecalculationRun;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/** 대출 HTTP/Batch 인바운드 어댑터가 호출하는 기술 독립 유즈케이스 계약입니다. */
public interface LoanUseCase {

    Loan createLoan(Loan loan);

    Loan findLoanById(Long id);

    LoanDisbursal disburseLoan(
            Long loanId,
            LocalDate disbursalDate,
            BigDecimal disbursedAmount,
            String user);

    DeferredItemType createDeferredItemType(DeferredItemType itemType);

    DeferredItemType findDeferredItemTypeByCode(String code);

    DeferredItem createDeferredItem(
            Long loanId,
            Long deferredItemTypeId,
            BigDecimal amount,
            LocalDate deferralDate,
            LocalDate amortizationEndDate,
            String user);

    List<EIRAmortizationSchedule> generateAmortizationSchedule(
            Long loanId,
            LocalDate recalculationDate,
            BigDecimal newEIR,
            String user);

    RecalculationRun recalculateLoan(
            Long loanId,
            LocalDate recalculationDate,
            RecalculationRun.RecalculationReason reason,
            String user,
            Optional<BigDecimal> newPrincipal,
            Optional<LocalDate> newMaturityDate);

    LoanEventResult processLoanEvent(
            Long loanId,
            LoanEvent.EventType eventType,
            LocalDate eventDate,
            String description,
            String user,
            Optional<BigDecimal> newPrincipal,
            Optional<LocalDate> newMaturityDate);

    RecalculationRun reproduceDoDScenario(Long loanId, String user);

    record LoanEventResult(LoanEvent event, Optional<RecalculationRun> recalculationRun) {
        public LoanEventResult {
            if (event == null) {
                throw new IllegalArgumentException("event is required.");
            }
            recalculationRun = recalculationRun == null ? Optional.empty() : recalculationRun;
        }
    }
}
