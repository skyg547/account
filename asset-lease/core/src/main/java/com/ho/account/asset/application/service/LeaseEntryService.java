package com.ho.account.asset.application.service;

import com.ho.account.asset.application.port.in.LeaseUseCase;
import com.ho.account.asset.application.port.out.AssetEventPort;
import com.ho.account.asset.application.port.out.LeasePersistencePort;
import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseLiability;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import com.ho.account.asset.domain.RightOfUseAsset;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionCommand;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseEntryService implements LeaseUseCase {

    private final LeasePersistencePort persistencePort;
    private final AssetEventPort eventPort;
    private final LeasePaymentResolutionPort leasePaymentResolutionPort;

    private static final String TOPIC = "transaction-events";

    @Override
    @Transactional
    public LeaseContract registerLeaseContract(LeaseContract contract) {
        LeaseContract savedContract = persistencePort.saveContract(contract);

        if (savedContract.isIfrs16Applicable() && !savedContract.isShortTermLease() && !savedContract.isLowValueLease()) {
            recognizeInitialLease(savedContract);
        }
        return savedContract;
    }

    private void recognizeInitialLease(LeaseContract contract) {
        RightOfUseAsset rouAsset = new RightOfUseAsset();
        rouAsset.setLeaseContract(contract);
        rouAsset.setAssetName(contract.getContractName() + " - ROU");
        rouAsset.setRecognitionDate(contract.getStartDate());
        rouAsset.setInitialValue(contract.getInitialRightOfUseAssetValue());
        rouAsset.setCurrentBookValue(contract.getInitialRightOfUseAssetValue());
        long totalMonths = contract.getStartDate().until(contract.getEndDate()).toTotalMonths();
        rouAsset.setDepreciationAmountPerPeriod(totalMonths > 0 
                ? contract.getInitialRightOfUseAssetValue().divide(new BigDecimal(totalMonths), 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO);
        persistencePort.saveROUAsset(rouAsset);

        LeaseLiability leaseLiability = new LeaseLiability();
        leaseLiability.setLeaseContract(contract);
        leaseLiability.setRecognitionDate(contract.getStartDate());
        leaseLiability.setInitialValue(contract.getInitialLeaseLiabilityValue());
        leaseLiability.setCurrentValue(contract.getInitialLeaseLiabilityValue());
        persistencePort.saveLiability(leaseLiability);

        generateLeasePaymentSchedule(contract, leaseLiability);

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "IFRS16_INITIAL_RECOGNITION");
        event.put("contractId", contract.getId());
        event.put("rouAmount", contract.getInitialRightOfUseAssetValue());
        event.put("liabilityAmount", contract.getInitialLeaseLiabilityValue());
        event.put("accountingDate", contract.getStartDate().toString());
        event.put("deptCode", contract.getDepartment().getCode());
        
        eventPort.sendAssetEvent(TOPIC, event);
    }

    @Override
    @Transactional
    public void processMonthlyLeaseAccounting(LocalDate processDate) {
        List<LeaseContract> activeContracts = persistencePort.findIfrs16ApplicableActiveContracts();
        for (LeaseContract contract : activeContracts) {
            processContractMonthlyAccounting(contract, processDate);
        }
    }

    @Override
    @Transactional
    public void processMonthlyLeasePayment(LocalDate paymentDate) {
        List<LeaseContract> activeContracts = persistencePort.findActiveContracts("ACTIVE");

        for (LeaseContract contract : activeContracts) {
            if (contract.getPaymentDay() == paymentDate.getDayOfMonth()) {
                createLeaseExpenditure(contract, paymentDate);
            }
        }
    }

    @Override
    @Transactional
    public LeaseContract remeasureLease(Long contractId, LocalDate remeasureDate, BigDecimal newPayment, LocalDate newEndDate, BigDecimal newRate) {
        LeaseContract contract = persistencePort.findContractById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        
        if (newPayment != null) contract.setMonthlyPayment(newPayment);
        if (newEndDate != null) contract.setEndDate(newEndDate);
        if (newRate != null) contract.setDiscountRate(newRate);
        
        LeaseContract updated = persistencePort.saveContract(contract);
        
        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "IFRS16_REMEASUREMENT");
        event.put("contractId", updated.getId());
        event.put("accountingDate", remeasureDate.toString());
        event.put("deptCode", updated.getDepartment().getCode());
        eventPort.sendAssetEvent(TOPIC, event);
        
        return updated;
    }

    @Override
    public Optional<LeaseContract> getLeaseContract(Long id) {
        return persistencePort.findContractById(id);
    }

    @Override
    public List<LeaseContract> getAllActiveLeaseContracts() {
        return persistencePort.findActiveContracts("ACTIVE");
    }

    private void processContractMonthlyAccounting(LeaseContract contract, LocalDate processDate) {
        LeasePaymentSchedule schedule = persistencePort.findSchedulesByContract(contract)
                .stream()
                .filter(s -> s.getPaymentDate().getYear() == processDate.getYear() && s.getPaymentDate().getMonth() == processDate.getMonth())
                .findFirst()
                .orElse(null);

        if (schedule == null || !"SCHEDULED".equals(schedule.getStatus())) return;

        RightOfUseAsset rouAsset = persistencePort.findROUAssetByContract(contract)
                .orElseThrow(() -> new IllegalStateException("ROU Asset not found for contract: " + contract.getId()));
        
        BigDecimal depreciationAmount = rouAsset.getDepreciationAmountPerPeriod();
        rouAsset.setAccumulatedDepreciation(rouAsset.getAccumulatedDepreciation().add(depreciationAmount));
        rouAsset.setCurrentBookValue(rouAsset.getInitialValue().subtract(rouAsset.getAccumulatedDepreciation()));
        persistencePort.saveROUAsset(rouAsset);

        LeaseLiability leaseLiability = persistencePort.findLiabilityByContract(contract)
                .orElseThrow(() -> new IllegalStateException("Lease Liability not found for contract: " + contract.getId()));
        
        leaseLiability.setCurrentValue(leaseLiability.getCurrentValue().subtract(schedule.getPrincipalPortion()));
        persistencePort.saveLiability(leaseLiability);

        schedule.setStatus("PAID");
        persistencePort.savePaymentSchedule(schedule);

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "IFRS16_MONTHLY_PROCESS");
        event.put("contractId", contract.getId());
        event.put("depreciationAmount", depreciationAmount);
        event.put("interestAmount", schedule.getInterestPortion());
        event.put("principalAmount", schedule.getPrincipalPortion());
        event.put("totalPayment", schedule.getScheduledPaymentAmount());
        event.put("accountingDate", processDate.toString());
        event.put("deptCode", contract.getDepartment().getCode());
        
        eventPort.sendAssetEvent(TOPIC, event);
    }

    private void generateLeasePaymentSchedule(LeaseContract contract, LeaseLiability leaseLiability) {
        List<LeasePaymentSchedule> schedules = new ArrayList<>();
        LocalDate currentDate = contract.getStartDate();
        BigDecimal remainingLiability = leaseLiability.getInitialValue();
        BigDecimal monthlyPayment = contract.getMonthlyPayment();
        BigDecimal monthlyDiscountRate = contract.getDiscountRate().divide(new BigDecimal("1200"), 6, RoundingMode.HALF_UP);

        while (currentDate.isBefore(contract.getEndDate()) || currentDate.isEqual(contract.getEndDate())) {
            LeasePaymentSchedule schedule = new LeasePaymentSchedule();
            schedule.setLeaseContract(contract);
            schedule.setPaymentDate(currentDate);
            schedule.setScheduledPaymentAmount(monthlyPayment);
            BigDecimal interestPortion = remainingLiability.multiply(monthlyDiscountRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalPortion = monthlyPayment.subtract(interestPortion);
            remainingLiability = remainingLiability.subtract(principalPortion);
            schedule.setInterestPortion(interestPortion);
            schedule.setPrincipalPortion(principalPortion);
            schedule.setRemainingLeaseLiability(remainingLiability);
            schedule.setStatus("SCHEDULED");
            schedules.add(schedule);
            currentDate = currentDate.plusMonths(1);
        }
        persistencePort.savePaymentSchedules(schedules);
    }

    private void createLeaseExpenditure(LeaseContract contract, LocalDate date) {
        leasePaymentResolutionPort.createLeasePaymentResolution(new LeasePaymentResolutionCommand(
                "리스료 지급 " + contract.getContractName(),
                date,
                date,
                contract.getDepartment().getCode(),
                contract.getExpenseAccount().getCode(),
                contract.getLessor().getBusinessPartnerCode(),
                contract.getMonthlyPayment(),
                "월 리스료"
        ));
    }
}
