package com.ho.account.asset.application.service;

import com.ho.account.asset.application.port.out.AssetEventPort;
import com.ho.account.asset.application.port.out.LeaseAccountMappingPort;
import com.ho.account.asset.application.port.out.LeasePersistencePort;
import com.ho.account.asset.domain.LeaseContract;
import com.ho.account.asset.domain.LeaseLiability;
import com.ho.account.asset.domain.LeasePaymentSchedule;
import com.ho.account.asset.domain.RightOfUseAsset;
import com.ho.account.asset.infrastructure.persistence.LeasePersistenceAdapter;
import com.ho.account.asset.infrastructure.persistence.repository.LeaseContractRepository;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionCommand;
import com.ho.account.contracts.expenditure.LeasePaymentResolutionPort;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.annotation.DirtiesContext;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void remeasurementReconcilesRemainingBalanceScheduleAndNextMonthlyEvent() {
        LeaseScenario scenario = registeredScenarioAfterMay();
        BigDecimal oldLiability = scenario.persistence.liability.getCurrentValue();
        BigDecimal oldBookValue = scenario.persistence.rouAsset.getCurrentBookValue();
        BigDecimal historicalInitial = scenario.persistence.liability.getInitialValue();
        List<LeasePaymentSchedule> paid = scenario.persistence.schedules.stream()
                .filter(schedule -> "PAID".equals(schedule.getStatus())).toList();
        List<BigDecimal> paidBalances = paid.stream().map(LeasePaymentSchedule::getRemainingLeaseLiability).toList();

        scenario.service.remeasureLease(1L, LocalDate.of(2026, 6, 1),
                new BigDecimal("2400.00"), null, null, "lease-user");

        BigDecimal expectedPv = LeaseContract.calculatePresentValue(new BigDecimal("2400.00"), 7,
                new BigDecimal("6.0"));
        BigDecimal adjustment = expectedPv.subtract(oldLiability);
        assertThat(scenario.persistence.liability.getCurrentValue()).isEqualByComparingTo(expectedPv);
        assertThat(scenario.persistence.liability.getInitialValue()).isEqualByComparingTo(historicalInitial);
        assertThat(scenario.persistence.rouAsset.getCurrentBookValue()).isEqualByComparingTo(oldBookValue.add(adjustment));
        assertThat(scenario.persistence.rouAsset.getAccumulatedDepreciation())
                .isEqualByComparingTo(scenario.persistence.rouAsset.getInitialValue()
                        .subtract(oldBookValue));
        assertThat(scenario.persistence.rouAsset.getDepreciationAmountPerPeriod())
                .isEqualByComparingTo(oldBookValue.add(adjustment).divide(new BigDecimal("7"), 2,
                        java.math.RoundingMode.HALF_UP));
        assertThat(paid.stream().map(LeasePaymentSchedule::getRemainingLeaseLiability).toList())
                .containsExactlyElementsOf(paidBalances);
        assertThat(paid).allMatch(schedule -> "PAID".equals(schedule.getStatus()));

        List<LeasePaymentSchedule> future = scenario.persistence.schedules.stream()
                .filter(schedule -> "SCHEDULED".equals(schedule.getStatus())).toList();
        assertEquals(7, future.size());
        assertEquals(12, scenario.persistence.schedules.size());
        assertThat(future.get(0).getScheduledPaymentAmount()).isEqualByComparingTo("2400.00");
        assertThat(future.get(0).getInterestPortion()).isEqualByComparingTo("82.34");
        assertThat(future.get(0).getPrincipalPortion()).isEqualByComparingTo("2317.66");
        assertThat(future.get(6).getRemainingLeaseLiability()).isEqualByComparingTo("0.00");
        assertThat(scenario.event.eventData.get("adjustmentAmount")).isEqualTo(adjustment);
        assertEquals("2026-06-01", scenario.event.eventData.get("accountingDate"));

        scenario.service.processMonthlyLeaseAccounting(LocalDate.of(2026, 6, 30), "lease-user");
        assertEquals("PAID", future.get(0).getStatus());
        assertThat(scenario.event.eventData.get("interestAmount")).isEqualTo(future.get(0).getInterestPortion());
        assertThat(scenario.event.eventData.get("principalAmount")).isEqualTo(future.get(0).getPrincipalPortion());
        assertThat(scenario.event.eventData.get("totalPayment")).isEqualTo(future.get(0).getScheduledPaymentAmount());
        scenario.service.processMonthlyLeasePayment(LocalDate.of(2026, 6, 25));
        assertEquals(2, scenario.resolution.command.debitLines().size());
        assertThat(scenario.resolution.command.debitLines().get(0).amount())
                .isEqualByComparingTo(future.get(0).getInterestPortion());
        assertThat(scenario.resolution.command.debitLines().get(1).amount())
                .isEqualByComparingTo(future.get(0).getPrincipalPortion());
        for (int month = 7; month <= 12; month++) {
            scenario.service.processMonthlyLeaseAccounting(LocalDate.of(2026, month, 28), "lease-user");
        }
        assertThat(scenario.persistence.liability.getCurrentValue()).isEqualByComparingTo("0.00");
        assertThat(scenario.persistence.rouAsset.getCurrentBookValue()).isEqualByComparingTo("0.00");
        assertEquals(RightOfUseAsset.STATUS_FULLY_DEPRECIATED, scenario.persistence.rouAsset.getStatus());
    }

    @Test
    void extensionAddsOnlyNewFutureInstallmentsAndRetryDoesNotDuplicateThem() {
        LeaseScenario scenario = registeredScenarioAfterMay();
        LocalDate extension = LocalDate.of(2027, 3, 31);

        scenario.service.remeasureLease(1L, LocalDate.of(2026, 6, 1),
                null, extension, null, "lease-user");
        BigDecimal firstLiability = scenario.persistence.liability.getCurrentValue();
        BigDecimal firstBookValue = scenario.persistence.rouAsset.getCurrentBookValue();
        assertEquals(15, scenario.persistence.schedules.size());
        assertEquals(5, scenario.persistence.schedules.stream().filter(s -> "PAID".equals(s.getStatus())).count());
        assertEquals(10, scenario.persistence.schedules.stream().filter(s -> "SCHEDULED".equals(s.getStatus())).count());

        scenario.service.remeasureLease(1L, LocalDate.of(2026, 6, 1),
                null, extension, null, "lease-user");
        assertEquals(15, scenario.persistence.schedules.size());
        assertThat(scenario.persistence.liability.getCurrentValue()).isEqualByComparingTo(firstLiability);
        assertThat(scenario.persistence.rouAsset.getCurrentBookValue()).isEqualByComparingTo(firstBookValue);
    }

    @Test
    void shorteningCancelsOnlyFutureInstallmentsBeyondNewEndDate() {
        LeaseScenario scenario = registeredScenarioAfterMay();
        scenario.service.remeasureLease(1L, LocalDate.of(2026, 6, 1),
                null, LocalDate.of(2026, 9, 15), new BigDecimal("3.0"), "lease-user");

        assertEquals(5, scenario.persistence.schedules.stream().filter(s -> "PAID".equals(s.getStatus())).count());
        assertEquals(4, scenario.persistence.schedules.stream().filter(s -> "SCHEDULED".equals(s.getStatus())).count());
        assertEquals(3, scenario.persistence.schedules.stream().filter(s -> "CANCELLED".equals(s.getStatus())).count());
        assertThat(scenario.persistence.liability.getCurrentValue()).isEqualByComparingTo(
                LeaseContract.calculatePresentValue(new BigDecimal("1200.00"), 4, new BigDecimal("3.0")));
        assertThat(scenario.persistence.schedules.stream().filter(s -> "SCHEDULED".equals(s.getStatus()))
                .reduce((left, right) -> right).orElseThrow().getRemainingLeaseLiability()).isEqualByComparingTo("0.00");
        scenario.service.processMonthlyLeasePayment(LocalDate.of(2026, 9, 25));
        assertEquals(1, scenario.resolution.callCount);
        assertThat(scenario.resolution.command.amount()).isEqualByComparingTo("1200.00");
        scenario.service.processMonthlyLeasePayment(LocalDate.of(2026, 10, 25));
        assertEquals(1, scenario.resolution.callCount);
    }

    @Test
    void longLowPaymentScheduleClosesWithoutNegativeInterestOrChangingCashPayment() {
        LeaseContract contract = createIfrs16LeaseContract();
        contract.setMonthlyPayment(new BigDecimal("5.00"));
        contract.setDiscountRate(new BigDecimal("3.0"));
        contract.setEndDate(LocalDate.of(2035, 12, 31));
        FakeLeasePersistencePort persistence = new FakeLeasePersistencePort(List.of(contract), List.of());
        LeaseEntryService service = new LeaseEntryService(persistence, new NoOpAssetEventPort(),
                new RecordingLeasePaymentResolutionPort(), new StaticLeaseAccountMappingPort());

        service.registerLeaseContract(contract, "lease-user");

        assertEquals(120, persistence.schedules.size());
        assertThat(persistence.liability.getInitialValue()).isEqualByComparingTo("517.81");
        assertThat(persistence.schedules.get(119).getRemainingLeaseLiability()).isEqualByComparingTo("0.00");
        assertThat(persistence.schedules).allSatisfy(schedule -> {
            assertThat(schedule.getScheduledPaymentAmount()).isEqualByComparingTo("5.00");
            assertThat(schedule.getInterestPortion()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(schedule.getPrincipalPortion()).isGreaterThanOrEqualTo(BigDecimal.ZERO);
            assertThat(schedule.getInterestPortion().add(schedule.getPrincipalPortion()))
                    .isEqualByComparingTo("5.00");
        });
    }

    @Test
    void monthlyAccountingRejectsSkippedPeriodsBeforeFinalRoundingWriteOff() {
        LeaseContract contract = createIfrs16LeaseContract();
        contract.setDiscountRate(new BigDecimal("6.0"));
        FakeLeasePersistencePort persistence = new FakeLeasePersistencePort(List.of(contract), List.of());
        RecordingAssetEventPort event = new RecordingAssetEventPort();
        RecordingLeasePaymentResolutionPort resolution = new RecordingLeasePaymentResolutionPort();
        LeaseEntryService service = new LeaseEntryService(persistence, event,
                resolution, new StaticLeaseAccountMappingPort());
        service.registerLeaseContract(contract, "lease-user");
        BigDecimal initialBook = persistence.rouAsset.getCurrentBookValue();
        BigDecimal initialLiability = persistence.liability.getCurrentValue();
        int eventCount = event.events.size();

        assertThrows(IllegalStateException.class, () -> service.processMonthlyLeaseAccounting(
                LocalDate.of(2026, 12, 31), "lease-user"));

        assertThat(persistence.rouAsset.getCurrentBookValue()).isEqualByComparingTo(initialBook);
        assertThat(persistence.liability.getCurrentValue()).isEqualByComparingTo(initialLiability);
        assertEquals(12, persistence.schedules.stream().filter(s -> "SCHEDULED".equals(s.getStatus())).count());
        assertEquals(eventCount, event.events.size());
    }

    @Test
    void retroactiveRemeasurementAfterPaidPeriodLeavesFinancialStateUntouched() {
        LeaseScenario scenario = registeredScenarioAfterMay();
        BigDecimal liability = scenario.persistence.liability.getCurrentValue();
        BigDecimal bookValue = scenario.persistence.rouAsset.getCurrentBookValue();
        int eventCount = scenario.event.events.size();

        assertThrows(IllegalArgumentException.class, () -> scenario.service.remeasureLease(1L,
                LocalDate.of(2026, 5, 1), new BigDecimal("2400.00"), null, null, "lease-user"));

        assertThat(scenario.persistence.liability.getCurrentValue()).isEqualByComparingTo(liability);
        assertThat(scenario.persistence.rouAsset.getCurrentBookValue()).isEqualByComparingTo(bookValue);
        assertThat(scenario.contract.getMonthlyPayment()).isEqualByComparingTo("1200.00");
        assertEquals(12, scenario.persistence.schedules.size());
        assertEquals(eventCount, scenario.event.events.size());
    }

    @Nested
    @SpringBootTest(classes = LeaseTransactionTestApp.class, properties = {
            "spring.datasource.url=jdbc:h2:mem:lease861;DB_CLOSE_DELAY=-1",
            "spring.jpa.hibernate.ddl-auto=create-drop",
            "spring.cloud.vault.enabled=false",
            "spring.cloud.config.enabled=false",
            "spring.cloud.discovery.enabled=false"
    })
    @DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
    class TransactionRollbackTest {
        @Autowired private LeaseEntryService service;
        @Autowired private LeasePersistencePort persistence;
        @Autowired private EntityManager entityManager;
        @Autowired private ControlledAssetEventPort eventPort;

        @Test
        void successfulRemeasurementPersistsPaidHistoryAndExtendedSchedule() {
            LeaseContract contract = createIfrs16LeaseContract();
            contract.setId(null);
            contract.setDiscountRate(new BigDecimal("6.0"));
            Long id = service.registerLeaseContract(contract, "lease-user").getId();
            for (int month = 1; month <= 5; month++) {
                service.processMonthlyLeaseAccounting(LocalDate.of(2026, month, 28), "lease-user");
            }
            BigDecimal historicalInitial = persistence.findLiabilityByContract(contract).orElseThrow().getInitialValue();
            List<BigDecimal> paidBalances = persistence.findSchedulesByContract(contract).stream()
                    .filter(s -> "PAID".equals(s.getStatus()))
                    .map(LeasePaymentSchedule::getRemainingLeaseLiability).toList();

            service.remeasureLease(id, LocalDate.of(2026, 6, 1), new BigDecimal("2400.00"),
                    LocalDate.of(2027, 3, 31), null, "lease-user");
            entityManager.clear();

            LeaseContract after = persistence.findContractById(id).orElseThrow();
            List<LeasePaymentSchedule> schedules = persistence.findSchedulesByContract(after);
            assertEquals(15, schedules.size());
            assertEquals(10, schedules.stream().filter(s -> "SCHEDULED".equals(s.getStatus())).count());
            assertThat(schedules.stream().filter(s -> "PAID".equals(s.getStatus()))
                    .map(LeasePaymentSchedule::getRemainingLeaseLiability).toList())
                    .containsExactlyElementsOf(paidBalances);
            assertThat(persistence.findLiabilityByContract(after).orElseThrow().getInitialValue())
                    .isEqualByComparingTo(historicalInitial);
            assertThat(persistence.findLiabilityByContract(after).orElseThrow().getCurrentValue())
                    .isEqualByComparingTo(LeaseContract.calculatePresentValue(
                            new BigDecimal("2400.00"), 10, new BigDecimal("6.0")));
            assertThat(schedules.get(14).getRemainingLeaseLiability()).isEqualByComparingTo("0.00");
            assertThat(eventPort.eventData.get("adjustmentAmount")).isInstanceOf(BigDecimal.class);
        }

        @Test
        void concurrentSameDateRetryWaitsForCommitAndDoesNotInsertDuplicateRows() throws Exception {
            LeaseContract contract = createIfrs16LeaseContract();
            contract.setId(null);
            contract.setDiscountRate(new BigDecimal("6.0"));
            Long id = service.registerLeaseContract(contract, "lease-user").getId();
            for (int month = 1; month <= 5; month++) {
                service.processMonthlyLeaseAccounting(LocalDate.of(2026, month, 28), "lease-user");
            }

            eventPort.blockFirstRemeasurement.set(true);
            ExecutorService executor = Executors.newFixedThreadPool(2);
            try {
                Runnable remeasure = () -> service.remeasureLease(id, LocalDate.of(2026, 6, 1),
                        new BigDecimal("2400.00"), LocalDate.of(2027, 3, 31), null, "lease-user");
                Future<?> first = executor.submit(remeasure);
                assertTrue(eventPort.firstRemeasurementEntered.await(10, TimeUnit.SECONDS));
                CountDownLatch secondStarted = new CountDownLatch(1);
                Future<?> second = executor.submit(() -> {
                    secondStarted.countDown();
                    remeasure.run();
                });
                assertTrue(secondStarted.await(10, TimeUnit.SECONDS));
                assertThrows(TimeoutException.class, () -> second.get(250, TimeUnit.MILLISECONDS),
                        "the second request must wait for the first contract transaction");
                eventPort.releaseFirstRemeasurement.countDown();
                first.get(15, TimeUnit.SECONDS);
                second.get(15, TimeUnit.SECONDS);
            } finally {
                eventPort.releaseFirstRemeasurement.countDown();
                executor.shutdownNow();
            }

            entityManager.clear();
            LeaseContract after = persistence.findContractById(id).orElseThrow();
            assertEquals(15, persistence.findSchedulesByContract(after).size());
            assertThat(persistence.findLiabilityByContract(after).orElseThrow().getCurrentValue())
                    .isEqualByComparingTo(LeaseContract.calculatePresentValue(
                            new BigDecimal("2400.00"), 10, new BigDecimal("6.0")));
        }

        @Test
        void eventFailureRollsBackContractBalancesAndScheduleChanges() {
            LeaseContract contract = createIfrs16LeaseContract();
            contract.setId(null);
            contract.setDiscountRate(new BigDecimal("6.0"));
            Long id = service.registerLeaseContract(contract, "lease-user").getId();
            for (int month = 1; month <= 5; month++) {
                service.processMonthlyLeaseAccounting(LocalDate.of(2026, month, 28), "lease-user");
            }
            LeaseContract before = persistence.findContractById(id).orElseThrow();
            BigDecimal liabilityBefore = persistence.findLiabilityByContract(before).orElseThrow().getCurrentValue();
            BigDecimal bookBefore = persistence.findROUAssetByContract(before).orElseThrow().getCurrentBookValue();
            eventPort.failOnRemeasurement = true;

            assertThrows(IllegalStateException.class, () -> service.remeasureLease(id,
                    LocalDate.of(2026, 6, 1), new BigDecimal("2400.00"),
                    LocalDate.of(2027, 3, 31), null, "lease-user"));

            entityManager.clear();
            LeaseContract after = persistence.findContractById(id).orElseThrow();
            assertThat(after.getMonthlyPayment()).isEqualByComparingTo("1200.00");
            assertEquals(LocalDate.of(2026, 12, 31), after.getEndDate());
            assertThat(persistence.findLiabilityByContract(after).orElseThrow().getCurrentValue())
                    .isEqualByComparingTo(liabilityBefore);
            assertThat(persistence.findROUAssetByContract(after).orElseThrow().getCurrentBookValue())
                    .isEqualByComparingTo(bookBefore);
            List<LeasePaymentSchedule> schedules = persistence.findSchedulesByContract(after);
            assertEquals(12, schedules.size());
            assertEquals(5, schedules.stream().filter(s -> "PAID".equals(s.getStatus())).count());
            assertEquals(7, schedules.stream().filter(s -> "SCHEDULED".equals(s.getStatus())).count());
        }
    }

    @SpringBootConfiguration
    @EnableAutoConfiguration
    @EntityScan(basePackageClasses = LeaseContract.class)
    @EnableJpaRepositories(basePackageClasses = LeaseContractRepository.class)
    @Import({LeaseEntryService.class, LeasePersistenceAdapter.class})
    static class LeaseTransactionTestApp {
        @Bean
        ControlledAssetEventPort assetEventPort() {
            return new ControlledAssetEventPort();
        }

        @Bean
        LeasePaymentResolutionPort leasePaymentResolutionPort() {
            return command -> { };
        }

        @Bean
        LeaseAccountMappingPort leaseAccountMappingPort() {
            return new StaticLeaseAccountMappingPort();
        }
    }

    private static class ControlledAssetEventPort implements AssetEventPort {
        private volatile boolean failOnRemeasurement;
        private volatile Map<String, Object> eventData;
        private final AtomicBoolean blockFirstRemeasurement = new AtomicBoolean();
        private final CountDownLatch firstRemeasurementEntered = new CountDownLatch(1);
        private final CountDownLatch releaseFirstRemeasurement = new CountDownLatch(1);

        @Override
        public void sendAssetEvent(String topic, Map<String, Object> data) {
            if (failOnRemeasurement && "IFRS16_REMEASUREMENT".equals(data.get("transactionType"))) {
                throw new IllegalStateException("injected event failure");
            }
            if ("IFRS16_REMEASUREMENT".equals(data.get("transactionType"))
                    && blockFirstRemeasurement.compareAndSet(true, false)) {
                firstRemeasurementEntered.countDown();
                try {
                    if (!releaseFirstRemeasurement.await(10, TimeUnit.SECONDS)) {
                        throw new IllegalStateException("timed out waiting to release first remeasurement");
                    }
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException("remeasurement test interrupted", interrupted);
                }
            }
            eventData = data;
        }
    }

    private LeaseScenario registeredScenarioAfterMay() {
        LeaseContract contract = createIfrs16LeaseContract();
        contract.setDiscountRate(new BigDecimal("6.0"));
        FakeLeasePersistencePort persistence = new FakeLeasePersistencePort(List.of(contract), List.of());
        RecordingAssetEventPort event = new RecordingAssetEventPort();
        RecordingLeasePaymentResolutionPort resolution = new RecordingLeasePaymentResolutionPort();
        LeaseEntryService service = new LeaseEntryService(persistence, event,
                resolution, new StaticLeaseAccountMappingPort());
        service.registerLeaseContract(contract, "lease-user");
        for (int month = 1; month <= 5; month++) {
            service.processMonthlyLeaseAccounting(LocalDate.of(2026, month, 28), "lease-user");
        }
        return new LeaseScenario(contract, service, persistence, event, resolution);
    }

    private record LeaseScenario(LeaseContract contract, LeaseEntryService service,
                                 FakeLeasePersistencePort persistence, RecordingAssetEventPort event,
                                 RecordingLeasePaymentResolutionPort resolution) { }

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
        private RightOfUseAsset rouAsset;
        private LeaseLiability liability;

        private FakeLeasePersistencePort(List<LeaseContract> activeContracts, List<LeasePaymentSchedule> schedules) {
            this(activeContracts, schedules, null, null);
        }

        private FakeLeasePersistencePort(
                List<LeaseContract> activeContracts,
                List<LeasePaymentSchedule> schedules,
                RightOfUseAsset rouAsset,
                LeaseLiability liability) {
            this.activeContracts = activeContracts;
            this.schedules = new ArrayList<>(schedules);
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
        public Optional<LeaseContract> findContractByIdForUpdate(Long id) {
            return findContractById(id);
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
            this.rouAsset = asset;
            return asset;
        }

        @Override
        public Optional<RightOfUseAsset> findROUAssetByContract(LeaseContract contract) {
            return Optional.ofNullable(rouAsset);
        }

        @Override
        public LeaseLiability saveLiability(LeaseLiability liability) {
            this.liability = liability;
            return liability;
        }

        @Override
        public Optional<LeaseLiability> findLiabilityByContract(LeaseContract contract) {
            return Optional.ofNullable(liability);
        }

        @Override
        public void savePaymentSchedules(List<LeasePaymentSchedule> schedules) {
            for (LeasePaymentSchedule schedule : schedules) {
                if (!this.schedules.contains(schedule)) {
                    this.schedules.add(schedule);
                }
            }
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
        private final List<Map<String, Object>> events = new ArrayList<>();

        @Override
        public void sendAssetEvent(String topic, Map<String, Object> eventData) {
            this.eventData = eventData;
            this.events.add(eventData);
        }
    }

    private static class RecordingLeasePaymentResolutionPort implements LeasePaymentResolutionPort {

        private LeasePaymentResolutionCommand command;
        private int callCount;

        @Override
        public void createLeasePaymentResolution(LeasePaymentResolutionCommand command) {
            this.command = command;
            this.callCount++;
        }
    }
}
