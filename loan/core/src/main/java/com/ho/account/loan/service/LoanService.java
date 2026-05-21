package com.ho.account.loan.service;

import com.ho.account.journalledger.application.port.in.JournalUseCase;
import com.ho.account.journalledger.domain.journal.domain.JournalDetail;
import com.ho.account.journalledger.domain.journal.domain.JournalEntry;
import com.ho.account.journalledger.domain.journal.domain.JournalEntryStatus;
import com.ho.account.journalledger.domain.journal.domain.JournalSide;
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
import com.ho.account.loan.infrastructure.persistence.DeferredItemRepository;
import com.ho.account.loan.infrastructure.persistence.DeferredItemTypeRepository;
import com.ho.account.loan.infrastructure.persistence.EIRAmortizationScheduleRepository;
import com.ho.account.loan.infrastructure.persistence.LoanDisbursalRepository;
import com.ho.account.loan.infrastructure.persistence.LoanEventRepository;
import com.ho.account.loan.infrastructure.persistence.LoanRepository;
import com.ho.account.loan.infrastructure.persistence.RecalculationRunRepository;
import com.ho.account.masterdata.core.application.port.out.AccountSubjectPersistencePort;
import com.ho.account.masterdata.core.application.port.out.BusinessPartnerPersistencePort;
import com.ho.account.masterdata.core.application.port.out.CurrencyPersistencePort;
import com.ho.account.masterdata.core.domain.model.AccountSubject;
import com.ho.account.masterdata.core.domain.model.BusinessPartner;
import com.ho.account.masterdata.core.domain.model.Currency;
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
 * 2. 결과를 영속성 어댑터(Repository)를 통해 DB에 저장한 뒤,
 * 3. 회계 모듈 포트(JournalUseCase)를 통해 '대출 전표'를 자동으로 발행합니다.
 * 
 * 타 모듈(Master Data, Journal Ledger)과는 In/Out Port를 통해서만 약하게 결합(Loose Coupling)하여 유지보수성을 높입니다.
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

    private final BusinessPartnerPersistencePort businessPartnerPersistencePort;
    private final CurrencyPersistencePort currencyPersistencePort;
    private final AccountSubjectPersistencePort accountSubjectPersistencePort;
    private final LoanAccountingProperties accountingProperties;
    private final JournalUseCase journalUseCase;

    public Loan createLoan(Loan loan) {
        Long businessPartnerId = Optional.ofNullable(loan.getBusinessPartner())
                .map(BusinessPartner::getId)
                .orElseThrow(() -> new IllegalArgumentException("businessPartnerId is required"));
        String currencyCode = Optional.ofNullable(loan.getCurrency())
                .map(Currency::getCurrencyCode)
                .filter(code -> !code.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("currencyCode is required"));

        BusinessPartner bp = businessPartnerPersistencePort.findById(businessPartnerId)
                .orElseThrow(() -> new EntityNotFoundException("BusinessPartner not found"));
        Currency currency = currencyPersistencePort.findByCode(currencyCode)
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

        AccountSubject cashAccount = resolveAccount(
                accountingProperties.getCashAccountCode(),
                "Cash account not found.");
        AccountSubject loanReceivableAccount = resolveAccount(
                accountingProperties.getLoanReceivableAccountCode(),
                "Loan receivable account not found.");

        JournalEntry disbursalJe = createAutomatedJournalEntry(
                disbursalDate,
                loan.getLoanNumber() + " loan disbursal",
                user,
                "LOAN_DISBURSAL",
                loanId.toString(),
                resolveLoanCurrencyCode(loan),
                disbursedAmount,
                cashAccount,
                loanReceivableAccount
        );
        disbursal.setJournalEntry(disbursalJe);

        return loanDisbursalRepository.save(disbursal);
    }

    public DeferredItemType createDeferredItemType(DeferredItemType itemType) {
        deferredItemTypeRepository.findByCode(itemType.getCode())
                .ifPresent(existing -> {
                    throw new IllegalStateException("Deferred item type already exists: " + itemType.getCode());
                });
        if (itemType.getDeferralMethod() == null) {
            itemType.setDeferralMethod(DeferralMethod.STRAIGHT_LINE);
        }
        if (itemType.getAuditUser() == null) {
            itemType.setAuditUser("SYSTEM");
        }
        return deferredItemTypeRepository.save(itemType);
    }

    @Transactional(readOnly = true)
    public DeferredItemType findDeferredItemTypeByCode(String code) {
        return deferredItemTypeRepository.findByCode(code)
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
        DeferredItemType deferredItemType = deferredItemTypeRepository.findById(deferredItemTypeId)
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

        JournalEntry initialEntry = createDeferredItemInitialJournalEntry(loan, deferredItemType, amount, deferralDate, user);
        deferredItem.setInitialJournalEntry(initialEntry);

        return deferredItemRepository.save(deferredItem);
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
            List<EIRAmortizationSchedule> schedulesToDelete = eirAmortizationScheduleRepository
                    .findByLoanAndScheduleDateGreaterThanEqualOrderByScheduleDateAsc(loan, scheduleStartDate);
            if (!schedulesToDelete.isEmpty()) {
                eirAmortizationScheduleRepository.deleteAll(schedulesToDelete);
            }
        } else {
            List<EIRAmortizationSchedule> schedulesToDelete = eirAmortizationScheduleRepository.findByLoan(loan);
            if (!schedulesToDelete.isEmpty()) {
                eirAmortizationScheduleRepository.deleteAll(schedulesToDelete);
            }
        }

        BigDecimal annualEir = newEIR != null
                ? newEIR
                : (loan.getCurrentEIR() != null ? loan.getCurrentEIR() : loan.getInterestRate());
        loan.setCurrentEIR(annualEir);
        loanRepository.save(loan);

        long monthSpan = ChronoUnit.MONTHS.between(scheduleStartDate, loan.getMaturityDate()) + 1;
        int periods = (int) Math.max(monthSpan, 1L);

        BigDecimal monthlyRate = normalizeAnnualRate(annualEir)
                .divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);
        BigDecimal principalBalance = loan.getPrincipalAmount();
        BigDecimal regularPrincipalRepayment = principalBalance
                .divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);

        BigDecimal totalDeferredAmount = deferredItemRepository.findByLoan(loan).stream()
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
            eirAmortizationScheduleRepository.save(schedule);

            principalBalance = endingBalance;
        }

        return eirAmortizationScheduleRepository.findByLoanOrderByScheduleDateAsc(loan);
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

        BigDecimal recalculatedEir = eirCalculator.calculateEIR(loan, deferredItemRepository.findByLoan(loan));
        loan.setCurrentEIR(recalculatedEir);
        loan.setAuditUser(user);
        loanRepository.save(loan);

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
            AccountSubject cashAccount = resolveAccount(
                    accountingProperties.getCashAccountCode(),
                    "Cash account not found.");
            AccountSubject loanReceivableAccount = resolveAccount(
                    accountingProperties.getLoanReceivableAccountCode(),
                    "Loan receivable account not found.");

            JournalEntry adjustmentEntry = createAutomatedJournalEntry(
                    recalculationDate,
                    loan.getLoanNumber() + " principal adjustment",
                    user,
                    "LOAN_RECALCULATION",
                    loanId.toString(),
                    resolveLoanCurrencyCode(loan),
                    principalDelta,
                    loanReceivableAccount,
                    cashAccount
            );
            run.setAdjustmentJournalEntry(adjustmentEntry);
        }

        RecalculationRun savedRun = recalculationRunRepository.save(run);

        LoanEvent event = new LoanEvent();
        event.setLoan(loan);
        event.setEventDate(recalculationDate);
        event.setEventType(mapReasonToEventType(reason));
        event.setDescription("Loan recalculation triggered by " + reason);
        event.setRecalculationRun(savedRun);
        event.setRelatedJournalEntry(savedRun.getAdjustmentJournalEntry());
        event.setAuditUser(user);
        loanEventRepository.save(event);

        return savedRun;
    }

    public RecalculationRun reproduceDoDScenario(Long loanId, String user) {
        Loan loan = findLoanById(loanId);

        DeferredItemType deferredItemType = deferredItemTypeRepository.findByCode("DOD_DEFERRED_FEE")
                .orElseGet(() -> {
                    DeferredItemType type = new DeferredItemType();
                    type.setCode("DOD_DEFERRED_FEE");
                    type.setName("DoD Deferred Fee");
                    type.setDescription("Deferred fee for DoD loan scenario");
                    type.setDeferralMethod(DeferralMethod.EIR_METHOD);
                    type.setActive(true);
                    type.setAuditUser(user);
                    return deferredItemTypeRepository.save(type);
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

    private JournalEntry createDeferredItemInitialJournalEntry(
            Loan loan,
            DeferredItemType deferredItemType,
            BigDecimal amount,
            LocalDate deferralDate,
            String user) {
        AccountSubject deferredAssetAccount = deferredItemType.getDeferredAssetAccount() != null
                ? deferredItemType.getDeferredAssetAccount()
                : resolveAccount(
                        accountingProperties.getDeferredAssetAccountCode(),
                        "Deferred asset account not found.");
        AccountSubject recognizedIncomeAccount = deferredItemType.getRecognizedIncomeAccount() != null
                ? deferredItemType.getRecognizedIncomeAccount()
                : resolveAccount(
                        accountingProperties.getRecognizedIncomeAccountCode(),
                        "Recognized income account not found.");

        return createAutomatedJournalEntry(
                deferralDate,
                loan.getLoanNumber() + " deferred item recognition",
                user,
                "LOAN_DEFERRED_ITEM",
                loan.getId().toString(),
                resolveLoanCurrencyCode(loan),
                amount,
                recognizedIncomeAccount,
                deferredAssetAccount
        );
    }

    private JournalEntry createAutomatedJournalEntry(
            LocalDate accountingDate,
            String description,
            String createdBy,
            String lineageSourceType,
            String lineageSourceId,
            String currencyCode,
            BigDecimal amount,
            AccountSubject creditAccount,
            AccountSubject debitAccount) {
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
        entry.setCurrencyCode(currencyCode);

        JournalDetail debitDetail = new JournalDetail();
        debitDetail.setSide(JournalSide.DEBIT);
        debitDetail.setAccountCode(debitAccount.getCode());
        debitDetail.setAmount(amount);
        debitDetail.setBaseAmount(amount);
        debitDetail.setDetailDescription(description + " (DEBIT)");
        entry.addDetail(debitDetail);

        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setSide(JournalSide.CREDIT);
        creditDetail.setAccountCode(creditAccount.getCode());
        creditDetail.setAmount(amount);
        creditDetail.setBaseAmount(amount);
        creditDetail.setDetailDescription(description + " (CREDIT)");
        entry.addDetail(creditDetail);

        entry.setSlipNo(accountingDate + "-LOAN-" + System.currentTimeMillis());
        JournalEntry savedEntry = journalUseCase.createJournalEntry(entry);
        postAutomatedJournalEntry(savedEntry, createdBy);
        return journalUseCase.getJournalEntryWithDetails(savedEntry.getId()).orElse(savedEntry);
    }

    private AccountSubject resolveAccount(String accountCode, String notFoundMessage) {
        return accountSubjectPersistencePort.findByCode(accountCode)
                .orElseThrow(() -> new EntityNotFoundException(notFoundMessage + " code=" + accountCode));
    }

    private String resolveLoanCurrencyCode(Loan loan) {
        return Optional.ofNullable(loan.getCurrency())
                .map(Currency::getCurrencyCode)
                .filter(code -> !code.isBlank())
                .orElseThrow(() -> new IllegalStateException("Loan currencyCode is required for journal posting."));
    }

    private void postAutomatedJournalEntry(JournalEntry entry, String user) {
        if (entry == null || entry.getId() == null) {
            throw new IllegalStateException("Loan journal entry was not persisted with an id.");
        }
        journalUseCase.approveJournalEntry(entry.getId(), user);
        journalUseCase.postJournalEntry(entry.getId(), user);
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
