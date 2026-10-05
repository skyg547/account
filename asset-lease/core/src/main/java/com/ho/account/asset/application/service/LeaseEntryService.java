package com.ho.account.asset.application.service;

import com.ho.account.asset.application.port.in.LeaseUseCase;
import com.ho.account.asset.application.port.out.AssetEventPort;
import com.ho.account.asset.application.port.out.LeaseAccountMappingPort;
import com.ho.account.asset.application.port.out.LeasePersistencePort;
import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseLiability;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import com.ho.account.asset.domain.RightOfUseAsset;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionCommand;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionLineCommand;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final LeaseAccountMappingPort leaseAccountMappingPort;

    private static final String TOPIC = "transaction-events";

    /**
     * 🎓 [교육적 주석 - IFRS 16 최초 인식 및 외부 입력 PV 교차 검증 적용]
     * 신규 리스 계약을 등록할 때 외부 요청 객체(DTO 등)가 지정한 현재가치(PV)를 무조건 신뢰하지 않습니다.
     * 계약의 월 리스료, 계약 기간, 증분차입이자율을 기반으로 도메인 내부에서 PV를 자동 계산하여 
     * 외부 값과의 교차 검증을 거치고, 검증된 도메인 PV 값으로 사용권자산과 리스부채를 최초 인식합니다.
     */
    @Override
    @Transactional
    public LeaseContract registerLeaseContract(LeaseContract contract, String actor) {
        contract.validateForRegistration();
        if (contract.isIfrs16Applicable() && !contract.isShortTermLease() && !contract.isLowValueLease()) {
            contract.updatePresentValueAndValidate();
        }
        LeaseContract savedContract = persistencePort.saveContract(contract);

        if (savedContract.isIfrs16Applicable() && !savedContract.isShortTermLease() && !savedContract.isLowValueLease()) {
            recognizeInitialLease(savedContract, requireActor(actor));
        }
        return savedContract;
    }

    private void recognizeInitialLease(LeaseContract contract, String actor) {
        // 외부 요청 입력값을 100% 신뢰하지 않고 도메인 자동 산출 PV로 재검증 및 설정
        contract.updatePresentValueAndValidate();

        RightOfUseAsset rouAsset = new RightOfUseAsset();
        rouAsset.setLeaseContract(contract);
        rouAsset.setAssetName(contract.getContractName() + " - ROU");
        rouAsset.setRecognitionDate(contract.getStartDate());
        rouAsset.setInitialValue(contract.getInitialRightOfUseAssetValue());
        rouAsset.setCurrentBookValue(contract.getInitialRightOfUseAssetValue());
        rouAsset.setAccumulatedDepreciation(BigDecimal.ZERO.setScale(2));
        int termMonths = contract.calculateTermMonths();
        rouAsset.setDepreciationAmountPerPeriod(termMonths > 0 
                ? contract.getInitialRightOfUseAssetValue().divide(new BigDecimal(termMonths), 2, RoundingMode.HALF_UP) 
                : BigDecimal.ZERO);
        persistencePort.saveROUAsset(rouAsset);

        LeaseLiability leaseLiability = new LeaseLiability();
        leaseLiability.setLeaseContract(contract);
        leaseLiability.setRecognitionDate(contract.getStartDate());
        leaseLiability.setInitialValue(contract.getInitialLeaseLiabilityValue());
        leaseLiability.setCurrentValue(contract.getInitialLeaseLiabilityValue());
        leaseLiability.setAccumulatedInterestExpense(BigDecimal.ZERO.setScale(2));
        persistencePort.saveLiability(leaseLiability);

        generateLeasePaymentSchedule(contract, leaseLiability);

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "IFRS16_INITIAL_RECOGNITION");
        event.put("contractId", contract.getId());
        event.put("rouAmount", contract.getInitialRightOfUseAssetValue());
        event.put("liabilityAmount", contract.getInitialLeaseLiabilityValue());
        event.put("accountingDate", contract.getStartDate().toString());
        event.put("deptCode", contract.getDepartmentCode());
        event.put("actor", actor);
        
        eventPort.sendAssetEvent(TOPIC, event);
    }

    @Override
    @Transactional
    public void processMonthlyLeaseAccounting(LocalDate processDate, String actor) {
        String auditActor = requireActor(actor);
        List<LeaseContract> activeContracts = inLockOrder(persistencePort.findIfrs16ApplicableActiveContracts());
        for (LeaseContract contract : activeContracts) {
            processContractMonthlyAccounting(contract, processDate, auditActor);
        }
    }

    @Override
    @Transactional
    public void processMonthlyLeasePayment(LocalDate paymentDate) {
        List<LeaseContract> activeContracts = inLockOrder(persistencePort.findActiveContracts("ACTIVE"));

        for (LeaseContract contract : activeContracts) {
            if (contract.getPaymentDay() == paymentDate.getDayOfMonth()) {
                LeaseContract lockedContract = persistencePort.findContractByIdForUpdate(contract.getId())
                        .orElseThrow(() -> new IllegalStateException("Contract not found: " + contract.getId()));
                YearMonth paymentMonth = YearMonth.from(paymentDate);
                if ("ACTIVE".equals(lockedContract.getStatus())
                        && lockedContract.getPaymentDay() == paymentDate.getDayOfMonth()
                        && !paymentMonth.isBefore(YearMonth.from(lockedContract.getStartDate()))
                        && !paymentMonth.isAfter(YearMonth.from(lockedContract.getEndDate()))) {
                    createLeaseExpenditure(lockedContract, paymentDate);
                }
            }
        }
    }

    @Override
    @Transactional
    public LeaseContract remeasureLease(Long contractId, LocalDate remeasureDate, BigDecimal newPayment, LocalDate newEndDate, BigDecimal newRate, String actor) {
        String auditActor = requireActor(actor);
        if (remeasureDate == null) {
            throw new IllegalArgumentException("remeasurement date is required");
        }
        LeaseContract contract = persistencePort.findContractByIdForUpdate(contractId)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + contractId));
        if (!"ACTIVE".equals(contract.getStatus()) || !isCapitalizedIfrs16Lease(contract)) {
            throw new IllegalStateException("only active capitalized IFRS 16 leases can be remeasured");
        }

        BigDecimal payment = newPayment != null ? newPayment : contract.getMonthlyPayment();
        BigDecimal rate = newRate != null ? newRate : contract.getDiscountRate();
        LocalDate endDate = newEndDate != null ? newEndDate : contract.getEndDate();
        if (endDate.isBefore(contract.getStartDate())) {
            throw new IllegalArgumentException("lease start date must be on or before end date");
        }
        if (remeasureDate.isBefore(contract.getStartDate()) || remeasureDate.isAfter(contract.getEndDate())
                || endDate.isBefore(remeasureDate)) {
            throw new IllegalArgumentException("remeasurement date must be an unpaid installment within the lease term");
        }

        List<LeasePaymentSchedule> schedules = persistencePort.findSchedulesByContract(contract);
        Map<LocalDate, LeasePaymentSchedule> futureSchedules = new HashMap<>();
        boolean basisIsScheduled = false;
        for (LeasePaymentSchedule schedule : schedules) {
            if (schedule.getPaymentDate() == null || schedule.getStatus() == null) {
                throw new IllegalStateException("lease payment schedule is incomplete");
            }
            if (schedule.getPaymentDate().isBefore(remeasureDate)) {
                if ("SCHEDULED".equals(schedule.getStatus())) {
                    throw new IllegalArgumentException("earlier installments must be processed before remeasurement");
                }
                if (!"PAID".equals(schedule.getStatus()) && !"CANCELLED".equals(schedule.getStatus())) {
                    throw new IllegalStateException("unsupported earlier lease payment status: " + schedule.getStatus());
                }
                continue;
            }
            if ("PAID".equals(schedule.getStatus())) {
                throw new IllegalArgumentException("processed installments cannot be remeasured");
            }
            if (!"SCHEDULED".equals(schedule.getStatus()) && !"CANCELLED".equals(schedule.getStatus())) {
                throw new IllegalStateException("unsupported future lease payment status: " + schedule.getStatus());
            }
            if (futureSchedules.putIfAbsent(schedule.getPaymentDate(), schedule) != null) {
                throw new IllegalStateException("duplicate future lease payment date: " + schedule.getPaymentDate());
            }
            if (schedule.getPaymentDate().equals(remeasureDate) && "SCHEDULED".equals(schedule.getStatus())) {
                basisIsScheduled = true;
            }
        }
        if (!basisIsScheduled) {
            throw new IllegalArgumentException("remeasurement date must match an unpaid scheduled installment");
        }

        int remainingPeriods = 0;
        for (LocalDate date = remeasureDate; !date.isAfter(endDate); date = date.plusMonths(1)) {
            remainingPeriods++;
        }
        LeaseLiability liability = persistencePort.findLiabilityByContract(contract)
                .orElseThrow(() -> new IllegalStateException("Lease Liability not found for contract: " + contractId));
        RightOfUseAsset rouAsset = persistencePort.findROUAssetByContract(contract)
                .orElseThrow(() -> new IllegalStateException("ROU Asset not found for contract: " + contractId));
        BigDecimal oldLiability = liability.getCurrentValue();
        LeaseContract.Remeasurement remeasurement = LeaseContract.calculateRemeasurement(
                oldLiability, payment, remainingPeriods, rate);
        rouAsset.applyRemeasurement(remeasurement.adjustmentAmount(), remainingPeriods);

        List<LeasePaymentSchedule> changedSchedules = repriceFutureSchedules(
                contract, remeasureDate, remainingPeriods, payment, rate, remeasurement.presentValue(), futureSchedules);
        contract.setMonthlyPayment(payment);
        contract.setEndDate(endDate);
        contract.setDiscountRate(rate);
        liability.setCurrentValue(remeasurement.presentValue());
        LeaseContract updated = persistencePort.saveContract(contract);
        persistencePort.saveLiability(liability);
        persistencePort.saveROUAsset(rouAsset);
        persistencePort.savePaymentSchedules(changedSchedules);

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "IFRS16_REMEASUREMENT");
        event.put("contractId", updated.getId());
        event.put("accountingDate", remeasureDate.toString());
        event.put("remeasureDate", remeasureDate.toString());
        event.put("adjustmentAmount", remeasurement.adjustmentAmount());
        event.put("oldLiabilityAmount", oldLiability);
        event.put("newLiabilityAmount", remeasurement.presentValue());
        event.put("deptCode", updated.getDepartmentCode());
        event.put("actor", auditActor);
        eventPort.sendAssetEvent(TOPIC, event);

        return updated;
    }

    private List<LeasePaymentSchedule> repriceFutureSchedules(
            LeaseContract contract, LocalDate basisDate, int remainingPeriods, BigDecimal payment,
            BigDecimal rate, BigDecimal presentValue, Map<LocalDate, LeasePaymentSchedule> futureSchedules) {
        List<LeasePaymentSchedule> changed = new ArrayList<>();
        List<LeaseContract.Installment> installments = LeaseContract.calculateInstallments(
                presentValue, payment, remainingPeriods, rate);
        LocalDate date = basisDate;
        for (LeaseContract.Installment installment : installments) {
            LeasePaymentSchedule schedule = futureSchedules.remove(date);
            if (schedule == null) {
                schedule = new LeasePaymentSchedule();
                schedule.setLeaseContract(contract);
                schedule.setPaymentDate(date);
            }
            schedule.setScheduledPaymentAmount(payment);
            schedule.setInterestPortion(installment.interest());
            schedule.setPrincipalPortion(installment.principal());
            schedule.setRemainingLeaseLiability(installment.remainingLiability());
            schedule.setStatus("SCHEDULED");
            changed.add(schedule);
            date = date.plusMonths(1);
        }
        // Retain shortened installments as cancelled rows so retries or later extensions can reuse them.
        for (LeasePaymentSchedule schedule : futureSchedules.values()) {
            if (!"CANCELLED".equals(schedule.getStatus())) {
                schedule.setStatus("CANCELLED");
                changed.add(schedule);
            }
        }
        return changed;
    }

    @Override
    public Optional<LeaseContract> getLeaseContract(Long id) {
        return persistencePort.findContractById(id);
    }

    @Override
    public List<LeaseContract> getAllActiveLeaseContracts() {
        return persistencePort.findActiveContracts("ACTIVE");
    }

    private List<LeaseContract> inLockOrder(List<LeaseContract> contracts) {
        // Monthly runs acquire several row locks; a stable order avoids opposite-order deadlocks.
        return contracts.stream().sorted(Comparator.comparing(LeaseContract::getId)).toList();
    }

    private void processContractMonthlyAccounting(LeaseContract contract, LocalDate processDate, String actor) {
        Long contractId = contract.getId();
        LeaseContract lockedContract = persistencePort.findContractByIdForUpdate(contractId)
                .orElseThrow(() -> new IllegalStateException("Contract not found: " + contractId));
        List<LeasePaymentSchedule> schedules = persistencePort.findSchedulesByContract(lockedContract);
        LeasePaymentSchedule schedule = schedules
                .stream()
                .filter(s -> sameYearMonth(s.getPaymentDate(), processDate))
                .filter(s -> "SCHEDULED".equals(s.getStatus()))
                .findFirst()
                .orElse(null);

        if (schedule == null) return;
        if (schedules.stream().anyMatch(s -> "SCHEDULED".equals(s.getStatus())
                && s.getPaymentDate().isBefore(schedule.getPaymentDate()))) {
            throw new IllegalStateException("earlier scheduled lease installments must be processed first: " + contractId);
        }

        RightOfUseAsset rouAsset = persistencePort.findROUAssetByContract(lockedContract)
                .orElseThrow(() -> new IllegalStateException("ROU Asset not found for contract: " + contractId));
        
        // 🎓 [교육적 주석 - 도메인 캡슐화 & 음수 전락 방지]
        // 서비스에서 장부가액을 절차적으로 직접 계산하지 않고, RightOfUseAsset의 일반/최종 기간 상각을 호출하여
        // IFRS 16 장부가액 음수 전락 방지(Non-negativity Floor) 및 도메인 불변성을 보장받습니다.
        boolean finalInstallment = schedules.stream()
                .noneMatch(s -> "SCHEDULED".equals(s.getStatus())
                        && s.getPaymentDate().isAfter(schedule.getPaymentDate()));
        // The last period absorbs cent rounding so the ROU book value closes with the schedule.
        BigDecimal depreciationAmount = finalInstallment
                ? rouAsset.depreciateRemaining() : rouAsset.depreciate();
        persistencePort.saveROUAsset(rouAsset);

        LeaseLiability leaseLiability = persistencePort.findLiabilityByContract(lockedContract)
                .orElseThrow(() -> new IllegalStateException("Lease Liability not found for contract: " + contractId));
        
        leaseLiability.setCurrentValue(leaseLiability.getCurrentValue().subtract(schedule.getPrincipalPortion()));
        persistencePort.saveLiability(leaseLiability);

        schedule.setStatus("PAID");
        persistencePort.savePaymentSchedule(schedule);

        Map<String, Object> event = new HashMap<>();
        event.put("transactionType", "IFRS16_MONTHLY_PROCESS");
        event.put("contractId", contractId);
        event.put("depreciationAmount", depreciationAmount);
        event.put("interestAmount", schedule.getInterestPortion());
        event.put("principalAmount", schedule.getPrincipalPortion());
        event.put("totalPayment", schedule.getScheduledPaymentAmount());
        event.put("accountingDate", processDate.toString());
        event.put("deptCode", lockedContract.getDepartmentCode());
        event.put("actor", actor);

        eventPort.sendAssetEvent(TOPIC, event);
    }

    private void generateLeasePaymentSchedule(LeaseContract contract, LeaseLiability leaseLiability) {
        List<LeasePaymentSchedule> schedules = new ArrayList<>();
        LocalDate currentDate = contract.getStartDate();
        BigDecimal remainingLiability = leaseLiability.getInitialValue();
        BigDecimal monthlyPayment = contract.getMonthlyPayment();
        List<LeaseContract.Installment> installments = LeaseContract.calculateInstallments(
                remainingLiability, monthlyPayment, contract.calculateTermMonths(), contract.getDiscountRate());
        for (LeaseContract.Installment installment : installments) {
            LeasePaymentSchedule schedule = new LeasePaymentSchedule();
            schedule.setLeaseContract(contract);
            schedule.setPaymentDate(currentDate);
            schedule.setScheduledPaymentAmount(monthlyPayment);
            remainingLiability = installment.remainingLiability();
            schedule.setInterestPortion(installment.interest());
            schedule.setPrincipalPortion(installment.principal());
            schedule.setRemainingLeaseLiability(remainingLiability);
            schedule.setStatus("SCHEDULED");
            schedules.add(schedule);
            currentDate = currentDate.plusMonths(1);
        }
        persistencePort.savePaymentSchedules(schedules);
    }

    private void createLeaseExpenditure(LeaseContract contract, LocalDate date) {
        LeaseAccountMappingPort.LeasePaymentAccounts accounts = leaseAccountMappingPort.resolvePaymentAccounts(contract);
        if (isCapitalizedIfrs16Lease(contract)) {
            Optional<LeasePaymentSchedule> schedule = findLeasePaymentScheduleForMonth(contract, date);
            if (schedule.isPresent()) {
                createIfrs16LeasePaymentResolution(contract, date, schedule.get(), accounts);
                return;
            }
        }

        // 단기/소액/스케줄 미확정 리스는 기존 비용 처리 경로를 유지한다.
        leasePaymentResolutionPort.createLeasePaymentResolution(new LeasePaymentResolutionCommand(
                "리스료 지급 " + contract.getContractName(),
                date,
                date,
                contract.getDepartmentCode(),
                contract.getExpenseAccountCode(), // 차변: 리스부채 또는 비용 계정
                accounts.accountsPayableAccountCode(), // 대변: 미지급금
                contract.getLessorCode(),
                contract.getMonthlyPayment(),
                "월 리스료"
        ));
    }

    private boolean isCapitalizedIfrs16Lease(LeaseContract contract) {
        return contract.isIfrs16Applicable()
                && !contract.isShortTermLease()
                && !contract.isLowValueLease();
    }

    private Optional<LeasePaymentSchedule> findLeasePaymentScheduleForMonth(LeaseContract contract, LocalDate date) {
        return persistencePort.findSchedulesByContract(contract).stream()
                .filter(schedule -> sameYearMonth(schedule.getPaymentDate(), date))
                .filter(schedule -> "SCHEDULED".equals(schedule.getStatus())
                        || "PAID".equals(schedule.getStatus()))
                .findFirst();
    }

    private boolean sameYearMonth(LocalDate left, LocalDate right) {
        return left != null
                && right != null
                && left.getYear() == right.getYear()
                && left.getMonth() == right.getMonth();
    }

    private void createIfrs16LeasePaymentResolution(
            LeaseContract contract,
            LocalDate date,
            LeasePaymentSchedule schedule,
            LeaseAccountMappingPort.LeasePaymentAccounts accounts) {
        List<LeasePaymentResolutionLineCommand> debitLines = new ArrayList<>();
        addDebitLine(
                debitLines,
                accounts.leaseInterestExpenseAccountCode(),
                schedule.getInterestPortion(),
                "리스 이자비용");
        addDebitLine(
                debitLines,
                accounts.leaseLiabilityAccountCode(),
                schedule.getPrincipalPortion(),
                "리스부채 원금 상환");

        if (debitLines.isEmpty()) {
            throw new IllegalStateException("Lease payment schedule has no positive payment portions: "
                    + contract.getId());
        }

        leasePaymentResolutionPort.createLeasePaymentResolution(new LeasePaymentResolutionCommand(
                "리스료 지급 " + contract.getContractName(),
                date,
                date,
                contract.getDepartmentCode(),
                accounts.accountsPayableAccountCode(),
                contract.getLessorCode(),
                debitLines));
    }

    private void addDebitLine(
            List<LeasePaymentResolutionLineCommand> debitLines,
            String accountCode,
            BigDecimal amount,
            String description) {
        if (amount != null && amount.signum() > 0) {
            debitLines.add(new LeasePaymentResolutionLineCommand(accountCode, amount, description));
        }
    }

    private String requireActor(String actor) {
        if (actor == null || actor.isBlank()) {
            throw new IllegalArgumentException("X-User-ID is required for lease accounting audit");
        }
        return actor.trim();
    }
}
