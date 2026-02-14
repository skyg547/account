package com.ho.account.asset.service;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseLiability;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import com.ho.account.asset.domain.RightOfUseAsset;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.asset.repository.LeaseLiabilityRepository;
import com.ho.account.asset.repository.LeasePaymentScheduleRepository;
import com.ho.account.asset.repository.RightOfUseAssetRepository;
import com.ho.account.journal.service.JournalService; // JournalService로 변경
import com.ho.account.basic.repository.AccountSubjectRepository; // 계정과목 리포지토리 추가
import com.ho.account.basic.repository.DepartmentRepository; // 부서 리포지토리 추가
import com.ho.account.basic.repository.BusinessPartnerRepository; // 거래처 리포지토리 추가
import com.ho.account.journal.domain.JournalEntry; // JournalEntry 추가
import com.ho.account.journal.domain.JournalDetail; // JournalDetail 추가
import com.ho.account.journal.domain.JournalEntryStatus; // JournalEntryStatus 추가
import com.ho.account.basic.domain.AccountSubject; // AccountSubject 추가
import com.ho.account.basic.domain.Department; // Department 추가
import com.ho.account.basic.domain.BusinessPartner; // BusinessPartner 추가

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * IFRS 16 리스 회계 처리를 위한 서비스 클래스.
 * 리스 계약의 초기 인식, 후속 측정(월별 상각 및 이자 계산), 변경/재측정 로직을 포함합니다.
 */
@Service
public class LeaseAccountingService {

    private final LeaseContractRepository leaseContractRepository;
    private final RightOfUseAssetRepository rightOfUseAssetRepository;
    private final LeaseLiabilityRepository leaseLiabilityRepository;
    private final LeasePaymentScheduleRepository leasePaymentScheduleRepository;
    private final JournalService journalService; // JournalService로 변경

    // 전표 생성을 위한 추가 리포지토리 주입
    private final AccountSubjectRepository accountSubjectRepository;
    private final DepartmentRepository departmentRepository;
    private final BusinessPartnerRepository businessPartnerRepository;

    public LeaseAccountingService(LeaseContractRepository leaseContractRepository,
                                  RightOfUseAssetRepository rightOfUseAssetRepository,
                                  LeaseLiabilityRepository leaseLiabilityRepository,
                                  LeasePaymentScheduleRepository leasePaymentScheduleRepository,
                                  JournalService journalService, // JournalService로 변경
                                  AccountSubjectRepository accountSubjectRepository,
                                  DepartmentRepository departmentRepository,
                                  BusinessPartnerRepository businessPartnerRepository) {
        this.leaseContractRepository = leaseContractRepository;
        this.rightOfUseAssetRepository = rightOfUseAssetRepository;
        this.leaseLiabilityRepository = leaseLiabilityRepository;
        this.leasePaymentScheduleRepository = leasePaymentScheduleRepository;
        this.journalService = journalService; // JournalService로 변경
        this.accountSubjectRepository = accountSubjectRepository;
        this.departmentRepository = departmentRepository;
        this.businessPartnerRepository = businessPartnerRepository;
    }

    /**
     * IFRS 16 리스 계약의 초기 인식을 처리합니다.
     * 사용권자산과 리스부채를 인식하고, 리스 상환 스케줄을 생성합니다.
     *
     * @param contract 초기 인식할 리스 계약 엔티티
     * @return 초기 인식된 리스 계약 엔티티
     */
    @Transactional
    public LeaseContract recognizeInitialLease(LeaseContract contract) {
        if (!contract.isIfrs16Applicable()) {
            throw new IllegalArgumentException("This lease contract is not applicable for IFRS 16 recognition.");
        }
        if (contract.getInitialRightOfUseAssetValue() == null || contract.getInitialLeaseLiabilityValue() == null || contract.getDiscountRate() == null) {
            throw new IllegalArgumentException("Initial Right-of-Use Asset Value, Lease Liability Value, and Discount Rate must be provided for IFRS 16 recognition.");
        }

        // 1. 사용권자산 인식
        RightOfUseAsset rouAsset = new RightOfUseAsset();
        rouAsset.setLeaseContract(contract);
        rouAsset.setAssetName(contract.getContractName() + " - 사용권자산");
        rouAsset.setRecognitionDate(contract.getStartDate());
        rouAsset.setInitialValue(contract.getInitialRightOfUseAssetValue());
        rouAsset.setCurrentBookValue(contract.getInitialRightOfUseAssetValue());
        rouAsset.setAccumulatedDepreciation(BigDecimal.ZERO);
        // 월별 상각액 계산 (계약 종료일까지 정액법 상각 가정)
        long totalMonths = contract.getStartDate().until(contract.getEndDate()).toTotalMonths();
        if (totalMonths > 0) {
            rouAsset.setDepreciationAmountPerPeriod(contract.getInitialRightOfUseAssetValue().divide(new BigDecimal(totalMonths), 2, RoundingMode.HALF_UP));
        } else {
            rouAsset.setDepreciationAmountPerPeriod(BigDecimal.ZERO);
        }
        rightOfUseAssetRepository.save(rouAsset);

        // 2. 리스부채 인식
        LeaseLiability leaseLiability = new LeaseLiability();
        leaseLiability.setLeaseContract(contract);
        leaseLiability.setRecognitionDate(contract.getStartDate());
        leaseLiability.setInitialValue(contract.getInitialLeaseLiabilityValue());
        leaseLiability.setCurrentValue(contract.getInitialLeaseLiabilityValue());
        leaseLiability.setAccumulatedInterestExpense(BigDecimal.ZERO);
        leaseLiabilityRepository.save(leaseLiability);

        // 3. 리스 상환 스케줄 생성
        generateLeasePaymentSchedule(contract, leaseLiability);

        // 4. 초기 인식 분개 생성 (사용권자산 및 리스부채)
        // 계정과목 조회 (예시 코드, 실제로는 코드 또는 설정을 통해 조회)
        AccountSubject rouAssetAccount = accountSubjectRepository.findById("12300") // 예: 사용권자산 계정 코드
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Right-of-Use Asset not found"));
        AccountSubject leaseLiabilityAccount = accountSubjectRepository.findById("25100") // 예: 리스부채 계정 코드
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Lease Liability not found"));

        JournalEntry initialRecognitionEntry = new JournalEntry();
        initialRecognitionEntry.setSlipDate(contract.getStartDate());
        initialRecognitionEntry.setAccountingDate(contract.getStartDate());
        initialRecognitionEntry.setDescription("IFRS 16 리스 초기 인식 - " + contract.getContractName() + " (" + contract.getContractNo() + ")");
        initialRecognitionEntry.setEntryType("IFRS16_INITIAL"); // IFRS 16 전용 타입
        initialRecognitionEntry.setLineageSourceType("IFRS16_LEASE");
        initialRecognitionEntry.setLineageSourceId(contract.getId().toString());
        initialRecognitionEntry.setCreatedBy("SYSTEM");
        initialRecognitionEntry.setStatus(JournalEntryStatus.DRAFT); // 초기에는 DRAFT 상태로 생성

        // 차변: 사용권자산
        JournalDetail debitDetail = new JournalDetail();
        debitDetail.setDrcrType("DEBIT");
        debitDetail.setAccountSubject(rouAssetAccount);
        debitDetail.setAmount(contract.getInitialRightOfUseAssetValue());
        debitDetail.setDepartment(contract.getDepartment()); // 계약에 연결된 부서 사용
        debitDetail.setDetailDescription("IFRS 16 사용권자산 인식");
        initialRecognitionEntry.addDetail(debitDetail);

        // 대변: 리스부채
        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setDrcrType("CREDIT");
        creditDetail.setAccountSubject(leaseLiabilityAccount);
        creditDetail.setAmount(contract.getInitialLeaseLiabilityValue());
        creditDetail.setDepartment(contract.getDepartment()); // 계약에 연결된 부서 사용
        creditDetail.setDetailDescription("IFRS 16 리스부채 인식");
        initialRecognitionEntry.addDetail(creditDetail);

        journalService.createJournalEntry(initialRecognitionEntry);

        return contract;
    }

    /**
     * 리스 상환 스케줄을 생성합니다. (월별 원리금균등상환 가정)
     * 실제 구현에서는 리스료 납입 스케줄, 이자율 등을 고려하여 복잡한 계산이 필요합니다.
     * 여기서는 단순화된 월별 균등 상환 스케줄을 생성합니다.
     *
     * @param contract 리스 계약 엔티티
     * @param leaseLiability 인식된 리스부채 엔티티
     */
    private void generateLeasePaymentSchedule(LeaseContract contract, LeaseLiability leaseLiability) {
        List<LeasePaymentSchedule> schedules = new ArrayList<>();
        LocalDate currentDate = contract.getStartDate();
        BigDecimal remainingLiability = leaseLiability.getInitialValue();
        BigDecimal monthlyPayment = contract.getMonthlyPayment();
        BigDecimal monthlyDiscountRate = contract.getDiscountRate().divide(new BigDecimal("1200"), 6, RoundingMode.HALF_UP); // 연이율을 월이율로 변환 (예: 5% -> 0.05/12)

        while (currentDate.isBefore(contract.getEndDate()) || currentDate.isEqual(contract.getEndDate())) {
            LeasePaymentSchedule schedule = new LeasePaymentSchedule();
            schedule.setLeaseContract(contract);
            schedule.setPaymentDate(currentDate);
            schedule.setScheduledPaymentAmount(monthlyPayment);

            BigDecimal interestPortion = remainingLiability.multiply(monthlyDiscountRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPortion = monthlyPayment.subtract(interestPortion);

            // 원금이 남았지만 월 납입액이 부족한 경우 마지막 회차 조정
            if (principalPortion.compareTo(remainingLiability) > 0) {
                principalPortion = remainingLiability;
                monthlyPayment = interestPortion.add(principalPortion); // Adjust monthly payment if needed
                schedule.setScheduledPaymentAmount(monthlyPayment);
            }

            remainingLiability = remainingLiability.subtract(principalPortion);
            if (remainingLiability.compareTo(BigDecimal.ZERO) < 0) {
                remainingLiability = BigDecimal.ZERO;
            }

            schedule.setInterestPortion(interestPortion);
            schedule.setPrincipalPortion(principalPortion);
            schedule.setRemainingLeaseLiability(remainingLiability);
            schedule.setStatus("SCHEDULED");
            schedules.add(schedule);

            currentDate = currentDate.plusMonths(1);
        }
        leasePaymentScheduleRepository.saveAll(schedules);
    }

    /**
     * 리스 계약의 조건을 변경하고 리스부채 및 사용권자산을 재측정합니다.
     * 새로운 리스료, 리스 종료일, 할인율 등을 반영하여 잔여 리스부채를 재계산하고,
     * 이에 따라 사용권자산의 장부가액을 조정합니다.
     *
     * @param contractId 재측정할 리스 계약의 ID
     * @param remeasurementDate 재측정 기준일
     * @param newMonthlyPayment 변경된 월 리스료 (nullable)
     * @param newEndDate 변경된 리스 종료일 (nullable)
     * @param newDiscountRate 변경된 할인율 (nullable)
     * @return 재측정된 리스 계약 엔티티
     */
    @Transactional
    public LeaseContract remeasureLease(Long contractId, LocalDate remeasurementDate,
                                        BigDecimal newMonthlyPayment, LocalDate newEndDate,
                                        BigDecimal newDiscountRate) {
        LeaseContract contract = leaseContractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("LeaseContract not found with ID: " + contractId));

        if (!contract.isIfrs16Applicable()) {
            throw new IllegalArgumentException("This lease contract is not applicable for IFRS 16 remeasurement.");
        }

        RightOfUseAsset rouAsset = rightOfUseAssetRepository.findByLeaseContract(contract)
                .orElseThrow(() -> new IllegalStateException("Right-of-Use Asset not found for contract: " + contract.getContractNo()));
        LeaseLiability leaseLiability = leaseLiabilityRepository.findByLeaseContract(contract)
                .orElseThrow(() -> new IllegalStateException("Lease Liability not found for contract: " + contract.getContractNo()));

        // 기존 리스 스케줄 중 재측정일 이후 스케줄 삭제 또는 취소
        List<LeasePaymentSchedule> futureSchedules = leasePaymentScheduleRepository
                .findByLeaseContractOrderByPaymentDateAsc(contract)
                .stream()
                .filter(s -> s.getPaymentDate().isAfter(remeasurementDate) || s.getPaymentDate().isEqual(remeasurementDate))
                .toList();
        leasePaymentScheduleRepository.deleteAll(futureSchedules);


        // 계약 조건 업데이트
        if (newMonthlyPayment != null) {
            contract.setMonthlyPayment(newMonthlyPayment);
        }
        if (newEndDate != null) {
            contract.setEndDate(newEndDate);
        }
        if (newDiscountRate != null) {
            contract.setDiscountRate(newDiscountRate);
        }
        LeaseContract updatedContract = leaseContractRepository.save(contract);

        // 새로운 미래 리스료 현재가치 계산 (새로운 리스부채)
        BigDecimal oldLeaseLiabilityValue = leaseLiability.getCurrentValue();
        BigDecimal newLeaseLiabilityValue = calculatePresentValueOfFuturePayments(
                updatedContract.getMonthlyPayment(),
                updatedContract.getEndDate(),
                remeasurementDate, // 재측정일 이후의 잔여 기간만 고려
                updatedContract.getDiscountRate()
        );

        leaseLiability.setCurrentValue(newLeaseLiabilityValue);
        leaseLiabilityRepository.save(leaseLiability);

        // 사용권자산 조정
        BigDecimal adjustmentAmount = newLeaseLiabilityValue.subtract(oldLeaseLiabilityValue);
        rouAsset.setCurrentBookValue(rouAsset.getCurrentBookValue().add(adjustmentAmount));
        rouAsset.setInitialValue(rouAsset.getInitialValue().add(adjustmentAmount)); // 최초 인식 가액도 조정 (IFRS16.B4.2.4(a) 또는 B4.2.5(a))
        // 상각액 재계산
        long remainingMonths = remeasurementDate.until(updatedContract.getEndDate()).toTotalMonths();
        if (remainingMonths > 0) {
            rouAsset.setDepreciationAmountPerPeriod(rouAsset.getCurrentBookValue().divide(new BigDecimal(remainingMonths), 2, RoundingMode.HALF_UP));
        } else {
            rouAsset.setDepreciationAmountPerPeriod(BigDecimal.ZERO);
        }
        rightOfUseAssetRepository.save(rouAsset);


        // 새로운 리스 상환 스케줄 재-생성
        generateLeasePaymentSchedule(updatedContract, leaseLiability);

        // 재측정 분개 생성
        AccountSubject rouAssetAccount = accountSubjectRepository.findById("12300") // 예: 사용권자산 계정 코드
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Right-of-Use Asset not found"));
        AccountSubject leaseLiabilityAccount = accountSubjectRepository.findById("25100") // 예: 리스부채 계정 코드
                .orElseThrow(() -> new IllegalStateException("AccountSubject for Lease Liability not found"));

        JournalEntry remeasurementEntry = new JournalEntry();
        remeasurementEntry.setSlipDate(remeasurementDate);
        remeasurementEntry.setAccountingDate(remeasurementDate);
        remeasurementEntry.setDescription("IFRS 16 리스 재측정: " + updatedContract.getContractName() + " (" + updatedContract.getContractNo() + ")");
        remeasurementEntry.setEntryType("IFRS16_REMEASUREMENT");
        remeasurementEntry.setLineageSourceType("IFRS16_LEASE");
        remeasurementEntry.setLineageSourceId(updatedContract.getId().toString());
        remeasurementEntry.setCreatedBy("SYSTEM");
        remeasurementEntry.setStatus(JournalEntryStatus.DRAFT);

        // 차변/대변 금액이 양수(+)이면 차변, 음수(-)이면 대변
        if (adjustmentAmount.compareTo(BigDecimal.ZERO) > 0) { // 자산/부채 증가
            // 차변: 사용권자산
            JournalDetail rouDebit = new JournalDetail();
            rouDebit.setDrcrType("DEBIT");
            rouDebit.setAccountSubject(rouAssetAccount);
            rouDebit.setAmount(adjustmentAmount);
            rouDebit.setDepartment(updatedContract.getDepartment());
            rouDebit.setDetailDescription("사용권자산 재측정 증가");
            remeasurementEntry.addDetail(rouDebit);

            // 대변: 리스부채
            JournalDetail llCredit = new JournalDetail();
            llCredit.setDrcrType("CREDIT");
            llCredit.setAccountSubject(leaseLiabilityAccount);
            llCredit.setAmount(adjustmentAmount);
            llCredit.setDepartment(updatedContract.getDepartment());
            llCredit.setDetailDescription("리스부채 재측정 증가");
            remeasurementEntry.addDetail(llCredit);
        } else if (adjustmentAmount.compareTo(BigDecimal.ZERO) < 0) { // 자산/부채 감소
            // 차변: 리스부채
            JournalDetail llDebit = new JournalDetail();
            llDebit.setDrcrType("DEBIT");
            llDebit.setAccountSubject(leaseLiabilityAccount);
            llDebit.setAmount(adjustmentAmount.abs());
            llDebit.setDepartment(updatedContract.getDepartment());
            llDebit.setDetailDescription("리스부채 재측정 감소");
            remeasurementEntry.addDetail(llDebit);

            // 대변: 사용권자산
            JournalDetail rouCredit = new JournalDetail();
            rouCredit.setDrcrType("CREDIT");
            rouCredit.setAccountSubject(rouAssetAccount);
            rouCredit.setAmount(adjustmentAmount.abs());
            rouCredit.setDepartment(updatedContract.getDepartment());
            rouCredit.setDetailDescription("사용권자산 재측정 감소");
            remeasurementEntry.addDetail(rouCredit);
        } else {
            // 조정 금액이 0인 경우, 분개 생성 안 함
            return updatedContract;
        }

        journalService.createJournalEntry(remeasurementEntry);

        return updatedContract;
    }

    /**
     * 특정 날짜 이후의 남은 리스료의 현재 가치를 계산합니다.
     * 복잡한 재무 계산이므로 단순화된 형태로 구현합니다.
     *
     * @param monthlyPayment 월 리스료
     * @param endDate 리스 종료일
     * @param calculationStartDate 계산 시작일 (재측정일)
     * @param annualDiscountRate 연간 할인율
     * @return 미래 리스료의 현재 가치
     */
    private BigDecimal calculatePresentValueOfFuturePayments(BigDecimal monthlyPayment, LocalDate endDate,
                                                           LocalDate calculationStartDate, BigDecimal annualDiscountRate) {
        BigDecimal totalPresentValue = BigDecimal.ZERO;
        BigDecimal monthlyDiscountRate = annualDiscountRate.divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP); // 연이율을 월이율로 변환 (예: 5% -> 0.05/12)

        LocalDate currentPaymentDate = calculationStartDate.plusMonths(1); // 재측정일 다음 달부터 시작
        int monthCount = 1;

        while (currentPaymentDate.isBefore(endDate) || currentPaymentDate.isEqual(endDate)) {
            // PV = PMT / (1 + r)^n
            BigDecimal discountFactor = BigDecimal.ONE.add(monthlyDiscountRate).pow(monthCount);
            BigDecimal presentValue = monthlyPayment.divide(discountFactor, 2, RoundingMode.HALF_UP);
            totalPresentValue = totalPresentValue.add(presentValue);

            currentPaymentDate = currentPaymentDate.plusMonths(1);
            monthCount++;
        }
        return totalPresentValue;
    }


    /**
     * 특정 날짜를 기준으로 월별 리스 회계 처리를 수행합니다.
     * IFRS 16 적용 리스 계약에 대해 사용권자산 상각, 리스부채 이자 계산 및 상환을 처리하고 분개를 생성합니다.
     *
     * @param processDate 회계 처리 기준일
     */
    @Transactional
    public void processMonthlyLeaseAccounting(LocalDate processDate) {
        // IFRS 16 적용 대상인 활성 리스 계약을 조회
        List<LeaseContract> ifrs16LeaseContracts = leaseContractRepository.findByIfrs16ApplicableTrueAndStatus("ACTIVE");

        for (LeaseContract contract : ifrs16LeaseContracts) {
            // 해당 월의 스케줄된 지급액이 있는지 확인
            LeasePaymentSchedule nextSchedule = leasePaymentScheduleRepository
                    .findByLeaseContractOrderByPaymentDateAsc(contract)
                    .stream()
                    .filter(s -> s.getPaymentDate().getYear() == processDate.getYear() &&
                                 s.getPaymentDate().getMonth() == processDate.getMonth())
                    .findFirst()
                    .orElse(null);

            if (nextSchedule == null || !nextSchedule.getStatus().equals("SCHEDULED")) {
                continue; // 해당 월에 처리할 스케줄이 없거나 이미 처리됨
            }

            RightOfUseAsset rouAsset = rightOfUseAssetRepository.findByLeaseContract(contract)
                    .orElseThrow(() -> new IllegalStateException("Right-of-Use Asset not found for contract: " + contract.getContractNo()));
            LeaseLiability leaseLiability = leaseLiabilityRepository.findByLeaseContract(contract)
                    .orElseThrow(() -> new IllegalStateException("Lease Liability not found for contract: " + contract.getContractNo()));

            // 1. 사용권자산 상각 처리
            BigDecimal depreciationAmount = rouAsset.getDepreciationAmountPerPeriod();
            rouAsset.setAccumulatedDepreciation(rouAsset.getAccumulatedDepreciation().add(depreciationAmount));
            rouAsset.setCurrentBookValue(rouAsset.getInitialValue().subtract(rouAsset.getAccumulatedDepreciation()));
            rightOfUseAssetRepository.save(rouAsset);

            // 2. 리스부채 이자 비용 및 원금 상환 처리
            BigDecimal interestPortion = nextSchedule.getInterestPortion();
            BigDecimal principalPortion = nextSchedule.getPrincipalPortion();
            BigDecimal totalPayment = nextSchedule.getScheduledPaymentAmount(); // 이자 + 원금

            leaseLiability.setAccumulatedInterestExpense(leaseLiability.getAccumulatedInterestExpense().add(interestPortion));
            leaseLiability.setCurrentValue(leaseLiability.getCurrentValue().subtract(principalPortion));
            leaseLiabilityRepository.save(leaseLiability);

            // 3. 상환 스케줄 업데이트
            nextSchedule.setActualPaymentAmount(totalPayment); // 실제 지급액은 예정 지급액과 동일하다고 가정
            nextSchedule.setStatus("PAID");
            leasePaymentScheduleRepository.save(nextSchedule);

            // 4. 월별 처리 분개 생성
            AccountSubject depreciationExpenseAccount = accountSubjectRepository.findById("51500") // 예: 감가상각비 계정 코드
                    .orElseThrow(() -> new IllegalStateException("AccountSubject for Depreciation Expense not found"));
            AccountSubject accumulatedDepreciationAccount = accountSubjectRepository.findById("12399") // 예: 사용권자산 감가상각누계액 계정 코드
                    .orElseThrow(() -> new IllegalStateException("AccountSubject for Accumulated Depreciation of ROU Asset not found"));
            AccountSubject interestExpenseAccount = accountSubjectRepository.findById("93100") // 예: 이자비용 계정 코드
                    .orElseThrow(() -> new IllegalStateException("AccountSubject for Interest Expense not found"));
            AccountSubject cashAccount = accountSubjectRepository.findById("10100") // 예: 현금 또는 예금 계정 코드
                    .orElseThrow(() -> new IllegalStateException("AccountSubject for Cash/Bank not found"));
            AccountSubject leaseLiabilityAccount = accountSubjectRepository.findById("25100") // 예: 리스부채 계정 코드
                    .orElseThrow(() -> new IllegalStateException("AccountSubject for Lease Liability not found"));

            // 4-1. 상각 분개: (차) 감가상각비 (대) 사용권자산 감가상각누계액
            JournalEntry depreciationEntry = new JournalEntry();
            depreciationEntry.setSlipDate(processDate);
            depreciationEntry.setAccountingDate(processDate);
            depreciationEntry.setDescription("IFRS 16 월별 리스 회계 처리 - 감가상각: " + contract.getContractName() + " (" + contract.getContractNo() + ")");
            depreciationEntry.setEntryType("IFRS16_MONTHLY");
            depreciationEntry.setLineageSourceType("IFRS16_LEASE");
            depreciationEntry.setLineageSourceId(contract.getId().toString());
            depreciationEntry.setCreatedBy("SYSTEM");
            depreciationEntry.setStatus(JournalEntryStatus.DRAFT);

            // 차변: 감가상각비
            JournalDetail depreciationDebit = new JournalDetail();
            depreciationDebit.setDrcrType("DEBIT");
            depreciationDebit.setAccountSubject(depreciationExpenseAccount);
            depreciationDebit.setAmount(depreciationAmount);
            depreciationDebit.setDepartment(contract.getDepartment());
            depreciationDebit.setDetailDescription("사용권자산 감가상각비");
            depreciationEntry.addDetail(depreciationDebit);

            // 대변: 사용권자산 감가상각누계액
            JournalDetail depreciationCredit = new JournalDetail();
            depreciationCredit.setDrcrType("CREDIT");
            depreciationCredit.setAccountSubject(accumulatedDepreciationAccount);
            depreciationCredit.setAmount(depreciationAmount);
            depreciationCredit.setDepartment(contract.getDepartment());
            depreciationCredit.setDetailDescription("사용권자산 감가상각누계액");
            depreciationEntry.addDetail(depreciationCredit);

            journalService.createJournalEntry(depreciationEntry);

            // 4-2. 이자 및 원금 상환 분개: (차) 이자비용, 리스부채 (대) 현금/예금
            JournalEntry paymentEntry = new JournalEntry();
            paymentEntry.setSlipDate(processDate);
            paymentEntry.setAccountingDate(processDate);
            paymentEntry.setDescription("IFRS 16 월별 리스 회계 처리 - 상환: " + contract.getContractName() + " (" + contract.getContractNo() + ")");
            paymentEntry.setEntryType("IFRS16_MONTHLY");
            paymentEntry.setLineageSourceType("IFRS16_LEASE");
            paymentEntry.setLineageSourceId(contract.getId().toString());
            paymentEntry.setCreatedBy("SYSTEM");
            paymentEntry.setStatus(JournalEntryStatus.DRAFT);

            // 차변: 이자비용
            JournalDetail interestDebit = new JournalDetail();
            interestDebit.setDrcrType("DEBIT");
            interestDebit.setAccountSubject(interestExpenseAccount);
            interestDebit.setAmount(interestPortion);
            interestDebit.setDepartment(contract.getDepartment());
            interestDebit.setDetailDescription("리스 이자비용");
            paymentEntry.addDetail(interestDebit);

            // 차변: 리스부채 (원금 상환분)
            JournalDetail principalDebit = new JournalDetail();
            principalDebit.setDrcrType("DEBIT");
            principalDebit.setAccountSubject(leaseLiabilityAccount);
            principalDebit.setAmount(principalPortion);
            principalDebit.setDepartment(contract.getDepartment());
            principalDebit.setDetailDescription("리스부채 원금 상환");
            paymentEntry.addDetail(principalDebit);

            // 대변: 현금/예금
            JournalDetail cashCredit = new JournalDetail();
            cashCredit.setDrcrType("CREDIT");
            cashCredit.setAccountSubject(cashAccount);
            cashCredit.setAmount(totalPayment); // 전체 월 납입액
            cashCredit.setDepartment(contract.getDepartment());
            cashCredit.setDetailDescription("리스료 지급");
            paymentEntry.addDetail(cashCredit);

            journalService.createJournalEntry(paymentEntry);
        }
    }
}
