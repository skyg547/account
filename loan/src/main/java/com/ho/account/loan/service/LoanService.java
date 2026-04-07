package com.ho.account.loan.service;

import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.domain.BusinessPartner;
import com.ho.account.basic.domain.Currency;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.basic.repository.BusinessPartnerRepository;
import com.ho.account.basic.repository.CurrencyRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.repository.JournalDetailRepository;
import com.ho.account.journal.repository.JournalEntryRepository;
import com.ho.account.loan.domain.*;
import com.ho.account.loan.domain.DeferredItemType.DeferralMethod;
import com.ho.account.loan.domain.Loan.LoanStatus;
import com.ho.account.loan.domain.LoanEvent.EventType;
import com.ho.account.loan.domain.RecalculationRun.RecalculationReason;
import com.ho.account.loan.repository.*;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 대출 회계 (Loan Accounting) 관련 비즈니스 로직을 처리하는 서비스 클래스.
 * 대출 생성, 실행, 이연 부대손익 관리, EIR 상각 스케줄 생성 및 재계산, 상환 처리 등을 담당합니다.
 */
@Service
@Transactional
public class LoanService {

    private final LoanRepository loanRepository;
    private final LoanDisbursalRepository loanDisbursalRepository;
    private final LoanEventRepository loanEventRepository;
    private final DeferredItemTypeRepository deferredItemTypeRepository;
    private final DeferredItemRepository deferredItemRepository;
    private final EIRAmortizationScheduleRepository eirAmortizationScheduleRepository;
    private final RecalculationRunRepository recalculationRunRepository;
    private final EIRCalculator eirCalculator;

    private final BusinessPartnerRepository businessPartnerRepository;
    private final CurrencyRepository currencyRepository;
    private final AccountSubjectRepository accountSubjectRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalDetailRepository journalDetailRepository;

    @Autowired
    public LoanService(LoanRepository loanRepository,
                       LoanDisbursalRepository loanDisbursalRepository,
                       LoanEventRepository loanEventRepository,
                       DeferredItemTypeRepository deferredItemTypeRepository,
                       DeferredItemRepository deferredItemRepository,
                       EIRAmortizationScheduleRepository eirAmortizationScheduleRepository,
                       RecalculationRunRepository recalculationRunRepository,
                       EIRCalculator eirCalculator,
                       BusinessPartnerRepository businessPartnerRepository,
                       CurrencyRepository currencyRepository,
                       AccountSubjectRepository accountSubjectRepository,
                       JournalEntryRepository journalEntryRepository,
                       JournalDetailRepository journalDetailRepository) {
        this.loanRepository = loanRepository;
        this.loanDisbursalRepository = loanDisbursalRepository;
        this.loanEventRepository = loanEventRepository;
        this.deferredItemTypeRepository = deferredItemTypeRepository;
        this.deferredItemRepository = deferredItemRepository;
        this.eirAmortizationScheduleRepository = eirAmortizationScheduleRepository;
        this.recalculationRunRepository = recalculationRunRepository;
        this.eirCalculator = eirCalculator;
        this.businessPartnerRepository = businessPartnerRepository;
        this.currencyRepository = currencyRepository;
        this.accountSubjectRepository = accountSubjectRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalDetailRepository = journalDetailRepository;
    }

    // --- Loan (대출) 관련 메서드 ---

    /**
     * 새로운 대출을 생성하고 초기 유효이자율(EIR)을 계산합니다.
     * @param loan 생성할 대출 엔티티
     * @return 생성된 대출
     */
    public Loan createLoan(Loan loan) {
        // BusinessPartner, Currency 엔티티 연결
        BusinessPartner bp = businessPartnerRepository.findById(loan.getBusinessPartner().getId())
                .orElseThrow(() -> new EntityNotFoundException("BusinessPartner not found with id: " + loan.getBusinessPartner().getId()));
        Currency currency = currencyRepository.findById(loan.getCurrency().getCurrencyCode())
                .orElseThrow(() -> new EntityNotFoundException("Currency not found with code: " + loan.getCurrency().getCurrencyCode()));
        loan.setBusinessPartner(bp);
        loan.setCurrency(currency);

        // 초기 EIR 계산 (이연 항목이 아직 없을 수 있으므로 명목 이자율로 초기화할 수도 있음)
        // 만약 대출 생성 시 이미 이연 수수료 정보가 포함되어 있다면 여기서 계산 가능
        loan.setInitialEIR(loan.getInterestRate());
        
        loan.setCurrentEIR(loan.getInitialEIR());
        loan.setStatus(LoanStatus.ACTIVE);
        return loanRepository.save(loan);
    }

    /**
     * ID로 대출을 조회합니다.
     * @param id 대출 ID
     * @return 조회된 대출
     * @throws EntityNotFoundException 해당 ID의 대출이 없을 경우
     */
    @Transactional(readOnly = true)
    public Loan findLoanById(Long id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Loan not found with id: " + id));
    }

    // --- LoanDisbursal (대출 실행) 관련 메서드 ---

    /**
     * 대출을 실행하고 관련 분개 전표를 생성합니다.
     * @param loanId 실행할 대출 ID
     * @param disbursalDate 실행일
     * @param disbursedAmount 실행 금액
     * @param user 실행자
     * @return 생성된 대출 실행 기록
     */
    public LoanDisbursal disburseLoan(Long loanId, LocalDate disbursalDate, BigDecimal disbursedAmount, String user) {
        Loan loan = findLoanById(loanId);
        // TODO: 실행 금액이 약정 원금을 초과하지 않는지 등 유효성 검사

        LoanDisbursal disbursal = new LoanDisbursal();
        disbursal.setLoan(loan);
        disbursal.setDisbursalDate(disbursalDate);
        disbursal.setDisbursedAmount(disbursedAmount);
        disbursal.setAuditUser(user);

        // 대출 실행 분개 생성 (예: 현금/예금 감소, 대출채권 증가)
        // 계정과목은 DeferredItemType에서 매핑된 계정을 사용하거나, 시스템 설정에서 가져옴
        AccountSubject cashAccount = accountSubjectRepository.findById("101000") // 예: 보통예금
                .orElseThrow(() -> new EntityNotFoundException("Cash AccountSubject (101000) not found."));
        AccountSubject loanReceivableAccount = accountSubjectRepository.findById("131000") // 예: 대출채권
                .orElseThrow(() -> new EntityNotFoundException("Loan Receivable AccountSubject (131000) not found."));

        JournalEntry disbursalJe = createAutomatedJournalEntry(
                disbursalDate,
                loan.getLoanNumber() + " 대출 실행",
                user,
                "LOAN_DISBURSAL",
                loanId.toString(),
                disbursedAmount,
                cashAccount, // 대변
                loanReceivableAccount // 차변
        );
        disbursal.setJournalEntry(disbursalJe);

        return loanDisbursalRepository.save(disbursal);
    }

    // --- DeferredItemType (이연 항목 유형) 관련 메서드 ---

    /**
     * 새로운 이연 항목 유형을 생성합니다.
     * @param deferredItemType 생성할 이연 항목 유형 엔티티
     * @return 생성된 이연 항목 유형
     */
    public DeferredItemType createDeferredItemType(DeferredItemType deferredItemType) {
        // AccountSubject 연결
        AccountSubject assetAcc = accountSubjectRepository.findById(deferredItemType.getDeferredAssetAccount().getCode())
                .orElseThrow(() -> new EntityNotFoundException("Deferred Asset AccountSubject not found."));
        AccountSubject incomeAcc = accountSubjectRepository.findById(deferredItemType.getRecognizedIncomeAccount().getCode())
                .orElseThrow(() -> new EntityNotFoundException("Recognized Income AccountSubject not found."));
        deferredItemType.setDeferredAssetAccount(assetAcc);
        deferredItemType.setRecognizedIncomeAccount(incomeAcc);
        return deferredItemTypeRepository.save(deferredItemType);
    }

    /**
     * 코드로 이연 항목 유형을 조회합니다.
     * @param code 이연 항목 유형 코드
     * @return 조회된 이연 항목 유형
     * @throws EntityNotFoundException 해당 코드의 이연 항목 유형이 없을 경우
     */
    @Transactional(readOnly = true)
    public DeferredItemType findDeferredItemTypeByCode(String code) {
        return deferredItemTypeRepository.findByCode(code)
                .orElseThrow(() -> new EntityNotFoundException("DeferredItemType not found with code: " + code));
    }

    // --- DeferredItem (이연 항목) 관련 메서드 ---

    /**
     * 대출에 대한 이연 항목을 생성하고 초기 분개 전표를 발행합니다.
     * @param loanId 대출 ID
     * @param itemTypeId 이연 항목 유형 ID
     * @param amount 이연 총 금액
     * @param deferralDate 이연 발생일
     * @param amortizationEndDate 상각 종료일
     * @param user 생성자
     * @return 생성된 이연 항목
     */
    public DeferredItem createDeferredItem(Long loanId, Long itemTypeId, BigDecimal amount, LocalDate deferralDate, LocalDate amortizationEndDate, String user) {
        Loan loan = findLoanById(loanId);
        DeferredItemType itemType = deferredItemTypeRepository.findById(itemTypeId)
                .orElseThrow(() -> new EntityNotFoundException("DeferredItemType not found with id: " + itemTypeId));

        DeferredItem deferredItem = new DeferredItem();
        deferredItem.setLoan(loan);
        deferredItem.setDeferredItemType(itemType);
        deferredItem.setAmount(amount);
        deferredItem.setRemainingAmount(amount);
        deferredItem.setDeferralDate(deferralDate);
        deferredItem.setAmortizationStartDate(deferralDate); // 이연 발생일부터 상각 시작
        deferredItem.setAmortizationEndDate(amortizationEndDate);
        deferredItem.setAuditUser(user);

        // 이연 처리 분개 생성 (예: 현금 감소, 이연대출부대손익 자산 증가)
        JournalEntry initialJe = createAutomatedJournalEntry(
                deferralDate,
                loan.getLoanNumber() + " 이연 " + itemType.getName() + " 발생",
                user,
                "DEFERRED_ITEM_INIT",
                deferredItem.getId() != null ? deferredItem.getId().toString() : "NEW", // ID가 아직 없을 수 있으므로 "NEW" 사용
                amount,
                accountSubjectRepository.findById("101000").orElseThrow(), // 대변
                itemType.getDeferredAssetAccount() // 차변
        );
        deferredItem.setInitialJournalEntry(initialJe);

        return deferredItemRepository.save(deferredItem);
    }

    // --- EIR Amortization Schedule (EIR 상각 스케줄) 관련 메서드 ---

    /**
     * 대출의 EIR 상각 스케줄을 생성합니다. (최초 실행 시 또는 재계산 시)
     * @param loanId 스케줄을 생성할 대출 ID
     * @param recalculationDate 재계산일 (최초 생성 시에는 대출 실행일)
     * @param eirPercent 적용할 유효이자율 (연율, %)
     * @param user 생성자
     * @return 생성된 상각 스케줄 목록
     */
    public List<EIRAmortizationSchedule> generateAmortizationSchedule(Long loanId, LocalDate recalculationDate, BigDecimal eirPercent, String user) {
        Loan loan = findLoanById(loanId);
        List<DeferredItem> deferredItems = deferredItemRepository.findByLoan(loan);
        
        // EIR (연율, %) -> 월 이자율
        BigDecimal monthlyEir = eirPercent.divide(BigDecimal.valueOf(1200), 10, RoundingMode.HALF_UP);
        BigDecimal monthlyNominalRate = loan.getInterestRate().divide(BigDecimal.valueOf(12), 10, RoundingMode.HALF_UP);

        BigDecimal outstandingPrincipal = loan.getPrincipalAmount();
        BigDecimal outstandingDeferred = deferredItems.stream()
                .filter(item -> item.getStatus() != DeferredItem.DeferredItemStatus.CANCELLED)
                .map(DeferredItem::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 총 잔여 회차 계산
        long remainingMonths = java.time.temporal.ChronoUnit.MONTHS.between(recalculationDate, loan.getMaturityDate());
        if (remainingMonths <= 0) remainingMonths = 1;

        // 기존 스케줄 비활성화 (재계산 시)
        List<EIRAmortizationSchedule> oldSchedules = eirAmortizationScheduleRepository.findByLoan(loan);
        oldSchedules.stream()
                .filter(s -> !s.getScheduleDate().isBefore(recalculationDate))
                .forEach(s -> {
                    s.setRecalculated(true);
                    eirAmortizationScheduleRepository.save(s);
                });

        List<EIRAmortizationSchedule> schedule = new java.util.ArrayList<>();
        LocalDate currentDate = recalculationDate;

        for (int i = 1; i <= remainingMonths; i++) {
            BigDecimal beginningCarryingAmount = outstandingPrincipal.add(outstandingDeferred);
            
            // 1. 이자 수익 = 기초 장부가액 * EIR (월)
            BigDecimal interestIncome = beginningCarryingAmount.multiply(monthlyEir).setScale(2, RoundingMode.HALF_UP);
            
            // 2. 명목 이자 = 기초 원금 * 명목 이자율 (월)
            BigDecimal nominalInterest = outstandingPrincipal.multiply(monthlyNominalRate).setScale(2, RoundingMode.HALF_UP);
            
            // 3. 이연 상각액 = 이자 수익 - 명목 이자
            BigDecimal deferredAmortization = interestIncome.subtract(nominalInterest).setScale(2, RoundingMode.HALF_UP);
            
            // 4. 원금 상환 (원리금 균등 PMT 가정 - 단순화)
            double p = loan.getPrincipalAmount().doubleValue();
            double r = monthlyNominalRate.doubleValue();
            long totalN = java.time.temporal.ChronoUnit.MONTHS.between(loan.getDisbursalDate(), loan.getMaturityDate());
            if (totalN <= 0) totalN = 1;
            double pmtValue = (p * r) / (1 - Math.pow(1 + r, -totalN));
            BigDecimal totalPayment = BigDecimal.valueOf(pmtValue).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalRepayment = totalPayment.subtract(nominalInterest).setScale(2, RoundingMode.HALF_UP);

            if (i == remainingMonths) {
                principalRepayment = outstandingPrincipal; // 마지막 회차 잔액 조정
            }

            EIRAmortizationSchedule entry = new EIRAmortizationSchedule();
            entry.setLoan(loan);
            entry.setScheduleDate(currentDate.plusMonths(i));
            entry.setBeginningBalance(beginningCarryingAmount);
            entry.setInterestIncome(interestIncome);
            entry.setPrincipalRepayment(principalRepayment);
            entry.setDeferredItemAmortization(deferredAmortization);
            entry.setCashFlow(principalRepayment.add(nominalInterest));
            
            outstandingPrincipal = outstandingPrincipal.subtract(principalRepayment);
            outstandingDeferred = outstandingDeferred.subtract(deferredAmortization);
            entry.setEndingBalance(outstandingPrincipal.add(outstandingDeferred));
            entry.setAuditUser(user);

            // 상각 분개 생성
            AccountSubject loanReceivableAccount = accountSubjectRepository.findById("131000").orElseThrow();
            AccountSubject interestIncomeAccount = accountSubjectRepository.findById("401000").orElseThrow();
            AccountSubject deferredAssetAccount = accountSubjectRepository.findById("171000").orElseThrow();

            // 원금/이자 회수 분개
            JournalEntry amortizationJe = createAutomatedJournalEntry(
                    entry.getScheduleDate(),
                    loan.getLoanNumber() + " " + i + "차 원리금 회수 및 이자수익",
                    user,
                    "EIR_AMORTIZATION",
                    loan.getId().toString(),
                    entry.getCashFlow(),
                    loanReceivableAccount, // 대변: 원금 감소 (단순화)
                    interestIncomeAccount // 차변: 이자 수익 (실제로는 현금 차변, 이자수익 대변 등 복잡함)
            );
            entry.setAmortizationJournalEntry(amortizationJe);

            schedule.add(entry);
            eirAmortizationScheduleRepository.save(entry);
        }
        
        // DeferredItem 잔액 업데이트 (단순화: 첫 번째 항목에 모두 반영)
        if (!deferredItems.isEmpty()) {
            DeferredItem firstItem = deferredItems.get(0);
            firstItem.setRemainingAmount(outstandingDeferred);
            if (outstandingDeferred.compareTo(BigDecimal.ZERO) <= 0) {
                firstItem.setStatus(DeferredItem.DeferredItemStatus.FULLY_AMORTIZED);
            } else {
                firstItem.setStatus(DeferredItem.DeferredItemStatus.AMORTIZING);
            }
            deferredItemRepository.save(firstItem);
        }

        loan.setCurrentEIR(eirPercent);
        loanRepository.save(loan);
        return schedule;
    }

    // --- Recalculation (재계산) 관련 메서드 ---

    /**
     * 중도상환 또는 조건 변경으로 인해 대출의 EIR을 재계산하고 스케줄을 재조정합니다.
     * @param loanId 재계산할 대출 ID
     * @param eventDate 재계산 트리거 이벤트 발생일
     * @param reason 재계산 사유
     * @param user 실행자
     * @param newPrincipal (선택적) 변경된 원금
     * @param newMaturityDate (선택적) 변경된 만기일
     * @return 생성된 재계산 실행 기록
     */
    public RecalculationRun recalculateLoan(Long loanId, LocalDate eventDate, RecalculationReason reason, String user,
                                            Optional<BigDecimal> newPrincipal, Optional<LocalDate> newMaturityDate) {
        Loan loan = findLoanById(loanId);

        RecalculationRun run = new RecalculationRun();
        run.setLoan(loan);
        run.setRecalculationDate(eventDate);
        run.setReason(reason);
        run.setOldEIR(loan.getCurrentEIR());
        run.setOldMaturityDate(loan.getMaturityDate());
        run.setAuditUser(user);

        // 원금 또는 만기일 업데이트
        newPrincipal.ifPresent(loan::setPrincipalAmount);
        newMaturityDate.ifPresent(loan::setMaturityDate);

        // 새로운 EIR 계산
        List<DeferredItem> deferredItems = deferredItemRepository.findByLoan(loan);
        BigDecimal recalculatedEIR = eirCalculator.calculateEIR(loan, deferredItems);
        
        run.setNewEIR(recalculatedEIR);
        run.setNewMaturityDate(loan.getMaturityDate());

        // 새로운 스케줄 생성
        List<EIRAmortizationSchedule> newSchedule = generateAmortizationSchedule(loanId, eventDate, recalculatedEIR, user);
        run.setRecalculatedAmortizationScheduleStart(newSchedule.isEmpty() ? null : newSchedule.get(0));

        // 재계산으로 인한 조정 분개 (단순화: 중도상환 시 차익/차손 등)
        if (reason == RecalculationReason.EARLY_REPAYMENT && newPrincipal.isPresent()) {
            BigDecimal repaymentAmount = run.getLoan().getPrincipalAmount().subtract(newPrincipal.get());
            AccountSubject cashAccount = accountSubjectRepository.findById("101000").orElseThrow();
            AccountSubject loanReceivableAccount = accountSubjectRepository.findById("131000").orElseThrow();

            JournalEntry adjJe = createAutomatedJournalEntry(
                    eventDate,
                    loan.getLoanNumber() + " 중도상환 원금 회수",
                    user,
                    "EARLY_REPAYMENT",
                    loan.getId().toString(),
                    repaymentAmount,
                    loanReceivableAccount, // 대변
                    cashAccount // 차변
            );
            run.setAdjustmentJournalEntry(adjJe);
        }

        loan.setCurrentEIR(recalculatedEIR);
        loanRepository.save(loan); // 대출 정보 업데이트

        return recalculationRunRepository.save(run);
    }

    // --- DoD 구현: 실행 -> 이연 -> 3개월 상각 -> 중도상환 재계산까지 재현 ---
    /**
     * DoD 시나리오를 재현하는 메서드: 대출 실행 -> 이연 항목 생성 -> 3개월 상각 -> 중도상환 재계산
     * @param loanId 대출 ID
     * @param user 실행자
     * @return 최종 RecalculationRun
     */
    public RecalculationRun reproduceDoDScenario(Long loanId, String user) {
        Loan loan = findLoanById(loanId);

        LocalDate initialDate = loan.getDisbursalDate();

        // 1. 이연 항목 생성
        DeferredItemType itemType = deferredItemTypeRepository.findByCode("LOAN_ORIGINATION_FEE")
                .orElseGet(() -> createDefaultDeferredItemType()); // 기본 이연 항목 유형 생성
        createDeferredItem(loanId, itemType.getId(), BigDecimal.valueOf(1000), initialDate, loan.getMaturityDate(), user);

        // 2. 최초 EIR 상각 스케줄 생성 (이연 항목 포함)
        generateAmortizationSchedule(loanId, initialDate, loan.getInitialEIR(), user);

        // 3. 3개월 후 중도상환 이벤트 발생 및 재계산
        LocalDate earlyRepaymentDate = initialDate.plusMonths(3);
        LoanEvent earlyRepaymentEvent = new LoanEvent();
        earlyRepaymentEvent.setLoan(loan);
        earlyRepaymentEvent.setEventType(EventType.EARLY_REPAYMENT);
        earlyRepaymentEvent.setEventDate(earlyRepaymentDate);
        earlyRepaymentEvent.setDescription("3개월 후 중도상환 발생");
        earlyRepaymentEvent.setAuditUser(user);
        loanEventRepository.save(earlyRepaymentEvent);

        // 중도상환으로 인한 재계산 트리거
        RecalculationRun recalculationRun = recalculateLoan(loanId, earlyRepaymentDate, RecalculationReason.EARLY_REPAYMENT, user,
                Optional.of(loan.getPrincipalAmount().subtract(BigDecimal.valueOf(5000))), Optional.empty()); // 임의의 원금 감소

        // TODO: 중도상환 금액에 대한 JournalEntry 생성 및 LoanEvent와 연결

        return recalculationRun;
    }

    // --- Helper Methods ---

    /**
     * 자동 생성되는 분개 전표를 생성합니다. (대출 실행, 상각 등에서 사용)
     * @param accountingDate 회계일자
     * @param description 적요
     * @param createdBy 생성자
     * @param lineageSourceType 원천 시스템 유형
     * @param lineageSourceId 원천 시스템 ID
     * @param amount 분개 금액
     * @param creditAccount 대변 계정과목
     * @param debitAccount 차변 계정과목
     * @return 생성된 JournalEntry (저장된 상태)
     */
    private JournalEntry createAutomatedJournalEntry(LocalDate accountingDate, String description, String createdBy,
                                                      String lineageSourceType, String lineageSourceId,
                                                      BigDecimal amount, AccountSubject creditAccount, AccountSubject debitAccount) {
        JournalEntry entry = new JournalEntry();
        entry.setSlipDate(LocalDate.now());
        entry.setAccountingDate(accountingDate);
        entry.setDescription(description);
        entry.setStatus(JournalEntryStatus.DRAFT);
        entry.setEntryType("NORMAL"); // 대출 관련 분개는 NORMAL로 간주
        entry.setCreatedBy(createdBy);
        entry.setAuditUser(createdBy);
        entry.setLineageSourceType(lineageSourceType);
        entry.setLineageSourceId(lineageSourceId);

        // JournalDetail - 차변
        JournalDetail debitDetail = new JournalDetail();
        debitDetail.setDrcrType("DEBIT");
        debitDetail.setAccountSubject(debitAccount);
        debitDetail.setAmount(amount);
        debitDetail.setBaseAmount(amount);
        debitDetail.setDetailDescription(description + " (차변)");
        entry.addDetail(debitDetail);

        // JournalDetail - 대변
        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setDrcrType("CREDIT");
        creditDetail.setAccountSubject(creditAccount);
        creditDetail.setAmount(amount);
        creditDetail.setBaseAmount(amount);
        creditDetail.setDetailDescription(description + " (대변)");
        entry.addDetail(creditDetail);

        entry.setSlipNo(accountingDate.format(java.time.format.DateTimeFormatter.BASIC_ISO_DATE) + "-LOAN-" + journalEntryRepository.count());
        return journalEntryRepository.save(entry);
    }

    /**
     * 기본 이연 항목 유형을 생성하는 헬퍼 메서드 (초기 데이터 로딩 시 사용 가능)
     * @return 생성된 기본 이연 항목 유형
     */
    private DeferredItemType createDefaultDeferredItemType() {
        DeferredItemType itemType = new DeferredItemType();
        itemType.setCode("LOAN_ORIGINATION_FEE");
        itemType.setName("대출 실행 수수료");
        itemType.setDescription("대출 실행 시 발생하는 수수료 이연");
        itemType.setDeferralMethod(DeferralMethod.EIR_METHOD);

        // TODO: 실제 계정과목 코드 필요
        itemType.setDeferredAssetAccount(accountSubjectRepository.findById("171000").orElseThrow(
                () -> new EntityNotFoundException("Default Deferred Asset AccountSubject (171000) not found."))); // 예: 이연대출부대손익
        itemType.setRecognizedIncomeAccount(accountSubjectRepository.findById("401000").orElseThrow(
                () -> new EntityNotFoundException("Default Recognized Income AccountSubject (401000) not found."))); // 예: 이자수익
        itemType.setActive(true);
        return deferredItemTypeRepository.save(itemType);
    }
}
