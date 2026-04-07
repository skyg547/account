package com.ho.account.asset.service;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseLiability;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import com.ho.account.asset.domain.RightOfUseAsset;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.asset.repository.LeaseLiabilityRepository;
import com.ho.account.asset.repository.LeasePaymentScheduleRepository;
import com.ho.account.asset.repository.RightOfUseAssetRepository;
import com.ho.account.basic.domain.AccountSubject;
import com.ho.account.basic.repository.AccountSubjectRepository;
import com.ho.account.journal.domain.JournalDetail;
import com.ho.account.journal.domain.JournalEntry;
import com.ho.account.journal.domain.JournalEntryStatus;
import com.ho.account.journal.service.JournalService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaseAccountingService {

    private final LeaseContractRepository leaseContractRepository;
    private final RightOfUseAssetRepository rightOfUseAssetRepository;
    private final LeaseLiabilityRepository leaseLiabilityRepository;
    private final LeasePaymentScheduleRepository leasePaymentScheduleRepository;
    private final JournalService journalService;
    private final AccountSubjectRepository accountSubjectRepository;

    public LeaseAccountingService(LeaseContractRepository leaseContractRepository,
                                  RightOfUseAssetRepository rightOfUseAssetRepository,
                                  LeaseLiabilityRepository leaseLiabilityRepository,
                                  LeasePaymentScheduleRepository leasePaymentScheduleRepository,
                                  JournalService journalService,
                                  AccountSubjectRepository accountSubjectRepository) {
        this.leaseContractRepository = leaseContractRepository;
        this.rightOfUseAssetRepository = rightOfUseAssetRepository;
        this.leaseLiabilityRepository = leaseLiabilityRepository;
        this.leasePaymentScheduleRepository = leasePaymentScheduleRepository;
        this.journalService = journalService;
        this.accountSubjectRepository = accountSubjectRepository;
    }

    @Transactional
    public LeaseContract recognizeInitialLease(LeaseContract contract) {
        if (!contract.isIfrs16Applicable()) {
            throw new IllegalArgumentException("This lease contract is not applicable for IFRS 16 recognition.");
        }
        if (contract.getInitialRightOfUseAssetValue() == null
                || contract.getInitialLeaseLiabilityValue() == null
                || contract.getDiscountRate() == null) {
            throw new IllegalArgumentException(
                    "Initial Right-of-Use Asset Value, Lease Liability Value, and Discount Rate must be provided for IFRS 16 recognition.");
        }

        LeaseContract savedContract = leaseContractRepository.save(contract);

        RightOfUseAsset rouAsset = new RightOfUseAsset();
        rouAsset.setLeaseContract(savedContract);
        rouAsset.setAssetName(savedContract.getContractName() + " - ROU");
        rouAsset.setRecognitionDate(savedContract.getStartDate());
        rouAsset.setInitialValue(savedContract.getInitialRightOfUseAssetValue());
        rouAsset.setCurrentBookValue(savedContract.getInitialRightOfUseAssetValue());
        rouAsset.setAccumulatedDepreciation(BigDecimal.ZERO);
        long totalMonths = savedContract.getStartDate().until(savedContract.getEndDate()).toTotalMonths();
        rouAsset.setDepreciationAmountPerPeriod(totalMonths > 0
                ? savedContract.getInitialRightOfUseAssetValue().divide(new BigDecimal(totalMonths), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        rightOfUseAssetRepository.save(rouAsset);

        LeaseLiability leaseLiability = new LeaseLiability();
        leaseLiability.setLeaseContract(savedContract);
        leaseLiability.setRecognitionDate(savedContract.getStartDate());
        leaseLiability.setInitialValue(savedContract.getInitialLeaseLiabilityValue());
        leaseLiability.setCurrentValue(savedContract.getInitialLeaseLiabilityValue());
        leaseLiability.setAccumulatedInterestExpense(BigDecimal.ZERO);
        leaseLiabilityRepository.save(leaseLiability);

        generateLeasePaymentSchedule(savedContract, leaseLiability);

        AccountSubject rouAssetAccount = getRequiredAccount("12300", "Right-of-Use Asset");
        AccountSubject leaseLiabilityAccount = getRequiredAccount("25100", "Lease Liability");

        JournalEntry initialRecognitionEntry = new JournalEntry();
        LocalDate initialRecognitionDate = savedContract.getStartDate().minusDays(1);
        initialRecognitionEntry.setSlipDate(initialRecognitionDate);
        initialRecognitionEntry.setAccountingDate(initialRecognitionDate);
        initialRecognitionEntry.setDescription("IFRS 16 리스 초기 인식 - "
                + savedContract.getContractName() + " (" + savedContract.getContractNo() + ")");
        initialRecognitionEntry.setEntryType("IFRS16_INITIAL");
        initialRecognitionEntry.setLineageSourceType("IFRS16_LEASE");
        initialRecognitionEntry.setLineageSourceId(savedContract.getId().toString());
        initialRecognitionEntry.setCreatedBy("SYSTEM");
        initialRecognitionEntry.setStatus(JournalEntryStatus.DRAFT);

        JournalDetail debitDetail = new JournalDetail();
        debitDetail.setDrcrType("DEBIT");
        debitDetail.setAccountSubject(rouAssetAccount);
        debitDetail.setAmount(savedContract.getInitialRightOfUseAssetValue());
        debitDetail.setDepartment(savedContract.getDepartment());
        debitDetail.setDetailDescription("IFRS 16 사용권자산 인식");
        initialRecognitionEntry.addDetail(debitDetail);

        JournalDetail creditDetail = new JournalDetail();
        creditDetail.setDrcrType("CREDIT");
        creditDetail.setAccountSubject(leaseLiabilityAccount);
        creditDetail.setAmount(savedContract.getInitialLeaseLiabilityValue());
        creditDetail.setDepartment(savedContract.getDepartment());
        creditDetail.setDetailDescription("IFRS 16 리스부채 인식");
        initialRecognitionEntry.addDetail(creditDetail);

        journalService.createJournalEntry(initialRecognitionEntry);
        return savedContract;
    }

    private void generateLeasePaymentSchedule(LeaseContract contract, LeaseLiability leaseLiability) {
        List<LeasePaymentSchedule> schedules = new ArrayList<>();
        LocalDate currentDate = contract.getStartDate();
        BigDecimal remainingLiability = leaseLiability.getInitialValue();
        BigDecimal monthlyPayment = contract.getMonthlyPayment();
        BigDecimal monthlyDiscountRate = contract.getDiscountRate()
                .divide(new BigDecimal("1200"), 6, RoundingMode.HALF_UP);

        while (currentDate.isBefore(contract.getEndDate()) || currentDate.isEqual(contract.getEndDate())) {
            LeasePaymentSchedule schedule = new LeasePaymentSchedule();
            schedule.setLeaseContract(contract);
            schedule.setPaymentDate(currentDate);
            schedule.setScheduledPaymentAmount(monthlyPayment);

            BigDecimal interestPortion = remainingLiability.multiply(monthlyDiscountRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPortion = monthlyPayment.subtract(interestPortion);
            if (principalPortion.compareTo(remainingLiability) > 0) {
                principalPortion = remainingLiability;
                monthlyPayment = interestPortion.add(principalPortion);
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

    @Transactional
    public LeaseContract remeasureLease(Long contractId,
                                        LocalDate remeasurementDate,
                                        BigDecimal newMonthlyPayment,
                                        LocalDate newEndDate,
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

        List<LeasePaymentSchedule> futureSchedules = leasePaymentScheduleRepository
                .findByLeaseContractOrderByPaymentDateAsc(contract)
                .stream()
                .filter(schedule -> !schedule.getPaymentDate().isBefore(remeasurementDate))
                .toList();
        leasePaymentScheduleRepository.deleteAll(futureSchedules);

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

        BigDecimal oldLeaseLiabilityValue = leaseLiability.getCurrentValue();
        BigDecimal newLeaseLiabilityValue = calculatePresentValueOfFuturePayments(
                updatedContract.getMonthlyPayment(),
                updatedContract.getEndDate(),
                remeasurementDate,
                updatedContract.getDiscountRate());

        leaseLiability.setCurrentValue(newLeaseLiabilityValue);
        leaseLiabilityRepository.save(leaseLiability);

        BigDecimal adjustmentAmount = newLeaseLiabilityValue.subtract(oldLeaseLiabilityValue);
        rouAsset.setCurrentBookValue(rouAsset.getCurrentBookValue().add(adjustmentAmount));
        rouAsset.setInitialValue(rouAsset.getInitialValue().add(adjustmentAmount));
        long remainingMonths = remeasurementDate.until(updatedContract.getEndDate()).toTotalMonths();
        rouAsset.setDepreciationAmountPerPeriod(remainingMonths > 0
                ? rouAsset.getCurrentBookValue().divide(new BigDecimal(remainingMonths), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO);
        rightOfUseAssetRepository.save(rouAsset);

        generateLeasePaymentSchedule(updatedContract, leaseLiability);

        if (adjustmentAmount.compareTo(BigDecimal.ZERO) == 0) {
            return updatedContract;
        }

        AccountSubject rouAssetAccount = getRequiredAccount("12300", "Right-of-Use Asset");
        AccountSubject leaseLiabilityAccount = getRequiredAccount("25100", "Lease Liability");

        JournalEntry remeasurementEntry = new JournalEntry();
        remeasurementEntry.setSlipDate(remeasurementDate);
        remeasurementEntry.setAccountingDate(remeasurementDate);
        remeasurementEntry.setDescription("IFRS 16 리스 재측정 " + updatedContract.getContractName()
                + " (" + updatedContract.getContractNo() + ")");
        remeasurementEntry.setEntryType("IFRS16_REMEASUREMENT");
        remeasurementEntry.setLineageSourceType("IFRS16_LEASE");
        remeasurementEntry.setLineageSourceId(updatedContract.getId().toString());
        remeasurementEntry.setCreatedBy("SYSTEM");
        remeasurementEntry.setStatus(JournalEntryStatus.DRAFT);

        if (adjustmentAmount.compareTo(BigDecimal.ZERO) > 0) {
            JournalDetail rouDebit = new JournalDetail();
            rouDebit.setDrcrType("DEBIT");
            rouDebit.setAccountSubject(rouAssetAccount);
            rouDebit.setAmount(adjustmentAmount);
            rouDebit.setDepartment(updatedContract.getDepartment());
            rouDebit.setDetailDescription("사용권자산 재측정 증가");
            remeasurementEntry.addDetail(rouDebit);

            JournalDetail llCredit = new JournalDetail();
            llCredit.setDrcrType("CREDIT");
            llCredit.setAccountSubject(leaseLiabilityAccount);
            llCredit.setAmount(adjustmentAmount);
            llCredit.setDepartment(updatedContract.getDepartment());
            llCredit.setDetailDescription("리스부채 재측정 증가");
            remeasurementEntry.addDetail(llCredit);
        } else {
            JournalDetail llDebit = new JournalDetail();
            llDebit.setDrcrType("DEBIT");
            llDebit.setAccountSubject(leaseLiabilityAccount);
            llDebit.setAmount(adjustmentAmount.abs());
            llDebit.setDepartment(updatedContract.getDepartment());
            llDebit.setDetailDescription("리스부채 재측정 감소");
            remeasurementEntry.addDetail(llDebit);

            JournalDetail rouCredit = new JournalDetail();
            rouCredit.setDrcrType("CREDIT");
            rouCredit.setAccountSubject(rouAssetAccount);
            rouCredit.setAmount(adjustmentAmount.abs());
            rouCredit.setDepartment(updatedContract.getDepartment());
            rouCredit.setDetailDescription("사용권자산 재측정 감소");
            remeasurementEntry.addDetail(rouCredit);
        }

        journalService.createJournalEntry(remeasurementEntry);
        return updatedContract;
    }

    private BigDecimal calculatePresentValueOfFuturePayments(BigDecimal monthlyPayment,
                                                             LocalDate endDate,
                                                             LocalDate calculationStartDate,
                                                             BigDecimal annualDiscountRate) {
        BigDecimal totalPresentValue = BigDecimal.ZERO;
        BigDecimal monthlyDiscountRate = annualDiscountRate
                .divide(new BigDecimal("1200"), 10, RoundingMode.HALF_UP);

        LocalDate currentPaymentDate = calculationStartDate.plusMonths(1);
        int monthCount = 1;
        while (currentPaymentDate.isBefore(endDate) || currentPaymentDate.isEqual(endDate)) {
            BigDecimal discountFactor = BigDecimal.ONE.add(monthlyDiscountRate).pow(monthCount);
            BigDecimal presentValue = monthlyPayment.divide(discountFactor, 2, RoundingMode.HALF_UP);
            totalPresentValue = totalPresentValue.add(presentValue);
            currentPaymentDate = currentPaymentDate.plusMonths(1);
            monthCount++;
        }
        return totalPresentValue;
    }

    @Transactional
    public void processMonthlyLeaseAccounting(LocalDate processDate) {
        List<LeaseContract> ifrs16LeaseContracts =
                leaseContractRepository.findByIfrs16ApplicableTrueAndStatus("ACTIVE");

        for (LeaseContract contract : ifrs16LeaseContracts) {
            LeasePaymentSchedule nextSchedule = leasePaymentScheduleRepository
                    .findByLeaseContractOrderByPaymentDateAsc(contract)
                    .stream()
                    .filter(schedule -> schedule.getPaymentDate().getYear() == processDate.getYear()
                            && schedule.getPaymentDate().getMonth() == processDate.getMonth())
                    .findFirst()
                    .orElse(null);

            if (nextSchedule == null || !"SCHEDULED".equals(nextSchedule.getStatus())) {
                continue;
            }

            RightOfUseAsset rouAsset = rightOfUseAssetRepository.findByLeaseContract(contract)
                    .orElseThrow(() -> new IllegalStateException("Right-of-Use Asset not found for contract: " + contract.getContractNo()));
            LeaseLiability leaseLiability = leaseLiabilityRepository.findByLeaseContract(contract)
                    .orElseThrow(() -> new IllegalStateException("Lease Liability not found for contract: " + contract.getContractNo()));

            BigDecimal depreciationAmount = rouAsset.getDepreciationAmountPerPeriod();
            rouAsset.setAccumulatedDepreciation(rouAsset.getAccumulatedDepreciation().add(depreciationAmount));
            rouAsset.setCurrentBookValue(rouAsset.getInitialValue().subtract(rouAsset.getAccumulatedDepreciation()));
            rightOfUseAssetRepository.save(rouAsset);

            BigDecimal interestPortion = nextSchedule.getInterestPortion();
            BigDecimal principalPortion = nextSchedule.getPrincipalPortion();
            BigDecimal totalPayment = nextSchedule.getScheduledPaymentAmount();

            leaseLiability.setAccumulatedInterestExpense(
                    leaseLiability.getAccumulatedInterestExpense().add(interestPortion));
            leaseLiability.setCurrentValue(leaseLiability.getCurrentValue().subtract(principalPortion));
            leaseLiabilityRepository.save(leaseLiability);

            nextSchedule.setActualPaymentAmount(totalPayment);
            nextSchedule.setStatus("PAID");
            leasePaymentScheduleRepository.save(nextSchedule);

            AccountSubject depreciationExpenseAccount = getRequiredAccount("51500", "Depreciation Expense");
            AccountSubject accumulatedDepreciationAccount = getRequiredAccount("12399", "Accumulated Depreciation");
            AccountSubject interestExpenseAccount = getRequiredAccount("93100", "Interest Expense");
            AccountSubject cashAccount = getRequiredAccount("10100", "Cash/Bank");
            AccountSubject leaseLiabilityAccount = getRequiredAccount("25100", "Lease Liability");

            JournalEntry depreciationEntry = new JournalEntry();
            depreciationEntry.setSlipDate(processDate);
            depreciationEntry.setAccountingDate(processDate);
            depreciationEntry.setDescription("IFRS 16 월별 리스 회계 처리 - 감가상각: "
                    + contract.getContractName() + " (" + contract.getContractNo() + ")");
            depreciationEntry.setEntryType("IFRS16_MONTHLY");
            depreciationEntry.setLineageSourceType("IFRS16_LEASE");
            depreciationEntry.setLineageSourceId(contract.getId().toString());
            depreciationEntry.setCreatedBy("SYSTEM");
            depreciationEntry.setStatus(JournalEntryStatus.DRAFT);

            JournalDetail depreciationDebit = new JournalDetail();
            depreciationDebit.setDrcrType("DEBIT");
            depreciationDebit.setAccountSubject(depreciationExpenseAccount);
            depreciationDebit.setAmount(depreciationAmount);
            depreciationDebit.setDepartment(contract.getDepartment());
            depreciationDebit.setDetailDescription("사용권자산 감가상각비");
            depreciationEntry.addDetail(depreciationDebit);

            JournalDetail depreciationCredit = new JournalDetail();
            depreciationCredit.setDrcrType("CREDIT");
            depreciationCredit.setAccountSubject(accumulatedDepreciationAccount);
            depreciationCredit.setAmount(depreciationAmount);
            depreciationCredit.setDepartment(contract.getDepartment());
            depreciationCredit.setDetailDescription("사용권자산 감가상각누계액");
            depreciationEntry.addDetail(depreciationCredit);

            journalService.createJournalEntry(depreciationEntry);

            JournalEntry paymentEntry = new JournalEntry();
            paymentEntry.setSlipDate(processDate);
            paymentEntry.setAccountingDate(processDate);
            paymentEntry.setDescription("IFRS 16 월별 리스 회계 처리 - 상환: "
                    + contract.getContractName() + " (" + contract.getContractNo() + ")");
            paymentEntry.setEntryType("IFRS16_MONTHLY");
            paymentEntry.setLineageSourceType("IFRS16_LEASE");
            paymentEntry.setLineageSourceId(contract.getId().toString());
            paymentEntry.setCreatedBy("SYSTEM");
            paymentEntry.setStatus(JournalEntryStatus.DRAFT);

            JournalDetail interestDebit = new JournalDetail();
            interestDebit.setDrcrType("DEBIT");
            interestDebit.setAccountSubject(interestExpenseAccount);
            interestDebit.setAmount(interestPortion);
            interestDebit.setDepartment(contract.getDepartment());
            interestDebit.setDetailDescription("리스 이자비용");
            paymentEntry.addDetail(interestDebit);

            JournalDetail principalDebit = new JournalDetail();
            principalDebit.setDrcrType("DEBIT");
            principalDebit.setAccountSubject(leaseLiabilityAccount);
            principalDebit.setAmount(principalPortion);
            principalDebit.setDepartment(contract.getDepartment());
            principalDebit.setDetailDescription("리스부채 원금 상환");
            paymentEntry.addDetail(principalDebit);

            JournalDetail cashCredit = new JournalDetail();
            cashCredit.setDrcrType("CREDIT");
            cashCredit.setAccountSubject(cashAccount);
            cashCredit.setAmount(totalPayment);
            cashCredit.setDepartment(contract.getDepartment());
            cashCredit.setDetailDescription("리스료 지급");
            paymentEntry.addDetail(cashCredit);

            journalService.createJournalEntry(paymentEntry);
        }
    }

    private AccountSubject getRequiredAccount(String code, String label) {
        return accountSubjectRepository.findById(code)
                .orElseThrow(() -> new IllegalStateException("AccountSubject for " + label + " not found"));
    }
}
