package com.ho.account.closing;

import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.application.port.in.FinalCloseEvidenceUseCase;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.ReopenApprovalPersistencePort;
import com.ho.account.closing.application.port.out.ClosingTaskPersistencePort;
import com.ho.account.closing.application.port.out.ClosingGatePersistencePort;
import com.ho.account.contracts.journal.JournalQueryPort;
import com.ho.account.closing.domain.ClosingAdjustment;
import com.ho.account.closing.domain.ClosingAuditLog.ActionType;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.domain.ClosingTask;
import com.ho.account.closing.domain.ClosingTask.ClosingTaskStatus;
import com.ho.account.closing.domain.ClosingGate;
import com.ho.account.closing.domain.ClosingGate.ClosingGateStatus;
import com.ho.account.closing.infrastructure.persistence.ClosingAuditLogRepository;
import com.ho.account.closing.application.service.FinalCloseEvidenceService;
import com.ho.account.closing.application.service.FinalCloseEvidenceValidationException;
import com.ho.account.closing.web.ClosingController;
import com.ho.account.closing.web.ClosingExceptionHandler;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.contracts.journal.JournalDetailSummary;
import com.ho.account.contracts.journal.JournalSide;
import com.ho.account.contracts.journal.JournalSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Commands run in separate real Spring transactions; Master uses a separate committed H2 database. */
@ActiveProfiles("local")
@SpringBootTest(classes = {ClosingApplication.class, ClosingAggregateConcurrencyIntegrationTest.Configuration.class},
        properties = {
                "spring.cloud.config.enabled=false", "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false", "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false", "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:closing-aggregate-concurrency;MODE=PostgreSQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000",
                "spring.datasource.username=sa", "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false"
        })
class ClosingAggregateConcurrencyIntegrationTest {
    @Autowired ClosingUseCase closing;
    @Autowired ClosingCalendarPersistencePort calendars;
    @Autowired ReopenApprovalPersistencePort approvals;
    @Autowired ClosingAuditLogRepository audits;
    @Autowired ControlledMaster master;
    @Autowired ClosingTaskPersistencePort tasks;
    @Autowired ClosingGatePersistencePort gates;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired EntityManager entityManager;
    @Autowired ClosingController controller;
    @Autowired FinalCloseEvidenceUseCase finalCloseEvidence;
    @Autowired FinalCloseEvidenceService finalCloseEvidenceService;
    @Autowired JournalQueryPort journalQuery;

    @Test
    void approvedReopenKeepsPriorEvidenceButRequiresFreshControlsAndCloseSnapshot() {
        ClosingCalendar prior = readyCalendar("2053", "01");
        long calendarId = prior.getId();
        long periodId = master.findFiscalPeriod("2053", "01").orElseThrow().id();
        ClosingTask oldTask = tasks.findByClosingCalendar(prior).get(0);
        ClosingGate oldGate = gates.findByClosingCalendar(prior).get(0);
        closing.determineClosingStatus(calendarId, "first-closer");
        ReopenApproval approval = closing.requestPeriodReopen(periodId, "requester", "later adjustment");
        closing.updateReopenApprovalStatus(approval.getId(), ReopenApprovalStatus.APPROVED, "approver");

        ClosingCalendar reopened = calendars.findById(calendarId).orElseThrow();
        assertThat(reopened.getCycleNumber()).isEqualTo(2);
        assertThat(tasks.findById(oldTask.getId()).orElseThrow().getStatus()).isEqualTo(ClosingTaskStatus.COMPLETED);
        assertThat(gates.findById(oldGate.getId()).orElseThrow().getStatus()).isEqualTo(ClosingGateStatus.PASSED);
        assertThatThrownBy(() -> closing.updateClosingTaskStatus(oldTask.getId(), ClosingTaskStatus.IN_PROGRESS, "worker"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> closing.checkAndPassClosingGate(oldGate.getId(), "reviewer"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(tasks.findByClosingCalendar(reopened)).hasSize(2);
        assertThat(gates.findByClosingCalendar(reopened)).hasSize(2);
        var currentTasks = closing.findClosingTasksByCalendarId(calendarId);
        var currentGates = gates.findByClosingCalendar(reopened).stream()
                .filter(candidate -> candidate.getCycleNumber() == reopened.getCycleNumber()).toList();
        assertThat(currentTasks).hasSize(1);
        assertThat(currentGates).hasSize(1);
        ClosingTask task = currentTasks.get(0);
        ClosingGate gate = currentGates.get(0);
        assertThat(task.getId()).isNotEqualTo(oldTask.getId());
        assertThat(gate.getId()).isNotEqualTo(oldGate.getId());
        assertThat(task.getCycleNumber()).isEqualTo(2);
        assertThat(gate.getCycleNumber()).isEqualTo(2);
        assertThat(task.getStatus()).isEqualTo(ClosingTaskStatus.PENDING);
        assertThat(gate.getStatus()).isEqualTo(ClosingGateStatus.PENDING);

        closing.updateClosingCalendarStatus(calendarId, ClosingCalendarStatus.IN_PROGRESS, "second-closer");
        assertThatThrownBy(() -> closing.determineClosingStatus(calendarId, "second-closer"))
                .isInstanceOf(IllegalStateException.class);
        closing.updateClosingTaskStatus(task.getId(), ClosingTaskStatus.IN_PROGRESS, "worker");
        closing.updateClosingTaskStatus(task.getId(), ClosingTaskStatus.COMPLETED, "worker");
        assertThatThrownBy(() -> closing.determineClosingStatus(calendarId, "second-closer"))
                .isInstanceOf(IllegalStateException.class);
        closing.checkAndPassClosingGate(gate.getId(), "reviewer");
        // The previous final-close snapshot predates the second start and cannot authorize this close.
        assertThatThrownBy(() -> closing.determineClosingStatus(calendarId, "second-closer"))
                .isInstanceOf(RuntimeException.class);
        assertThat(calendars.findById(calendarId).orElseThrow().getStatus())
                .isEqualTo(ClosingCalendarStatus.IN_PROGRESS);

        var periodBeforeAdjustment = master.findFiscalPeriodById(periodId).orElseThrow();
        FinalCloseEvidenceFixtures.recordValid(finalCloseEvidence, reopened, periodId,
                periodBeforeAdjustment.endDate());
        ClosingCalendar activeBeforeAdjustment = calendars.findById(calendarId).orElseThrow();
        assertThat(finalCloseEvidenceService.requireForFinalClose(activeBeforeAdjustment, periodBeforeAdjustment))
                .isNotNull();

        JournalSummary summary = new JournalSummary();
        summary.setAccountingDate(LocalDate.of(2053, 1, 15));
        JournalDetailSummary debit = new JournalDetailSummary();
        debit.setSide(JournalSide.DEBIT);
        debit.setAmount(new BigDecimal("125.000"));
        JournalDetailSummary credit = new JournalDetailSummary();
        credit.setSide(JournalSide.CREDIT);
        credit.setAmount(new BigDecimal("125.000"));
        when(journalQuery.getJournalSummary(885L)).thenReturn(summary);
        when(journalQuery.getJournalDetails(885L)).thenReturn(java.util.List.of(debit, credit));
        closing.createClosingAdjustment(periodId, 885L, ClosingAdjustment.AdjustmentType.ACCRUAL,
                "Post-control adjustment", "adjustment-approver");

        ClosingCalendar changed = calendars.findById(calendarId).orElseThrow();
        assertThat(changed.getCycleNumber()).isEqualTo(3);
        assertThat(changed.getLastSourceChangedAt()).isNotNull();
        assertThat(tasks.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(ClosingTaskStatus.COMPLETED);
        assertThat(gates.findById(gate.getId()).orElseThrow().getStatus()).isEqualTo(ClosingGateStatus.PASSED);
        assertThatThrownBy(() -> closing.determineClosingStatus(calendarId, "second-closer"))
                .isInstanceOf(IllegalStateException.class);
        var changedTask = closing.findClosingTasksByCalendarId(calendarId).get(0);
        var changedGate = gates.findByClosingCalendar(changed).stream()
                .filter(candidate -> candidate.getCycleNumber() == changed.getCycleNumber()).findFirst().orElseThrow();
        assertThat(changedTask.getId()).isNotEqualTo(task.getId());
        assertThat(changedGate.getId()).isNotEqualTo(gate.getId());
        assertThat(changedTask.getStatus()).isEqualTo(ClosingTaskStatus.PENDING);
        assertThat(changedGate.getStatus()).isEqualTo(ClosingGateStatus.PENDING);
        closing.updateClosingTaskStatus(changedTask.getId(), ClosingTaskStatus.IN_PROGRESS, "worker");
        closing.updateClosingTaskStatus(changedTask.getId(), ClosingTaskStatus.COMPLETED, "worker");
        closing.checkAndPassClosingGate(changedGate.getId(), "reviewer");
        // This valid snapshot was submitted after start but before the source mutation.
        assertThatThrownBy(() -> closing.determineClosingStatus(calendarId, "second-closer"))
                .isInstanceOf(FinalCloseEvidenceValidationException.class)
                .hasMessageContaining("source change");

        var period = master.findFiscalPeriodById(periodId).orElseThrow();
        FinalCloseEvidenceFixtures.recordValid(finalCloseEvidence, changed, periodId, period.endDate(),
                java.time.Instant.now(), "-after-adjustment", new java.math.BigDecimal("125.000"));
        assertThat(closing.determineClosingStatus(calendarId, "second-closer").getStatus())
                .isEqualTo(ClosingCalendarStatus.CLOSED);
        assertThat(tasks.findById(oldTask.getId()).orElseThrow().getStatus()).isEqualTo(ClosingTaskStatus.COMPLETED);
        assertThat(gates.findById(oldGate.getId()).orElseThrow().getStatus()).isEqualTo(ClosingGateStatus.PASSED);
    }

    @Test
    void approvingTransactionExcludesConcurrentRejectionAndPersistsOnlyOneDecision() throws Exception {
        Fixture fixture = closedFixture("2050", "01");
        Pause pause = master.pauseUpdate(fixture.periodId());
        Pause rejectRead = master.pauseRead(fixture.periodId(), "reject-losing-decision", 1);
        var http = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ClosingExceptionHandler()).build();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> approve = workers.submit(() -> outcome(() -> closing.updateReopenApprovalStatus(
                    fixture.approvalId(), ReopenApprovalStatus.APPROVED, "approver")));
            pause.awaitEntered();
            Future<MvcResult> reject = workers.submit(() -> {
                Thread.currentThread().setName("reject-losing-decision");
                return http.perform(put("/api/closing/reopen-approvals/{id}/status", fixture.approvalId())
                        .header("X-Auth-User", "rejector")
                        .header("X-Auth-Roles", "CLOSING_MANAGER")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"approvedBy\":\"rejector\"}")).andReturn();
            });
            // Both decision paths are now inside real transactions. The audited rejection
            // has already changed its stale PENDING entity; the fixed path has only read identity.
            rejectRead.awaitEntered();
            pause.release();
            assertThat(approve.get(10, TimeUnit.SECONDS)).isNull();
            rejectRead.release();
            MvcResult losingResponse = reject.get(10, TimeUnit.SECONDS);
            assertThat(losingResponse.getResponse().getStatus()).isEqualTo(409);
            assertThat(losingResponse.getResponse().getContentAsString()).contains("WORKFLOW_STATE_CONFLICT");
            assertThat(approvals.findById(fixture.approvalId()).orElseThrow().getStatus())
                    .isEqualTo(ReopenApprovalStatus.APPROVED);
            assertThat(calendars.findById(fixture.calendarId()).orElseThrow().getStatus())
                    .isEqualTo(ClosingCalendarStatus.OPEN);
            assertThat(master.findFiscalPeriodById(fixture.periodId()).orElseThrow().closingStatus()).isEqualTo("OPEN");
            assertThat(decisionAudits(fixture.calendarId())).containsExactly(ActionType.REOPEN_APPROVED);
        } finally {
            pause.release();
            rejectRead.release();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void rejectingTransactionExcludesConcurrentApprovalAndKeepsMasterClosed() throws Exception {
        Fixture fixture = closedFixture("2050", "02");
        Pause pause = master.pauseRead(fixture.periodId(), "reject-winning-decision", 2);
        Pause approveRead = master.pauseRead(fixture.periodId(), "approve-losing-decision", 1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        int writesBefore = master.writes.get();
        try {
            Future<Throwable> reject = workers.submit(() -> {
                Thread.currentThread().setName("reject-winning-decision");
                return outcome(() -> closing.updateReopenApprovalStatus(
                        fixture.approvalId(), ReopenApprovalStatus.REJECTED, "rejector"));
            });
            pause.awaitEntered();
            Future<Throwable> approve = workers.submit(() -> {
                Thread.currentThread().setName("approve-losing-decision");
                return outcome(() -> closing.updateReopenApprovalStatus(
                        fixture.approvalId(), ReopenApprovalStatus.APPROVED, "approver"));
            });
            approveRead.awaitEntered();
            pause.release();
            assertThat(reject.get(10, TimeUnit.SECONDS)).isNull();
            approveRead.release();
            assertThat(approve.get(10, TimeUnit.SECONDS)).isInstanceOf(IllegalStateException.class);
            assertThat(approvals.findById(fixture.approvalId()).orElseThrow().getStatus())
                    .isEqualTo(ReopenApprovalStatus.REJECTED);
            assertThat(calendars.findById(fixture.calendarId()).orElseThrow().getStatus())
                    .isEqualTo(ClosingCalendarStatus.CLOSED);
            assertThat(master.findFiscalPeriodById(fixture.periodId()).orElseThrow().closingStatus()).isEqualTo("CLOSED");
            assertThat(master.writes.get()).isEqualTo(writesBefore);
            assertThat(decisionAudits(fixture.calendarId())).containsExactly(ActionType.REOPEN_REJECTED);
        } finally {
            pause.release();
            approveRead.release();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    enum Mutation { START, TASK_CREATE, TASK_UPDATE, GATE_CREATE, GATE_PASS, CLOSE }

    @ParameterizedTest
    @EnumSource(Mutation.class)
    void everyWorkflowMutationWaitsForCalendarAndRevalidatesItsCommittedState(Mutation mutation) throws Exception {
        ClosingCalendar calendar = readyCalendar("2051", String.format(java.util.Locale.ROOT, "%02d", mutation.ordinal() + 1));
        ClosingTask optional = newTask(calendar, false);
        optional = closing.createClosingTask(optional);
        ClosingGate pendingGate = null;
        if (mutation == Mutation.GATE_PASS) pendingGate = closing.createClosingGate(newGate(calendar));
        Long taskId = optional.getId();
        Long gateId = pendingGate == null ? null : pendingGate.getId();
        int taskCount = tasks.findByClosingCalendar(calendar).size();
        int gateCount = gates.findByClosingCalendar(calendar).size();
        int writesBefore = master.writes.get();
        Pause rootOwner = new Pause();
        ExecutorService workers = Executors.newFixedThreadPool(2);
        try {
            Future<?> owner = workers.submit(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                ClosingCalendar managed = entityManager.find(ClosingCalendar.class, calendar.getId(), LockModeType.PESSIMISTIC_WRITE);
                rootOwner.stop();
                managed.setStatus(ClosingCalendarStatus.CLOSED);
            }));
            rootOwner.awaitEntered();
            CountDownLatch mutationStarted = new CountDownLatch(1);
            Future<Throwable> contender = workers.submit(() -> {
                mutationStarted.countDown();
                return outcome(() -> {
                    switch (mutation) {
                        case START -> closing.updateClosingCalendarStatus(calendar.getId(), ClosingCalendarStatus.IN_PROGRESS, "rival");
                        case TASK_CREATE -> closing.createClosingTask(newTask(calendar, true));
                        case TASK_UPDATE -> closing.updateClosingTaskStatus(taskId, ClosingTaskStatus.SKIPPED, "rival");
                        case GATE_CREATE -> closing.createClosingGate(newGate(calendar));
                        case GATE_PASS -> closing.checkAndPassClosingGate(gateId, "rival");
                        case CLOSE -> closing.determineClosingStatus(calendar.getId(), "rival");
                    }
                });
            });
            assertThat(mutationStarted.await(5, TimeUnit.SECONDS)).isTrue();
            assertThatThrownBy(() -> contender.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            assertThat(master.writes.get()).isEqualTo(writesBefore);
            rootOwner.release();
            owner.get(10, TimeUnit.SECONDS);
            assertThat(contender.get(10, TimeUnit.SECONDS)).isInstanceOf(IllegalStateException.class);
            assertThat(calendars.findById(calendar.getId()).orElseThrow().getStatus()).isEqualTo(ClosingCalendarStatus.CLOSED);
            assertThat(tasks.findByClosingCalendar(calendar)).hasSize(taskCount);
            assertThat(gates.findByClosingCalendar(calendar)).hasSize(gateCount);
            assertThat(tasks.findById(taskId).orElseThrow().getStatus()).isEqualTo(ClosingTaskStatus.PENDING);
            if (gateId != null) assertThat(gates.findById(gateId).orElseThrow().getStatus()).isEqualTo(ClosingGateStatus.PENDING);
            assertThat(master.writes.get()).isEqualTo(writesBefore);
        } finally {
            rootOwner.release();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void concurrentCloseCallsProduceOneMasterWriteAndOneClosingAudit() throws Exception {
        ClosingCalendar calendar = readyCalendar("2051", "07");
        long periodId = master.findFiscalPeriod("2051", "07").orElseThrow().id();
        Pause pause = master.pauseUpdate(periodId);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        int writesBefore = master.writes.get();
        try {
            Future<Throwable> first = workers.submit(() -> outcome(() -> closing.determineClosingStatus(calendar.getId(), "first")));
            pause.awaitEntered();
            Future<Throwable> second = workers.submit(() -> outcome(() -> closing.determineClosingStatus(calendar.getId(), "second")));
            assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            pause.release();
            assertThat(first.get(10, TimeUnit.SECONDS)).isNull();
            assertThat(second.get(10, TimeUnit.SECONDS)).isInstanceOf(IllegalStateException.class);
            assertThat(master.writes.get()).isEqualTo(writesBefore + 1);
            assertThat(master.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isEqualTo("CLOSED");
            assertThat(calendars.findById(calendar.getId()).orElseThrow().getStatus()).isEqualTo(ClosingCalendarStatus.CLOSED);
            assertThat(audits.findAll().stream().filter(a -> a.getClosingCalendar().getId().equals(calendar.getId()))
                    .filter(a -> a.getActionType() == ActionType.CALENDAR_CLOSED)).hasSize(1);
        } finally {
            pause.release();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void gatePassRefreshesTasksAlreadyManagedBeforeAnotherTransactionCompletedThem() throws Exception {
        master.create("2051", "08", "OPEN");
        ClosingCalendar calendar = newCalendar("2051", "08", ClosingCalendarStatus.IN_PROGRESS);
        ClosingTask task = closing.createClosingTask(newTask(calendar, true));
        closing.updateClosingTaskStatus(task.getId(), ClosingTaskStatus.IN_PROGRESS, "worker");
        ClosingGate gate = closing.createClosingGate(newGate(calendar));
        ExecutorService rival = Executors.newSingleThreadExecutor();
        try {
            new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                // Load both the child and collection into this persistence context before the rival commits.
                assertThat(tasks.findByClosingCalendar(calendar).get(0).getStatus()).isEqualTo(ClosingTaskStatus.IN_PROGRESS);
                try {
                    rival.submit(() -> closing.updateClosingTaskStatus(task.getId(), ClosingTaskStatus.COMPLETED, "worker"))
                            .get(10, TimeUnit.SECONDS);
                } catch (Exception e) { throw new AssertionError(e); }
                closing.checkAndPassClosingGate(gate.getId(), "gate-reviewer");
            });
            assertThat(gates.findById(gate.getId()).orElseThrow().getStatus()).isEqualTo(ClosingGateStatus.PASSED);
        } finally {
            rival.shutdownNow();
            assertThat(rival.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void staleTaskInAmbientTransactionCannotRepeatAnAlreadyCommittedTransition() throws Exception {
        master.create("2051", "09", "OPEN");
        ClosingCalendar calendar = newCalendar("2051", "09", ClosingCalendarStatus.IN_PROGRESS);
        ClosingTask task = closing.createClosingTask(newTask(calendar, true));
        ExecutorService rival = Executors.newSingleThreadExecutor();
        try {
            assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assertThat(tasks.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(ClosingTaskStatus.PENDING);
                try {
                    rival.submit(() -> closing.updateClosingTaskStatus(task.getId(), ClosingTaskStatus.IN_PROGRESS, "first"))
                            .get(10, TimeUnit.SECONDS);
                } catch (Exception e) { throw new AssertionError(e); }
                closing.updateClosingTaskStatus(task.getId(), ClosingTaskStatus.IN_PROGRESS, "second");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(tasks.findById(task.getId()).orElseThrow().getAuditUser()).isEqualTo("first");
            assertThat(audits.findAll().stream().filter(a -> a.getClosingCalendar().getId().equals(calendar.getId()))
                    .filter(a -> a.getActionType() == ActionType.TASK_STATUS_CHANGED)).hasSize(1);
        } finally {
            rival.shutdownNow();
            assertThat(rival.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void staleGateCannotAppendASecondPassAuditAfterAnotherTransactionWins() throws Exception {
        ClosingCalendar calendar = readyCalendar("2051", "10");
        ClosingGate gate = closing.createClosingGate(newGate(calendar));
        ExecutorService rival = Executors.newSingleThreadExecutor();
        try {
            assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                assertThat(gates.findById(gate.getId()).orElseThrow().getStatus()).isEqualTo(ClosingGateStatus.PENDING);
                try {
                    rival.submit(() -> closing.checkAndPassClosingGate(gate.getId(), "first"))
                            .get(10, TimeUnit.SECONDS);
                } catch (Exception e) { throw new AssertionError(e); }
                closing.checkAndPassClosingGate(gate.getId(), "second");
            })).isInstanceOf(IllegalStateException.class);
            assertThat(gates.findById(gate.getId()).orElseThrow().getPassedBy()).isEqualTo("first");
            assertThat(audits.findAll().stream().filter(a -> a.getClosingCalendar().getId().equals(calendar.getId()))
                    .filter(a -> a.getActionType() == ActionType.GATE_PASSED && a.getActionUser().equals("first"))).hasSize(1);
            assertThat(audits.findAll().stream().filter(a -> a.getClosingCalendar().getId().equals(calendar.getId()))
                    .filter(a -> a.getActionUser().equals("second"))).isEmpty();
        } finally {
            rival.shutdownNow();
            assertThat(rival.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void staleCalendarAndChecklistCannotAuthorizeMutationAfterAnotherTransactionCloses() throws Exception {
        ClosingCalendar calendar = readyCalendar("2051", "11");
        ExecutorService rival = Executors.newSingleThreadExecutor();
        try {
            assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                ClosingCalendar stale = calendars.findById(calendar.getId()).orElseThrow();
                assertThat(stale.getStatus()).isEqualTo(ClosingCalendarStatus.IN_PROGRESS);
                assertThat(tasks.findByClosingCalendar(stale)).hasSize(1);
                assertThat(gates.findByClosingCalendar(stale)).hasSize(1);
                try {
                    rival.submit(() -> closing.determineClosingStatus(calendar.getId(), "winner"))
                            .get(10, TimeUnit.SECONDS);
                } catch (Exception e) { throw new AssertionError(e); }
                closing.createClosingTask(newTask(stale, true));
            })).isInstanceOf(IllegalStateException.class);
            assertThat(calendars.findById(calendar.getId()).orElseThrow().getStatus()).isEqualTo(ClosingCalendarStatus.CLOSED);
            assertThat(tasks.findByClosingCalendar(calendar)).hasSize(1);
            assertThat(master.findFiscalPeriod("2051", "11").orElseThrow().closingStatus()).isEqualTo("CLOSED");
        } finally {
            rival.shutdownNow();
            assertThat(rival.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    ClosingCalendar readyCalendar(String year, String period) {
        master.create(year, period, "OPEN");
        ClosingCalendar calendar = newCalendar(year, period, ClosingCalendarStatus.IN_PROGRESS);
        ClosingTask task = closing.createClosingTask(newTask(calendar, true));
        closing.updateClosingTaskStatus(task.getId(), ClosingTaskStatus.IN_PROGRESS, "fixture");
        closing.updateClosingTaskStatus(task.getId(), ClosingTaskStatus.COMPLETED, "fixture");
        ClosingGate gate = closing.createClosingGate(newGate(calendar));
        closing.checkAndPassClosingGate(gate.getId(), "fixture");
        var fiscalPeriod = master.findFiscalPeriod(year, period).orElseThrow();
        FinalCloseEvidenceFixtures.recordValid(
                finalCloseEvidence, calendar, fiscalPeriod.id(), fiscalPeriod.endDate());
        return calendar;
    }

    ClosingCalendar newCalendar(String year, String period, ClosingCalendarStatus state) {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear(year);
        calendar.setFiscalPeriod(period);
        calendar.setStatus(state);
        if (state == ClosingCalendarStatus.IN_PROGRESS) {
            calendar.setCloseInitiatedBy("synthetic-fixture");
            calendar.setCloseInitiatedAt(java.time.LocalDateTime.now(java.time.ZoneOffset.UTC).minusSeconds(1));
        }
        calendar.setAuditUser("synthetic-fixture");
        return calendars.save(calendar);
    }

    static ClosingTask newTask(ClosingCalendar calendar, boolean mandatory) {
        ClosingTask task = new ClosingTask();
        task.setClosingCalendar(calendar);
        task.setName("Synthetic reconciliation");
        task.setMandatory(mandatory);
        task.setTaskOrder(1);
        return task;
    }

    static ClosingGate newGate(ClosingCalendar calendar) {
        ClosingGate gate = new ClosingGate();
        gate.setClosingCalendar(calendar);
        gate.setName("Synthetic evidence");
        return gate;
    }

    Fixture closedFixture(String year, String period) {
        long periodId = master.create(year, period, "CLOSED");
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear(year);
        calendar.setFiscalPeriod(period);
        calendar.setStatus(ClosingCalendarStatus.CLOSED);
        calendar.setAuditUser("synthetic-fixture");
        calendar = calendars.save(calendar);
        ReopenApproval approval = closing.requestPeriodReopen(periodId, "requester", "synthetic correction");
        return new Fixture(periodId, calendar.getId(), approval.getId());
    }

    java.util.List<ActionType> decisionAudits(Long calendarId) {
        return audits.findAll().stream().filter(a -> a.getClosingCalendar().getId().equals(calendarId))
                .map(a -> a.getActionType())
                .filter(a -> a == ActionType.REOPEN_APPROVED || a == ActionType.REOPEN_REJECTED).toList();
    }

    static Throwable outcome(Runnable work) {
        try { work.run(); return null; }
        catch (Throwable failure) { return failure; }
    }

    record Fixture(long periodId, long calendarId, long approvalId) { }

    static final class Pause {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch released = new CountDownLatch(1);
        void stop() {
            entered.countDown();
            try {
                if (!released.await(10, TimeUnit.SECONDS)) throw new IllegalStateException("Test barrier timed out");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Test barrier interrupted", e);
            }
        }
        void awaitEntered() throws InterruptedException { assertThat(entered.await(10, TimeUnit.SECONDS)).isTrue(); }
        void release() { released.countDown(); }
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean @Primary ControlledMaster controlledMaster() { return new ControlledMaster(); }
        @Bean @Primary JournalQueryPort controlledJournalQuery() { return mock(JournalQueryPort.class); }
    }

    static final class ControlledMaster implements FiscalPeriodControlPort {
        // This independent datasource deliberately cannot join the Closing transaction.
        private final JdbcTemplate jdbc = new JdbcTemplate(new DriverManagerDataSource(
                "jdbc:h2:mem:closing-synthetic-master-" + java.util.UUID.randomUUID() + ";DB_CLOSE_DELAY=-1", "sa", ""));
        private final AtomicInteger ids = new AtomicInteger();
        final AtomicInteger writes = new AtomicInteger();
        final AtomicInteger attempts = new AtomicInteger();
        private final java.util.Map<Long, Fault> faults = new java.util.concurrent.ConcurrentHashMap<>();
        volatile long wrongReadIdentityPeriod;
        volatile long pausedPeriod;
        volatile Pause updatePause;
        private final java.util.Map<String, ReadPause> readPauses = new java.util.concurrent.ConcurrentHashMap<>();

        ControlledMaster() {
            jdbc.execute("create table periods(id bigint primary key, fiscal_year varchar(4), fiscal_period varchar(20), status varchar(30))");
        }
        long create(String year, String period, String status) {
            long id = ids.incrementAndGet();
            jdbc.update("insert into periods values (?,?,?,?)", id, year, period, status);
            return id;
        }
        Pause pauseUpdate(long id) {
            pausedPeriod = id;
            updatePause = new Pause();
            return updatePause;
        }
        Pause pauseRead(long id, String thread, int ordinal) {
            ReadPause read = new ReadPause(id, ordinal, new AtomicInteger(), new Pause());
            readPauses.put(thread, read);
            return read.pause();
        }
        void failNextUpdate(long id, Fault fault) { faults.put(id, fault); }
        @Override public Optional<FiscalPeriodRef> findFiscalPeriodById(Long id) {
            Optional<FiscalPeriodRef> result = jdbc.query("select * from periods where id=?", (rs, row) -> ref(rs.getLong("id"),
                    rs.getString("fiscal_year"), rs.getString("fiscal_period"), rs.getString("status")), id).stream().findFirst();
            ReadPause read = readPauses.get(Thread.currentThread().getName());
            if (read != null && id == read.periodId() && read.calls().incrementAndGet() == read.ordinal()) read.pause().stop();
            if (id == wrongReadIdentityPeriod) return result.map(this::wrongIdentity);
            return result;
        }
        @Override public Optional<FiscalPeriodRef> findFiscalPeriod(String year, String period) {
            return jdbc.query("select * from periods where fiscal_year=? and fiscal_period=?", (rs, row) -> ref(rs.getLong("id"),
                    rs.getString("fiscal_year"), rs.getString("fiscal_period"), rs.getString("status")), year, period).stream().findFirst();
        }
        @Override public FiscalPeriodRef updateClosingStatus(Long id, String status, String actor) {
            attempts.incrementAndGet();
            Pause pause = updatePause;
            if (id == pausedPeriod && pause != null) pause.stop();
            Fault fault = faults.remove(id);
            if (fault == Fault.BEFORE_REMOTE_WRITE) throw new IllegalStateException("Synthetic transport unavailable before apply");
            jdbc.update("update periods set status=? where id=?", status, id);
            writes.incrementAndGet();
            if (fault == Fault.LOST_RESPONSE) throw new IllegalStateException("Synthetic response lost after committed Master write");
            if (fault == Fault.LOCAL_FINALIZE_ROLLBACK) {
                // The remote write has independently committed. Only the enclosing Closing
                // completion transaction is rolled back, including its terminal audit insert.
                org.springframework.transaction.support.TransactionSynchronizationManager.registerSynchronization(
                        new org.springframework.transaction.support.TransactionSynchronization() {
                            @Override public void beforeCommit(boolean readOnly) {
                                throw new IllegalStateException("Synthetic local commit failure after Master success");
                            }
                        });
            }
            FiscalPeriodRef result = findFiscalPeriodById(id).orElseThrow();
            return fault == Fault.WRONG_RESPONSE_ID ? wrongIdentity(result) : result;
        }
        enum Fault { BEFORE_REMOTE_WRITE, LOST_RESPONSE, LOCAL_FINALIZE_ROLLBACK, WRONG_RESPONSE_ID }
        private FiscalPeriodRef wrongIdentity(FiscalPeriodRef period) {
            return new FiscalPeriodRef(period.id() + 10000, period.fiscalYear(), period.fiscalPeriod(),
                    period.startDate(), period.endDate(), period.closingStatus());
        }
        private FiscalPeriodRef ref(long id, String year, String period, String status) {
            LocalDate start = LocalDate.of(Integer.parseInt(year), Integer.parseInt(period), 1);
            return new FiscalPeriodRef(id, year, period, start, start.withDayOfMonth(start.lengthOfMonth()), status);
        }
        private record ReadPause(long periodId, int ordinal, AtomicInteger calls, Pause pause) { }
    }
}
