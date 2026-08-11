package com.ho.account.loan.service;

import com.ho.account.loan.application.port.in.LoanUseCase;
import com.ho.account.loan.application.port.out.LoanJournalPort;
import com.ho.account.loan.application.port.out.LoanJournalPort.PostedJournal;
import com.ho.account.loan.application.port.out.LoanPersistencePort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort.AccountReference;
import com.ho.account.loan.application.port.out.LoanReferenceDataPort.LoanReferenceSnapshot;
import com.ho.account.loan.domain.CurrencyRoundingPolicy;
import com.ho.account.loan.domain.DeferredItem;
import com.ho.account.loan.domain.DeferredItemType;
import com.ho.account.loan.domain.DeferredItemType.DeferralMethod;
import com.ho.account.loan.domain.EIRAmortizationSchedule;
import com.ho.account.loan.domain.Loan;
import com.ho.account.loan.domain.LoanDisbursal;
import com.ho.account.loan.domain.LoanEvent;
import com.ho.account.loan.domain.LoanEvent.EventType;
import com.ho.account.loan.domain.RecalculationRun;
import com.ho.account.loan.domain.RecalculationRun.RecalculationReason;
import jakarta.persistence.EntityNotFoundException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * [헥사고날 아키텍처 - 애플리케이션 서비스 (Application Service)]
 *
 * <p>🐣 이 클래스는 대출 업무의 지휘자입니다. 도메인 객체가 금액·상태 규칙을 지키게 하고,
 * 기준정보·영속성·전표는 Loan이 소유한 출력 포트 뒤로 숨긴 채 호출 순서와 트랜잭션 경계를 관리합니다.</p>
 *
 * <p><b>[MSA Transactional Outbox 패턴 적용 및 Dual Write 정합성 해결]</b><br>
 * 트랜잭션 내 외부 전표 API 동기 직접 호출로 인한 Dual Write 정합성 이슈(로컬 DB 커밋 성공 후 네트워크 장애로 전표 발행 실패)를 
 * 해결하기 위해 Transactional Outbox 패턴을 도입했습니다.<br>
 * 1. 로컬 DB 트랜잭션 수반 시 `JournalOutboxEvent`를 Outbox 테이블에 원자적으로 함께 저장합니다.<br>
 * 2. 비동기 릴레이 프로세서(Outbox Relay)가 PENDING 상태 이벤트를 수신측 전표 포트({@link LoanJournalPort})로 안전하게 발행합니다.<br>
 * 3. `lineageSourceType`과 `lineageSourceId` 기반의 멱등성 키(idempotency key)를 통해 최종 정합성(Eventual Consistency)과 중복 발행 방지를 보장합니다.</p>
 */
@Service
@Transactional
@RequiredArgsConstructor
public class LoanService implements LoanUseCase {

    private static final String SYSTEM_ACTOR = "SYSTEM";

    private final LoanPersistencePort persistencePort;
    private final EIRCalculator eirCalculator;
    private final LoanReferenceDataPort referenceDataPort;
    private final LoanAccountingProperties accountingProperties;
    private final LoanJournalPort journalPort;

    @Override
    public Loan createLoan(Loan loan) {
        if (loan == null) {
            throw new IllegalArgumentException("loan is required.");
        }
        String actor = loan.getAuditUser() == null ? SYSTEM_ACTOR : loan.getAuditUser();
        loan.prepareForCreation(actor);
        LoanReferenceSnapshot references = referenceDataPort.requireLoanReferences(
                loan.getBusinessPartnerId(),
                loan.getCurrencyCode(),
                loan.getDisbursalDate());
        loan.setBusinessPartnerId(references.businessPartnerId());
        loan.setCurrencyCode(references.currencyCode());
        loan.attachBusinessPartnerName(references.businessPartnerName());
        return persistencePort.saveLoan(loan);
    }

    @Override
    @Transactional(readOnly = true)
    public Loan findLoanById(Long id) {
        Loan loan = persistencePort.findLoan(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found with id: " + id));
        attachReferenceDescription(loan);
        return loan;
    }

    @Override
    public LoanDisbursal disburseLoan(
            Long loanId,
            LocalDate disbursalDate,
            BigDecimal disbursedAmount,
            String user) {
        Loan loan = findLoanForUpdate(loanId);
        if (persistencePort.existsDisbursal(loanId)) {
            throw new IllegalStateException("Loan has already been disbursed: " + loanId);
        }

        // [금융 회계 통화 정책 적용] 대출 실행 금액에 통화별 절사/반올림 적용 (KRW: 0자리/절사, USD: 2자리/반올림 등)
        CurrencyRoundingPolicy roundingPolicy = CurrencyRoundingPolicy.of(loan.getCurrencyCode());
        BigDecimal roundedDisbursedAmount = roundingPolicy.applyRounding(disbursedAmount);

        LoanDisbursal disbursal = LoanDisbursal.recordDisbursal(
                loan, disbursalDate, roundedDisbursedAmount, user);
        String cashAccountCode = resolveAccountCode(
                accountingProperties.getCashAccountCode(), disbursalDate);
        String loanReceivableAccountCode = resolveAccountCode(
                accountingProperties.getLoanReceivableAccountCode(), disbursalDate);

        loan.activateAfterDisbursal(disbursalDate, roundedDisbursedAmount, user);
        PostedJournal journal = createAutomatedJournalEntry(
                disbursalDate,
                loan.getLoanNumber() + " loan disbursal",
                user,
                "LOAN_DISBURSAL",
                loanId.toString(),
                requireLoanCurrencyCode(loan),
                roundedDisbursedAmount,
                loanReceivableAccountCode,
                cashAccountCode);
        disbursal.linkPostedJournal(journal.journalEntryId(), journal.slipNo());
        persistencePort.saveLoan(loan);
        return persistencePort.saveDisbursal(disbursal);
    }

    @Override
    public DeferredItemType createDeferredItemType(DeferredItemType itemType) {
        if (itemType == null) {
            throw new IllegalArgumentException("deferredItemType is required.");
        }
        persistencePort.findDeferredItemType(itemType.getCode()).ifPresent(existing -> {
            throw new IllegalStateException("Deferred item type already exists: " + itemType.getCode());
        });

        LocalDate effectiveDate = LocalDate.now();
        DeferredAccounts accounts = resolveDeferredAccounts(itemType, effectiveDate);
        DeferredItemType normalized = DeferredItemType.create(
                itemType.getCode(),
                itemType.getName(),
                itemType.getDescription(),
                itemType.getDeferralMethod() == null ? DeferralMethod.STRAIGHT_LINE : itemType.getDeferralMethod(),
                itemType.getEirCashFlowTreatment() == null
                        ? DeferredItemType.EirCashFlowTreatment.CUSTOMER_FEE_INFLOW
                        : itemType.getEirCashFlowTreatment(),
                accounts.deferredAsset().code(),
                accounts.recognizedIncome().code(),
                itemType.isActive(),
                itemType.getAuditUser() == null ? SYSTEM_ACTOR : itemType.getAuditUser());
        normalized.attachAccountDescriptions(
                accounts.deferredAsset().name(),
                accounts.recognizedIncome().name());
        return persistencePort.saveDeferredItemType(normalized);
    }

    @Override
    @Transactional(readOnly = true)
    public DeferredItemType findDeferredItemTypeByCode(String code) {
        DeferredItemType itemType = persistencePort.findDeferredItemType(code)
                .orElseThrow(() -> new EntityNotFoundException(
                        "DeferredItemType not found with code: " + code));
        DeferredAccounts accounts = resolveDeferredAccounts(itemType, LocalDate.now());
        itemType.attachAccountDescriptions(accounts.deferredAsset().name(), accounts.recognizedIncome().name());
        return itemType;
    }

    @Override
    public DeferredItem createDeferredItem(
            Long loanId,
            Long deferredItemTypeId,
            BigDecimal amount,
            LocalDate deferralDate,
            LocalDate amortizationEndDate,
            String user) {
        Loan loan = findLoanForUpdate(loanId);
        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new IllegalStateException("Deferred items require an active loan: " + loan.getStatus());
        }
        DeferredItemType itemType = persistencePort.findDeferredItemType(deferredItemTypeId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "DeferredItemType not found with id: " + deferredItemTypeId));
        DeferredAccounts accounts = resolveDeferredAccounts(itemType, deferralDate);

        // [금융 회계 통화 정책 적용] 이연 항목 금액에 통화별 절사/반올림 적용
        CurrencyRoundingPolicy roundingPolicy = CurrencyRoundingPolicy.of(loan.getCurrencyCode());
        BigDecimal roundedAmount = roundingPolicy.applyRounding(amount);

        DeferredItem deferredItem = DeferredItem.create(
                loan, itemType, roundedAmount, deferralDate, amortizationEndDate, user);
        PostedJournal initialEntry = createAutomatedJournalEntry(
                deferralDate,
                loan.getLoanNumber() + " deferred item recognition",
                user,
                "LOAN_DEFERRED_ITEM",
                loan.getId().toString(),
                requireLoanCurrencyCode(loan),
                roundedAmount,
                accounts.deferredAsset().code(),
                accounts.recognizedIncome().code());
        deferredItem.linkInitialJournal(initialEntry.journalEntryId(), initialEntry.slipNo());
        return persistencePort.saveDeferredItem(deferredItem);
    }

    @Override
    public List<EIRAmortizationSchedule> generateAmortizationSchedule(
            Long loanId,
            LocalDate recalculationDate,
            BigDecimal newEIR,
            String user) {
        Loan loan = findLoanForUpdate(loanId);
        return generateAmortizationScheduleForLoan(loan, recalculationDate, newEIR, user);
    }

    @Override
    public RecalculationRun recalculateLoan(
            Long loanId,
            LocalDate recalculationDate,
            RecalculationReason reason,
            String user,
            Optional<BigDecimal> newPrincipal,
            Optional<LocalDate> newMaturityDate) {
        return recalculateLoanWithEvent(
                loanId,
                recalculationDate,
                reason,
                mapReasonToEventType(reason),
                "Loan recalculation triggered by " + reason,
                user,
                safeOptional(newPrincipal),
                safeOptional(newMaturityDate)).recalculationRun().orElseThrow();
    }

    @Override
    public LoanEventResult processLoanEvent(
            Long loanId,
            EventType eventType,
            LocalDate eventDate,
            String description,
            String user,
            Optional<BigDecimal> newPrincipal,
            Optional<LocalDate> newMaturityDate) {
        if (eventType == null) {
            throw new IllegalArgumentException("eventType is required.");
        }
        Optional<BigDecimal> principal = safeOptional(newPrincipal);
        Optional<LocalDate> maturity = safeOptional(newMaturityDate);
        return switch (eventType) {
            case EARLY_REPAYMENT -> recalculateLoanWithEvent(
                    loanId, eventDate, RecalculationReason.EARLY_REPAYMENT, eventType,
                    description, user, principal, maturity);
            case CONDITION_CHANGE -> recalculateLoanWithEvent(
                    loanId, eventDate, RecalculationReason.CONDITION_CHANGE, eventType,
                    description, user, principal, maturity);
            case RESCHEDULE -> recalculateLoanWithEvent(
                    loanId, eventDate, RecalculationReason.RESCHEDULE, eventType,
                    description, user, principal, maturity);
            case DEFAULT, RECOVERY, OTHER -> recordLifecycleEvent(
                    loanId, eventType, eventDate, description, user, principal, maturity);
        };
    }

    @Override
    public RecalculationRun reproduceDoDScenario(Long loanId, String user) {
        Loan loan = findLoanById(loanId);
        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new IllegalStateException("DoD scenario requires an active loan.");
        }

        DeferredItemType deferredItemType = persistencePort.findDeferredItemType("DOD_DEFERRED_FEE")
                .orElseGet(() -> createDeferredItemType(DeferredItemType.create(
                        "DOD_DEFERRED_FEE",
                        "DoD Deferred Fee",
                        "Deferred fee for DoD loan scenario",
                        DeferralMethod.EIR_METHOD,
                        DeferredItemType.EirCashFlowTreatment.CUSTOMER_FEE_INFLOW,
                        accountingProperties.getDeferredAssetAccountCode(),
                        accountingProperties.getRecognizedIncomeAccountCode(),
                        true,
                        user)));

        LocalDate deferralDate = loan.getDisbursalDate();
        LocalDate amortizationEndDate = deferralDate.plusMonths(12).isAfter(loan.getMaturityDate())
                ? loan.getMaturityDate()
                : deferralDate.plusMonths(12);
        BigDecimal deferredAmount = loan.getPrincipalAmount()
                .multiply(new BigDecimal("0.01"))
                .setScale(2, RoundingMode.HALF_UP);
        createDeferredItem(
                loanId,
                deferredItemType.getId(),
                deferredAmount,
                deferralDate,
                amortizationEndDate,
                user);

        generateAmortizationSchedule(
                loanId,
                loan.getDisbursalDate(),
                loan.getCurrentEIR(),
                user);

        LocalDate recalculationDate = loan.getDisbursalDate().plusMonths(3);
        if (!recalculationDate.isBefore(loan.getMaturityDate())) {
            throw new IllegalStateException("DoD scenario requires a loan term longer than three months.");
        }
        BigDecimal newPrincipal = loan.getOutstandingPrincipal()
                .multiply(new BigDecimal("0.70"))
                .setScale(2, RoundingMode.HALF_UP);
        return recalculateLoan(
                loanId,
                recalculationDate,
                RecalculationReason.EARLY_REPAYMENT,
                user,
                Optional.of(newPrincipal),
                Optional.empty());
    }

    private LoanEventResult recalculateLoanWithEvent(
            Long loanId,
            LocalDate recalculationDate,
            RecalculationReason reason,
            EventType eventType,
            String description,
            String user,
            Optional<BigDecimal> newPrincipal,
            Optional<LocalDate> newMaturityDate) {
        if (reason == null || recalculationDate == null) {
            throw new IllegalArgumentException("recalculationDate and reason are required.");
        }
        Loan loan = findLoanForUpdate(loanId);
        CurrencyRoundingPolicy roundingPolicy = CurrencyRoundingPolicy.of(loan.getCurrencyCode());

        BigDecimal oldOutstanding = loan.getOutstandingPrincipal();
        BigDecimal oldEir = loan.getCurrentEIR();
        LocalDate oldMaturityDate = loan.getMaturityDate();
        BigDecimal proposedOutstanding = newPrincipal.map(roundingPolicy::applyRounding).orElse(oldOutstanding);
        LocalDate proposedMaturityDate = newMaturityDate.orElse(oldMaturityDate);

        if (reason == RecalculationReason.EARLY_REPAYMENT
                && (newPrincipal.isEmpty() || proposedOutstanding.compareTo(oldOutstanding) >= 0)) {
            throw new IllegalArgumentException(
                    "EARLY_REPAYMENT requires a lower newPrincipal outstanding balance.");
        }

        List<DeferredItem> deferredItems = persistencePort.findDeferredItems(loan);
        BigDecimal recalculatedEir = eirCalculator.calculateEIR(
                loan, proposedOutstanding, proposedMaturityDate, deferredItems);

        AdjustmentAccounts adjustmentAccounts = null;
        BigDecimal principalDelta = roundingPolicy.applyRounding(oldOutstanding.subtract(proposedOutstanding));
        if (reason == RecalculationReason.EARLY_REPAYMENT && principalDelta.signum() > 0) {
            adjustmentAccounts = new AdjustmentAccounts(
                    resolveAccountCode(accountingProperties.getCashAccountCode(), recalculationDate),
                    resolveAccountCode(accountingProperties.getLoanReceivableAccountCode(), recalculationDate));
        }

        loan.applyRecalculatedTerms(
                proposedOutstanding,
                proposedMaturityDate,
                recalculatedEir,
                recalculationDate,
                user);
        persistencePort.saveLoan(loan);
        List<EIRAmortizationSchedule> schedules = generateAmortizationScheduleForLoan(
                loan, recalculationDate, recalculatedEir, user);

        String impactAnalysis = """
                {"oldOutstandingPrincipal":%s,"newOutstandingPrincipal":%s,"oldEIR":%s,"newEIR":%s}
                """.formatted(oldOutstanding, proposedOutstanding, oldEir, recalculatedEir).trim();
        RecalculationRun run = RecalculationRun.record(
                loan,
                recalculationDate,
                reason,
                oldEir,
                recalculatedEir,
                oldMaturityDate,
                proposedMaturityDate,
                schedules.isEmpty() ? null : schedules.get(0),
                impactAnalysis,
                user);

        if (adjustmentAccounts != null) {
            PostedJournal adjustmentJournal = createAutomatedJournalEntry(
                    recalculationDate,
                    loan.getLoanNumber() + " principal adjustment",
                    user,
                    "LOAN_RECALCULATION",
                    loanId.toString(),
                    requireLoanCurrencyCode(loan),
                    principalDelta,
                    adjustmentAccounts.cashAccountCode(),
                    adjustmentAccounts.loanReceivableAccountCode());
            run.linkAdjustmentJournal(adjustmentJournal.journalEntryId(), adjustmentJournal.slipNo());
        }

        RecalculationRun savedRun = persistencePort.saveRecalculationRun(run);
        LoanEvent event = LoanEvent.record(
                loan,
                eventType,
                recalculationDate,
                defaultDescription(description, "Loan recalculation triggered by " + reason),
                savedRun,
                user);
        LoanEvent savedEvent = persistencePort.saveLoanEvent(event);
        return new LoanEventResult(savedEvent, Optional.of(savedRun));
    }

    private LoanEventResult recordLifecycleEvent(
            Long loanId,
            EventType eventType,
            LocalDate eventDate,
            String description,
            String user,
            Optional<BigDecimal> newPrincipal,
            Optional<LocalDate> newMaturityDate) {
        if (newPrincipal.isPresent() || newMaturityDate.isPresent()) {
            throw new IllegalArgumentException(
                    eventType + " does not accept recalculation fields.");
        }
        Loan loan = findLoanForUpdate(loanId);
        if (eventDate == null || eventDate.isBefore(loan.getDisbursalDate())) {
            throw new IllegalArgumentException("eventDate must not be before the loan disbursal date.");
        }
        if (eventType == EventType.DEFAULT) {
            loan.markDefaulted(user);
            persistencePort.saveLoan(loan);
        } else if (eventType == EventType.RECOVERY) {
            loan.recoverFromDefault(user);
            persistencePort.saveLoan(loan);
        }
        LoanEvent event = LoanEvent.record(
                loan,
                eventType,
                eventDate,
                defaultDescription(description, "Loan event: " + eventType),
                null,
                user);
        return new LoanEventResult(persistencePort.saveLoanEvent(event), Optional.empty());
    }

    private List<EIRAmortizationSchedule> generateAmortizationScheduleForLoan(
            Loan loan,
            LocalDate recalculationDate,
            BigDecimal newEir,
            String user) {
        if (loan.getStatus() != Loan.LoanStatus.ACTIVE) {
            throw new IllegalStateException("Amortization schedules require an active loan: " + loan.getStatus());
        }
        LocalDate scheduleStartDate = recalculationDate == null
                ? loan.getDisbursalDate()
                : recalculationDate;
        BigDecimal annualEir = newEir == null
                ? Optional.ofNullable(loan.getCurrentEIR()).orElse(loan.getInterestRate())
                : newEir;
        BigDecimal totalDeferredAmount = persistencePort.findDeferredItems(loan).stream()
                .filter(item -> item.getStatus() != DeferredItem.DeferredItemStatus.CANCELLED)
                .map(DeferredItem::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        boolean recalculated = !scheduleStartDate.equals(loan.getDisbursalDate());

        List<EIRAmortizationSchedule> newSchedules = EIRAmortizationSchedule.generateMonthly(
                loan,
                scheduleStartDate,
                annualEir,
                totalDeferredAmount,
                recalculated,
                user);

        List<EIRAmortizationSchedule> oldSchedules = recalculated
                ? persistencePort.findSchedulesFrom(loan, scheduleStartDate)
                : persistencePort.findSchedules(loan);
        if (!oldSchedules.isEmpty()) {
            persistencePort.deleteSchedules(oldSchedules);
        }
        loan.updateCurrentEir(annualEir, user);
        persistencePort.saveLoan(loan);
        persistencePort.saveSchedules(newSchedules);
        return persistencePort.findSchedulesOrdered(loan);
    }

    private DeferredAccounts resolveDeferredAccounts(DeferredItemType itemType, LocalDate effectiveDate) {
        String deferredCode = Optional.ofNullable(itemType.getDeferredAssetAccountCode())
                .orElse(accountingProperties.getDeferredAssetAccountCode());
        String incomeCode = Optional.ofNullable(itemType.getRecognizedIncomeAccountCode())
                .orElse(accountingProperties.getRecognizedIncomeAccountCode());
        AccountReference deferredAsset = referenceDataPort.requireAccount(deferredCode, effectiveDate);
        AccountReference recognizedIncome = referenceDataPort.requireAccount(incomeCode, effectiveDate);
        itemType.attachAccountDescriptions(deferredAsset.name(), recognizedIncome.name());
        return new DeferredAccounts(deferredAsset, recognizedIncome);
    }

    private PostedJournal createAutomatedJournalEntry(
            LocalDate accountingDate,
            String description,
            String createdBy,
            String lineageSourceType,
            String lineageSourceId,
            String currencyCode,
            BigDecimal amount,
            String debitAccountCode,
            String creditAccountCode) {
        // [금융 회계 통화 정책 적용]
        // 통화 규격(KRW: 0자리/절사, USD/EUR: 2자리/반올림 등)에 맞춰 전표 금액(Line Amount)을 최종 정제합니다.
        CurrencyRoundingPolicy roundingPolicy = CurrencyRoundingPolicy.of(currencyCode);
        BigDecimal roundedAmount = roundingPolicy.applyRounding(amount);

        return journalPort.post(new LoanJournalPort.LoanJournalCommand(
                accountingDate,
                description,
                createdBy,
                lineageSourceType,
                lineageSourceId,
                currencyCode,
                List.of(
                        new LoanJournalPort.LoanJournalLine(
                                "DEBIT", debitAccountCode, roundedAmount, description + " (DEBIT)"),
                        new LoanJournalPort.LoanJournalLine(
                                "CREDIT", creditAccountCode, roundedAmount, description + " (CREDIT)"))));
    }

    private Loan findLoanForUpdate(Long loanId) {
        if (loanId == null || loanId < 1) {
            throw new IllegalArgumentException("loanId must be positive.");
        }
        return persistencePort.findLoanForUpdate(loanId)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found with id: " + loanId));
    }

    private void attachReferenceDescription(Loan loan) {
        LoanReferenceSnapshot references = referenceDataPort.requireLoanReferences(
                loan.getBusinessPartnerId(),
                loan.getCurrencyCode(),
                loan.getDisbursalDate());
        loan.attachBusinessPartnerName(references.businessPartnerName());
    }

    private String resolveAccountCode(String accountCode, LocalDate effectiveDate) {
        return referenceDataPort.requireAccount(accountCode, effectiveDate).code();
    }

    private String requireLoanCurrencyCode(Loan loan) {
        String code = loan.getCurrencyCode();
        if (code == null || code.isBlank()) {
            throw new IllegalStateException("Loan currencyCode is required for journal posting.");
        }
        return code;
    }

    private EventType mapReasonToEventType(RecalculationReason reason) {
        if (reason == null) {
            throw new IllegalArgumentException("reason is required.");
        }
        return switch (reason) {
            case EARLY_REPAYMENT -> EventType.EARLY_REPAYMENT;
            case CONDITION_CHANGE -> EventType.CONDITION_CHANGE;
            case RESCHEDULE -> EventType.RESCHEDULE;
            case OTHER -> EventType.OTHER;
        };
    }

    private String defaultDescription(String description, String fallback) {
        return description == null || description.isBlank() ? fallback : description.trim();
    }

    private <T> Optional<T> safeOptional(Optional<T> value) {
        return value == null ? Optional.empty() : value;
    }

    private record DeferredAccounts(
            AccountReference deferredAsset,
            AccountReference recognizedIncome) {
    }

    private record AdjustmentAccounts(
            String cashAccountCode,
            String loanReceivableAccountCode) {
    }
}
