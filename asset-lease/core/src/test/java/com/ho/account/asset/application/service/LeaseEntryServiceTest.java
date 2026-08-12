package com.ho.account.asset.application.service;

import com.ho.account.asset.application.port.out.AssetEventPort;
import com.ho.account.asset.application.port.out.LeaseAccountMappingPort;
import com.ho.account.asset.application.port.out.LeasePersistencePort;
import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseLiability;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import com.ho.account.asset.domain.RightOfUseAsset;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionCommand;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeaseEntryServiceTest {

    @Test
    void registrationRejectsPaymentDayOutsideCalendarRangeBeforePersistence() {
        LeaseContract contract = createIfrs16LeaseContract();
        contract.setPaymentDay(0);
        LeaseEntryService service = new LeaseEntryService(
                new FakeLeasePersistencePort(List.of(), List.of()),
                new NoOpAssetEventPort(),
                new RecordingLeasePaymentResolutionPort(),
                new StaticLeaseAccountMappingPort());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.registerLeaseContract(contract, "lease-user"));

        assertEquals("payment day must be between 1 and 31", exception.getMessage());
    }

    @Test
    void registrationRejectsReversedLeasePeriodBeforePersistence() {
        LeaseContract contract = createIfrs16LeaseContract();
        contract.setStartDate(LocalDate.of(2027, 1, 1));
        contract.setEndDate(LocalDate.of(2026, 12, 31));
        LeaseEntryService service = new LeaseEntryService(
                new FakeLeasePersistencePort(List.of(), List.of()),
                new NoOpAssetEventPort(),
                new RecordingLeasePaymentResolutionPort(),
                new StaticLeaseAccountMappingPort());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.registerLeaseContract(contract, "lease-user"));

        assertEquals("lease start date must be on or before end date", exception.getMessage());
    }

    @Test
    void registrationRejectsMissingLeaseDateBeforePersistence() {
        LeaseContract contract = createIfrs16LeaseContract();
        contract.setStartDate(null);
        LeaseEntryService service = new LeaseEntryService(
                new FakeLeasePersistencePort(List.of(), List.of()),
                new NoOpAssetEventPort(),
                new RecordingLeasePaymentResolutionPort(),
                new StaticLeaseAccountMappingPort());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.registerLeaseContract(contract, "lease-user"));

        assertEquals("lease start date and end date are required", exception.getMessage());
    }

    @Test
    void remeasurementRejectsReversedLeasePeriodBeforePersistenceOrEvent() {
        LeaseContract contract = createIfrs16LeaseContract();
        RecordingAssetEventPort eventPort = new RecordingAssetEventPort();
        LeaseEntryService service = new LeaseEntryService(
                new FakeLeasePersistencePort(List.of(contract), List.of()),
                eventPort,
                new RecordingLeasePaymentResolutionPort(),
                new StaticLeaseAccountMappingPort());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.remeasureLease(
                        contract.getId(),
                        LocalDate.of(2026, 6, 1),
                        null,
                        LocalDate.of(2025, 12, 31),
                        null,
                        "lease-user"));

        assertEquals("lease start date must be on or before end date", exception.getMessage());
        assertNull(eventPort.eventData);
    }

    @Test
    void processMonthlyLeasePaymentSplitsIfrs16PaymentIntoInterestAndPrincipal() {
        LeaseContract contract = createIfrs16LeaseContract();
        LeasePaymentSchedule schedule = createSchedule(contract);
        FakeLeasePersistencePort persistencePort = new FakeLeasePersistencePort(List.of(contract), List.of(schedule));
        RecordingLeasePaymentResolutionPort leasePaymentResolutionPort = new RecordingLeasePaymentResolutionPort();
        LeaseEntryService service = new LeaseEntryService(
                persistencePort,
                new NoOpAssetEventPort(),
                leasePaymentResolutionPort,
                new StaticLeaseAccountMappingPort());

        service.processMonthlyLeasePayment(LocalDate.of(2026, 5, 25));

        LeasePaymentResolutionCommand command = leasePaymentResolutionPort.command;
        assertEquals("21100", command.creditAccountCode());
        assertEquals(new BigDecimal("1200.00"), command.amount());
        assertEquals(2, command.debitLines().size());
        assertEquals("93100", command.debitLines().get(0).debitAccountCode());
        assertEquals(new BigDecimal("200.00"), command.debitLines().get(0).amount());
        assertEquals("25100", command.debitLines().get(1).debitAccountCode());
        assertEquals(new BigDecimal("1000.00"), command.debitLines().get(1).amount());
    }

    @Test
    void processMonthlyLeaseAccountingAddsActorToAuditEvent() {
        LeaseContract contract = createIfrs16LeaseContract();
        LeasePaymentSchedule schedule = createSchedule(contract);
        RightOfUseAsset rouAsset = createRightOfUseAsset(contract);
        LeaseLiability liability = createLeaseLiability(contract);
        FakeLeasePersistencePort persistencePort = new FakeLeasePersistencePort(
                List.of(contract),
                List.of(schedule),
                rouAsset,
                liability);
        RecordingAssetEventPort eventPort = new RecordingAssetEventPort();
        LeaseEntryService service = new LeaseEntryService(
                persistencePort,
                eventPort,
                new RecordingLeasePaymentResolutionPort(),
                new StaticLeaseAccountMappingPort());

        service.processMonthlyLeaseAccounting(LocalDate.of(2026, 5, 31), "lease-user");

        assertEquals("lease-user", eventPort.eventData.get("actor"));
        assertEquals("PAID", schedule.getStatus());
        assertEquals(new BigDecimal("9000.00"), liability.getCurrentValue());
    }

    @Test
    void processMonthlyLeaseAccountingClampsROUDepreciationToPreventNegativeBookValue() {
        LeaseContract contract = createIfrs16LeaseContract();
        LeasePaymentSchedule schedule = createSchedule(contract);
        RightOfUseAsset rouAsset = createRightOfUseAsset(contract);
        rouAsset.setCurrentBookValue(new BigDecimal("400.00"));
        rouAsset.setAccumulatedDepreciation(new BigDecimal("11600.00"));

        LeaseLiability liability = createLeaseLiability(contract);
        FakeLeasePersistencePort persistencePort = new FakeLeasePersistencePort(
                List.of(contract),
                List.of(schedule),
                rouAsset,
                liability);
        RecordingAssetEventPort eventPort = new RecordingAssetEventPort();
        LeaseEntryService service = new LeaseEntryService(
                persistencePort,
                eventPort,
                new RecordingLeasePaymentResolutionPort(),
                new StaticLeaseAccountMappingPort());

        service.processMonthlyLeaseAccounting(LocalDate.of(2026, 5, 31), "lease-user");

        assertThat((BigDecimal) eventPort.eventData.get("depreciationAmount")).isEqualByComparingTo("400.00");
        assertThat(rouAsset.getCurrentBookValue()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(rouAsset.getAccumulatedDepreciation()).isEqualByComparingTo("12000.00");
        assertEquals(RightOfUseAsset.STATUS_FULLY_DEPRECIATED, rouAsset.getStatus());
    }

    @Test
    void registerLeaseContractCalculatesAndSetsPresentValueAutomatically() {
        LeaseContract contract = createIfrs16LeaseContract();
        contract.setDiscountRate(new BigDecimal("6.0"));
        contract.setInitialRightOfUseAssetValue(new BigDecimal("99999999.00"));

        RecordingAssetEventPort eventPort = new RecordingAssetEventPort();
        LeaseEntryService service = new LeaseEntryService(
                new FakeLeasePersistencePort(List.of(contract), List.of()),
                eventPort,
                new RecordingLeasePaymentResolutionPort(),
                new StaticLeaseAccountMappingPort());

        LeaseContract registered = service.registerLeaseContract(contract, "lease-user");

        BigDecimal expectedPv = new BigDecimal("13942.72");
        assertEquals(expectedPv, registered.getInitialRightOfUseAssetValue());
        assertEquals(expectedPv, registered.getInitialLeaseLiabilityValue());
        assertEquals(expectedPv, eventPort.eventData.get("rouAmount"));
    }

    private static class StaticLeaseAccountMappingPort implements LeaseAccountMappingPort {

        @Override
        public LeasePaymentAccounts resolvePaymentAccounts(LeaseContract contract) {
            return new LeasePaymentAccounts("25100", "93100", "21100");
        }
    }

    private LeaseContract createIfrs16LeaseContract() {
        LeaseContract contract = new LeaseContract();
        contract.setId(1L);
        contract.setContractNo("LC-2026-001");
        contract.setContractName("테스트 리스");
        contract.setLessorCode("V001");
        contract.setDepartmentCode("D001");
        contract.setExpenseAccountCode("51500");
        contract.setMonthlyPayment(new BigDecimal("1200.00"));
        contract.setStartDate(LocalDate.of(2026, 1, 1));
        contract.setEndDate(LocalDate.of(2026, 12, 31));
        contract.setPaymentDay(25);
        contract.setIfrs16Applicable(true);
        contract.setShortTermLease(false);
        contract.setLowValueLease(false);
        contract.setStatus("ACTIVE");
        return contract;
    }

    private LeasePaymentSchedule createSchedule(LeaseContract contract) {
        LeasePaymentSchedule schedule = new LeasePaymentSchedule();
        schedule.setLeaseContract(contract);
        schedule.setPaymentDate(LocalDate.of(2026, 5, 1));
        schedule.setScheduledPaymentAmount(new BigDecimal("1200.00"));
        schedule.setInterestPortion(new BigDecimal("200.00"));
        schedule.setPrincipalPortion(new BigDecimal("1000.00"));
        schedule.setRemainingLeaseLiability(new BigDecimal("9000.00"));
        schedule.setStatus("SCHEDULED");
        return schedule;
    }

    private RightOfUseAsset createRightOfUseAsset(LeaseContract contract) {
        RightOfUseAsset asset = new RightOfUseAsset();
        asset.setLeaseContract(contract);
        asset.setInitialValue(new BigDecimal("12000.00"));
        asset.setCurrentBookValue(new BigDecimal("12000.00"));
        asset.setAccumulatedDepreciation(BigDecimal.ZERO);
        asset.setDepreciationAmountPerPeriod(new BigDecimal("1000.00"));
        asset.setStatus(RightOfUseAsset.STATUS_ACTIVE);
        return asset;
    }

    private LeaseLiability createLeaseLiability(LeaseContract contract) {
        LeaseLiability liability = new LeaseLiability();
        liability.setLeaseContract(contract);
        liability.setInitialValue(new BigDecimal("10000.00"));
        liability.setCurrentValue(new BigDecimal("10000.00"));
        return liability;
    }

    private static class FakeLeasePersistencePort implements LeasePersistencePort {

        private final List<LeaseContract> activeContracts;
        private final List<LeasePaymentSchedule> schedules;
        private final RightOfUseAsset rouAsset;
        private final LeaseLiability liability;

        private FakeLeasePersistencePort(List<LeaseContract> activeContracts, List<LeasePaymentSchedule> schedules) {
            this(activeContracts, schedules, null, null);
        }

        private FakeLeasePersistencePort(
                List<LeaseContract> activeContracts,
                List<LeasePaymentSchedule> schedules,
                RightOfUseAsset rouAsset,
                LeaseLiability liability) {
            this.activeContracts = activeContracts;
            this.schedules = schedules;
            this.rouAsset = rouAsset;
            this.liability = liability;
        }

        @Override
        public LeaseContract saveContract(LeaseContract contract) {
            if (contract.getId() == null) {
                contract.setId(1L);
            }
            return contract;
        }

        @Override
        public Optional<LeaseContract> findContractById(Long id) {
            return activeContracts.stream()
                    .filter(contract -> id.equals(contract.getId()))
                    .findFirst();
        }

        @Override
        public List<LeaseContract> findActiveContracts(String status) {
            return activeContracts.stream()
                    .filter(contract -> status.equals(contract.getStatus()))
                    .toList();
        }

        @Override
        public List<LeaseContract> findIfrs16ApplicableActiveContracts() {
            return activeContracts.stream()
                    .filter(LeaseContract::isIfrs16Applicable)
                    .filter(contract -> "ACTIVE".equals(contract.getStatus()))
                    .toList();
        }

        @Override
        public RightOfUseAsset saveROUAsset(RightOfUseAsset asset) {
            return asset;
        }

        @Override
        public Optional<RightOfUseAsset> findROUAssetByContract(LeaseContract contract) {
            return Optional.ofNullable(rouAsset);
        }

        @Override
        public LeaseLiability saveLiability(LeaseLiability liability) {
            return liability;
        }

        @Override
        public Optional<LeaseLiability> findLiabilityByContract(LeaseContract contract) {
            return Optional.ofNullable(liability);
        }

        @Override
        public void savePaymentSchedules(List<LeasePaymentSchedule> schedules) {
            // no-op or record schedules
        }

        @Override
        public void savePaymentSchedule(LeasePaymentSchedule schedule) {
            schedule.setStatus(schedule.getStatus());
        }

        @Override
        public List<LeasePaymentSchedule> findSchedulesByContract(LeaseContract contract) {
            return schedules.stream()
                    .filter(schedule -> schedule.getLeaseContract() == contract)
                    .toList();
        }
    }

    private static class NoOpAssetEventPort implements AssetEventPort {

        @Override
        public void sendAssetEvent(String topic, Map<String, Object> eventData) {
        }
    }

    private static class RecordingAssetEventPort implements AssetEventPort {

        private Map<String, Object> eventData;

        @Override
        public void sendAssetEvent(String topic, Map<String, Object> eventData) {
            this.eventData = eventData;
        }
    }

    private static class RecordingLeasePaymentResolutionPort implements LeasePaymentResolutionPort {

        private LeasePaymentResolutionCommand command;

        @Override
        public void createLeasePaymentResolution(LeasePaymentResolutionCommand command) {
            this.command = command;
        }
    }
}
