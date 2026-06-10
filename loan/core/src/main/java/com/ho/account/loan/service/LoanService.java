package com.ho.account.loan.service;

import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.loan.application.port.out.LoanJournalPort.PostedJournal;
import com.ho.account.loan.application.port.out.LoanPersistencePort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.domain.LoanEvent;
import com.ho.account.loan.domain.RecalculationRun;
import com.ho.account.loan.domain.DeferredItemType.DeferralMethod;
import com.ho.account.loan.domain.Loan.LoanStatus;
import com.ho.account.loan.domain.LoanEvent.EventType;
import com.ho.account.loan.domain.RecalculationRun.RecalculationReason;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 * 
 * 🐣 [초보자를 위한 설명]
 * 이 클래스는 대출 모듈의 '지휘자' 역할을 합니다.
 * "대출을 실행해줘(disburseLoan)!", "대출 조건을 변경해줘(recalculateLoan)!" 라는 외부 요청이 들어오면,
 * 1. 대출 도메인(Loan) 객체를 불러와 핵심 계산(EIR 상각 스케줄 생성 등)을 맡기고,
 * 2. 결과를 영속성 출력 포트를 통해 DB에 저장한 뒤,
 * 3. 대출 소유 회계 포트를 통해 '대출 전표'를 자동으로 발행합니다.
 * 
 * 타 모듈(Master Data, Journal Ledger)과는 In/Out Port를 통해서만 약하게 결합(Loose Coupling)하여 유지보수성을 높입니다.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class LoanService {

    private final LoanPersistencePort persistencePort;
    private final EIRCalculator eirCalculator;
    private final LoanReferenceDataPort referenceDataPort;
    private final LoanAccountingProperties accountingProperties;
    private final LoanJournalPort journalPort;

    public Loan createLoan(Loan loan) {
        referenceDataPort.attachValidatedLoanReferences(loan);

        loan.setInitialEIR(loan.getInterestRate());
        loan.setCurrentEIR(loan.getInitialEIR());
        loan.setStatus(LoanStatus.ACTIVE);
        return persistencePort.saveLoan(loan);
    }

    @Transactional(readOnly = true)
    public Loan findLoanById(Long id) {
        return persistencePort.findLoan(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found with id: " + id));
    }

    public LoanDisbursal disburseLoan(Long loanId, LocalDate disbursalDate, BigDecimal disbursedAmount, String user) {
        Loan loan = findLoanById(loanId);

        LoanDisbursal disbursal = new LoanDisbursal();
        disbursal.setLoan(loan);
        disbursal.setDisbursalDate(disbursalDate);
        disbursal.setDisbursedAmount(disbursedAmount);
        disbursal.setAuditUser(user);

        String cashAccountCode = resolveAccountCode(accountingProperties.getCashAccountCode());
        String loanReceivableAccountCode = resolveAccountCode(accountingProperties.getLoanReceivableAccountCode());

        PostedJournal disbursalJournal = createAutomatedJournalEntry(
                disbursalDate,
                loan.getLoanNumber() + " loan disbursal",
                user,
                "LOAN_DISBURSAL",
                loanId.toString(),
                resolveLoanCurrencyCode(loan),
                disbursedAmount,
                cashAccountCode,
                loanReceivableAccountCode
        );
        disbursal.setJournalEntryId(disbursalJournal.journalEntryId());
        disbursal.setJournalEntrySlipNo(disbursalJournal.slipNo());

        return persistencePort.saveDisbursal(disbursal);
    }

    public DeferredItemType createDeferredItemType(DeferredItemType itemType) {
        persistencePort.findDeferredItemType(itemType.getCode())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Deferred item type already exists: " + itemType.getCode());
                });
        if (itemType.getDeferralMethod() == null) {
            itemType.setDeferralMethod(DeferralMethod.STRAIGHT_LINE);
        }
        if (itemType.getAuditUser() == null) {
            itemType.setAuditUser("SYSTEM");
        }
        return persistencePort.saveDeferredItemType(itemType);
    }

    @Transactional(readOnly = true)
    public DeferredItemType findDeferredItemTypeByCode(String code) {
        return persistencePort.findDeferredItemType(code)
                .orElseThrow(() -> new EntityNotFoundException("DeferredItemType not found with code: " + code));
    }

    public DeferredItem createDeferredItem(
            Long loanId,
            Long deferredItemTypeId,
            BigDecimal amount,
            LocalDate deferralDate,
            LocalDate amortizationEndDate,
            String user) {
        Loan loan = findLoanById(loanId);
        DeferredItemType deferredItemType = persistencePort.findDeferredItemType(deferredItemTypeId)
                .orElseThrow(() -> new EntityNotFoundException("DeferredItemType not found with id: " + deferredItemTypeId));

        if (!deferredItemType.isActive()) {
            throw new IllegalStateException("DeferredItemType is inactive: " + deferredItemType.getCode());
        }
        if (amortizationEndDate.isBefore(deferralDate)) {
            throw new IllegalArgumentException("amortizationEndDate must be on or after deferralDate");
        }

        DeferredItem deferredItem = new DeferredItem();
        deferredItem.setLoan(loan);
        deferredItem.setDeferredItemType(deferredItemType);
        deferredItem.setAmount(amount);
        deferredItem.setDeferralDate(deferralDate);
        deferredItem.setAmortizationStartDate(deferralDate);
        deferredItem.setAmortizationEndDate(amortizationEndDate);
        deferredItem.setRemainingAmount(amount);
        deferredItem.setStatus(DeferredItem.DeferredItemStatus.DEFERRED);
        deferredItem.setAuditUser(user);

        PostedJournal initialEntry = createDeferredItemInitialJournalEntry(loan, deferredItemType, amount, deferralDate, user);
        deferredItem.setInitialJournalEntryId(initialEntry.journalEntryId());
        deferredItem.setInitialJournalEntrySlipNo(initialEntry.slipNo());

        return persistencePort.saveDeferredItem(deferredItem);
    }

    public List<EIRAmortizationSchedule> generateAmortizationSchedule(
            Long loanId,
            LocalDate recalculationDate,
            BigDecimal newEIR,
            String user) {
        Loan loan = findLoanById(loanId);
        LocalDate scheduleStartDate = recalculationDate != null ? recalculationDate : loan.getDisbursalDate();
        if (scheduleStartDate.isAfter(loan.getMaturityDate())) {
            throw new IllegalArgumentException("schedule start date is after maturity date");
        }

        boolean recalculated = !scheduleStartDate.equals(loan.getDisbursalDate());
        if (recalculated) {
            List<EIRAmortizationSchedule> schedulesToDelete = persistencePort.findSchedulesFrom(loan, scheduleStartDate);
            if (!schedulesToDelete.isEmpty()) {
                persistencePort.deleteSchedules(schedulesToDelete);
            }
        } else {
            List<EIRAmortizationSchedule> schedulesToDelete = persistencePort.findSchedules(loan);
            if (!schedulesToDelete.isEmpty()) {
                persistencePort.deleteSchedules(schedulesToDelete);
            }
        }

        BigDecimal annualEir = newEIR != null
                ? newEIR
                : (loan.getCurrentEIR() != null ? loan.getCurrentEIR() : loan.getInterestRate());
        loan.setCurrentEIR(annualEir);
        persistencePort.saveLoan(loan);

        long monthSpan = ChronoUnit.MONTHS.between(scheduleStartDate, loan.getMaturityDate()) + 1;
        int periods = (int) Math.max(monthSpan, 1L);

        BigDecimal monthlyRate = normalizeAnnualRate(annualEir)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal principalBalance = loan.getPrincipalAmount();
        BigDecimal regularPrincipalRepayment = principalBalance
                .divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);

        BigDecimal totalDeferredAmount = persistencePort.findDeferredItems(loan).stream()
                .map(DeferredItem::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal remainingDeferredAmount = totalDeferredAmount;
        BigDecimal regularDeferredAmortization = totalDeferredAmount.signum() == 0
                ? BigDecimal.ZERO
                : totalDeferredAmount.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);

        for (int i = 0; i < periods; i++) {
            LocalDate scheduleDate = scheduleStartDate.plusMonths(i);
            if (scheduleDate.isAfter(loan.getMaturityDate())) {
                break;
            }

            boolean lastPeriod = i == periods - 1 || scheduleDate.equals(loan.getMaturityDate());
            BigDecimal beginningBalance = principalBalance;
            BigDecimal interestIncome = beginningBalance.multiply(monthlyRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalRepayment = lastPeriod
                    ? beginningBalance
                    : regularPrincipalRepayment.min(beginningBalance);
            BigDecimal endingBalance = beginningBalance.subtract(principalRepayment).max(BigDecimal.ZERO);

            BigDecimal deferredAmortization = BigDecimal.ZERO;
            if (remainingDeferredAmount.signum() > 0) {
                deferredAmortization = lastPeriod
                        ? remainingDeferredAmount
                        : regularDeferredAmortization.min(remainingDeferredAmount);
                remainingDeferredAmount = remainingDeferredAmount.subtract(deferredAmortization);
            }

            EIRAmortizationSchedule schedule = new EIRAmortizationSchedule();
            schedule.setLoan(loan);
            schedule.setScheduleDate(scheduleDate);
            schedule.setBeginningBalance(beginningBalance);
            schedule.setInterestIncome(interestIncome);
            schedule.setPrincipalRepayment(principalRepayment);
            schedule.setEndingBalance(endingBalance);
            schedule.setDeferredItemAmortization(deferredAmortization);
            schedule.setCashFlow(principalRepayment.add(interestIncome));
            schedule.setRecalculated(recalculated);
            schedule.setAuditUser(user);
            persistencePort.saveSchedule(schedule);

            principalBalance = endingBalance;
        }

        return persistencePort.findSchedulesOrdered(loan);
    }

    public RecalculationRun recalculateLoan(
            Long loanId,
            LocalDate recalculationDate,
            RecalculationReason reason,
            String user,
            Optional<BigDecimal> newPrincipal,
            Optional<LocalDate> newMaturityDate) {
        Loan loan = findLoanById(loanId);

        BigDecimal oldPrincipal = loan.getPrincipalAmount();
        BigDecimal oldEir = loan.getCurrentEIR();
        LocalDate oldMaturityDate = loan.getMaturityDate();

        newPrincipal.ifPresent(loan::setPrincipalAmount);
        newMaturityDate.ifPresent(loan::setMaturityDate);

        BigDecimal recalculatedEir = eirCalculator.calculateEIR(loan, persistencePort.findDeferredItems(loan));
        loan.setCurrentEIR(recalculatedEir);
        loan.setAuditUser(user);
        persistencePort.saveLoan(loan);

        List<EIRAmortizationSchedule> recalculatedSchedules =
                generateAmortizationSchedule(loan.getId(), recalculationDate, recalculatedEir, user);

        RecalculationRun run = new RecalculationRun();
        run.setLoan(loan);
        run.setRecalculationDate(recalculationDate);
        run.setReason(reason);
        run.setOldEIR(oldEir);
        run.setNewEIR(recalculatedEir);
        run.setOldMaturityDate(oldMaturityDate);
        run.setNewMaturityDate(loan.getMaturityDate());
        if (!recalculatedSchedules.isEmpty()) {
            run.setRecalculatedAmortizationScheduleStart(recalculatedSchedules.get(0));
        }
        run.setImpactAnalysis(String.format(
                "{\"oldPrincipal\":%s,\"newPrincipal\":%s,\"oldEIR\":%s,\"newEIR\":%s}",
                oldPrincipal, loan.getPrincipalAmount(), oldEir, recalculatedEir));
        run.setAuditUser(user);

        BigDecimal principalDelta = oldPrincipal.subtract(loan.getPrincipalAmount());
        if (reason == RecalculationReason.EARLY_REPAYMENT && principalDelta.signum() > 0) {
            String cashAccountCode = resolveAccountCode(accountingProperties.getCashAccountCode());
            String loanReceivableAccountCode = resolveAccountCode(accountingProperties.getLoanReceivableAccountCode());

            PostedJournal adjustmentEntry = createAutomatedJournalEntry(
                    recalculationDate,
                    loan.getLoanNumber() + " principal adjustment",
                    user,
                    "LOAN_RECALCULATION",
                    loanId.toString(),
                    resolveLoanCurrencyCode(loan),
                    principalDelta,
                    loanReceivableAccountCode,
                    cashAccountCode
            );
            run.setAdjustmentJournalEntryId(adjustmentEntry.journalEntryId());
            run.setAdjustmentJournalEntrySlipNo(adjustmentEntry.slipNo());
        }

        RecalculationRun savedRun = persistencePort.saveRecalculationRun(run);

        LoanEvent event = new LoanEvent();
        event.setLoan(loan);
        event.setEventDate(recalculationDate);
        event.setEventType(mapReasonToEventType(reason));
        event.setDescription("Loan recalculation triggered by " + reason);
        event.setRecalculationRun(savedRun);
        event.setRelatedJournalEntryId(savedRun.getAdjustmentJournalEntryId());
        event.setAuditUser(user);
        persistencePort.saveLoanEvent(event);

        return savedRun;
    }

    public RecalculationRun reproduceDoDScenario(Long loanId, String user) {
        Loan loan = findLoanById(loanId);

        DeferredItemType deferredItemType = persistencePort.findDeferredItemType("DOD_DEFERRED_FEE")
                .orElseGet(() -> {
                    DeferredItemType type = new DeferredItemType();
                    type.setCode("DOD_DEFERRED_FEE");
                    type.setName("DoD Deferred Fee");
                    type.setDescription("Deferred fee for DoD loan scenario");
                    type.setDeferralMethod(DeferralMethod.EIR_METHOD);
                    type.setActive(true);
                    type.setAuditUser(user);
                    return persistencePort.saveDeferredItemType(type);
                });

        LocalDate deferralDate = loan.getDisbursalDate();
        LocalDate amortizationEndDate = deferralDate.plusMonths(12);
        if (amortizationEndDate.isAfter(loan.getMaturityDate())) {
            amortizationEndDate = loan.getMaturityDate();
        }

        BigDecimal deferredAmount = loan.getPrincipalAmount()
                .multiply(new BigDecimal("0.01"))
                .setScale(2, RoundingMode.HALF_UP);
        createDeferredItem(
                loanId,
                deferredItemType.getId(),
                deferredAmount,
                deferralDate,
                amortizationEndDate,
                user
        );

        generateAmortizationSchedule(
                loanId,
                loan.getDisbursalDate(),
                loan.getCurrentEIR() != null ? loan.getCurrentEIR() : loan.getInterestRate(),
                user
        );

        LocalDate recalculationDate = loan.getDisbursalDate().plusMonths(3);
        if (recalculationDate.isAfter(loan.getMaturityDate())) {
            recalculationDate = loan.getMaturityDate();
        }

        BigDecimal newPrincipal = loan.getPrincipalAmount()
                .multiply(new BigDecimal("0.70"))
                .setScale(2, RoundingMode.HALF_UP);

        return recalculateLoan(
                loanId,
                recalculationDate,
                RecalculationReason.EARLY_REPAYMENT,
                user,
                Optional.of(newPrincipal),
                Optional.empty()
        );
    }

    private PostedJournal createDeferredItemInitialJournalEntry(
            Loan loan,
            DeferredItemType deferredItemType,
            BigDecimal amount,
            LocalDate deferralDate,
            String user) {
        String deferredAssetAccountCode = resolveAccountCode(
                Optional.ofNullable(deferredItemType.getDeferredAssetAccountCode())
                        .orElse(accountingProperties.getDeferredAssetAccountCode()));
        String recognizedIncomeAccountCode = resolveAccountCode(
                Optional.ofNullable(deferredItemType.getRecognizedIncomeAccountCode())
                        .orElse(accountingProperties.getRecognizedIncomeAccountCode()));

        return createAutomatedJournalEntry(
                deferralDate,
                loan.getLoanNumber() + " deferred item recognition",
                user,
                "LOAN_DEFERRED_ITEM",
                loan.getId().toString(),
                resolveLoanCurrencyCode(loan),
                amount,
                recognizedIncomeAccountCode,
                deferredAssetAccountCode
        );
    }

    private PostedJournal createAutomatedJournalEntry(
            LocalDate accountingDate,
            String description,
            String createdBy,
            String lineageSourceType,
            String lineageSourceId,
            String currencyCode,
            BigDecimal amount,
            String creditAccountCode,
            String debitAccountCode) {
        return journalPort.post(new LoanJournalPort.LoanJournalCommand(
                accountingDate,
                description,
                createdBy,
                lineageSourceType,
                lineageSourceId,
                currencyCode,
                List.of(
                        new LoanJournalPort.LoanJournalLine("DEBIT", debitAccountCode, amount, description + " (DEBIT)"),
                        new LoanJournalPort.LoanJournalLine("CREDIT", creditAccountCode, amount, description + " (CREDIT)"))));
    }

    private String resolveAccountCode(String accountCode) {
        return referenceDataPort.requireAccountCode(accountCode);
    }

    private String resolveLoanCurrencyCode(Loan loan) {
        return loan.getCurrencyCode();
    }

    private LoanEvent.EventType mapReasonToEventType(RecalculationReason reason) {
        return switch (reason) {
            case EARLY_REPAYMENT -> EventType.EARLY_REPAYMENT;
            case CONDITION_CHANGE -> EventType.CONDITION_CHANGE;
            case RESCHEDULE -> EventType.RESCHEDULE;
            default -> EventType.OTHER;
        };
    }

    private BigDecimal normalizeAnnualRate(BigDecimal annualRate) {
        if (annualRate == null) {
            return BigDecimal.ZERO;
        }
        if (annualRate.compareTo(BigDecimal.ONE) > 0) {
            return annualRate.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        }
        return annualRate;
    }
}
