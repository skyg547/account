package com.ho.account.asset.service;

import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseLiability;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import com.ho.account.asset.domain.RightOfUseAsset;
import com.ho.account.asset.repository.LeaseContractRepository;
import com.ho.account.asset.repository.LeaseLiabilityRepository;
import com.ho.account.asset.repository.LeasePaymentScheduleRepository;
import com.ho.account.asset.repository.RightOfUseAssetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 리스 회계 서비스 (Lease Accounting Service)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeaseAccountingService {

    private final LeaseContractRepository leaseContractRepository;
    private final RightOfUseAssetRepository rightOfUseAssetRepository;
    private final LeaseLiabilityRepository leaseLiabilityRepository;
    private final LeasePaymentScheduleRepository leasePaymentScheduleRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String TOPIC = "transaction-events";

    @Transactional
    public LeaseContract recognizeInitialLease(LeaseContract contract) {
        LeaseContract savedContract = leaseContractRepository.save(contract);

        RightOfUseAsset rouAsset = new RightOfUseAsset();
        rouAsset.setLeaseContract(savedContract);
        rouAsset.setAssetName(savedContract.getContractName() + " - ROU");
        rouAsset.setRecognitionDate(savedContract.getStartDate());
        rouAsset.setInitialValue(savedContract.getInitialRightOfUseAssetValue());
        rouAsset.setCurrentBookValue(savedContract.getInitialRightOfUseAssetValue());
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
        leaseLiabilityRepository.save(leaseLiability);

        generateLeasePaymentSchedule(savedContract, leaseLiability);

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "IFRS16_INITIAL_RECOGNITION");
        event.put("contractId", savedContract.getId());
        event.put("rouAmount", savedContract.getInitialRightOfUseAssetValue());
        event.put("liabilityAmount", savedContract.getInitialLeaseLiabilityValue());
        event.put("accountingDate", savedContract.getStartDate().toString());
        event.put("deptCode", savedContract.getDepartment().getCode());
        
        kafkaTemplate.send(TOPIC, event);
        return savedContract;
    }

    @Transactional
    public void processMonthlyLeaseAccounting(LocalDate processDate) {
        List<LeaseContract> activeContracts = leaseContractRepository.findByIfrs16ApplicableTrueAndStatus("ACTIVE");
        for (LeaseContract contract : activeContracts) {
            processContractMonthlyAccounting(contract, processDate);
        }
    }

    /**
     * 리스 재측정 (Remeasurement) - 컨트롤러 호출을 위해 복원
     */
    @Transactional
    public LeaseContract remeasureLease(Long contractId, LocalDate remeasureDate, BigDecimal newPayment, LocalDate newEndDate, BigDecimal newRate) {
        LeaseContract contract = leaseContractRepository.findById(contractId).orElseThrow();
        // ... (간소화된 재측정 로직) ...
        if (newPayment != null) contract.setMonthlyPayment(newPayment);
        if (newEndDate != null) contract.setEndDate(newEndDate);
        if (newRate != null) contract.setDiscountRate(newRate);
        
        LeaseContract updated = leaseContractRepository.save(contract);
        
        // 재측정 이벤트 발행
        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "IFRS16_REMEASUREMENT");
        event.put("contractId", updated.getId());
        event.put("accountingDate", remeasureDate.toString());
        event.put("deptCode", updated.getDepartment().getCode());
        kafkaTemplate.send(TOPIC, event);
        
        return updated;
    }

    private void processContractMonthlyAccounting(LeaseContract contract, LocalDate processDate) {
        LeasePaymentSchedule schedule = leasePaymentScheduleRepository.findByLeaseContractOrderByPaymentDateAsc(contract)
                .stream()
                .filter(s -> s.getPaymentDate().getYear() == processDate.getYear() && s.getPaymentDate().getMonth() == processDate.getMonth())
                .findFirst()
                .orElse(null);

        if (schedule == null || !"SCHEDULED".equals(schedule.getStatus())) return;

        RightOfUseAsset rouAsset = rightOfUseAssetRepository.findByLeaseContract(contract).orElseThrow();
        BigDecimal depreciationAmount = rouAsset.getDepreciationAmountPerPeriod();
        rouAsset.setAccumulatedDepreciation(rouAsset.getAccumulatedDepreciation().add(depreciationAmount));
        rouAsset.setCurrentBookValue(rouAsset.getInitialValue().subtract(rouAsset.getAccumulatedDepreciation()));
        rightOfUseAssetRepository.save(rouAsset);

        LeaseLiability leaseLiability = leaseLiabilityRepository.findByLeaseContract(contract).orElseThrow();
        leaseLiability.setCurrentValue(leaseLiability.getCurrentValue().subtract(schedule.getPrincipalPortion()));
        leaseLiabilityRepository.save(leaseLiability);

        schedule.setStatus("PAID");
        leasePaymentScheduleRepository.save(schedule);

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "IFRS16_MONTHLY_PROCESS");
        event.put("contractId", contract.getId());
        event.put("depreciationAmount", depreciationAmount);
        event.put("interestAmount", schedule.getInterestPortion());
        event.put("principalAmount", schedule.getPrincipalPortion());
        event.put("totalPayment", schedule.getScheduledPaymentAmount());
        event.put("accountingDate", processDate.toString());
        event.put("deptCode", contract.getDepartment().getCode());
        
        kafkaTemplate.send(TOPIC, event);
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
        leasePaymentScheduleRepository.saveAll(schedules); // use saveAll instead of save
    }
}
