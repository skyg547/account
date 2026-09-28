package com.ho.account.closing;

import com.ho.account.closing.ClosingAggregateConcurrencyIntegrationTest.ControlledMaster;
import com.ho.account.closing.ClosingAggregateConcurrencyIntegrationTest.ControlledMaster.Fault;
import com.ho.account.closing.ClosingAggregateConcurrencyIntegrationTest.Fixture;
import com.ho.account.closing.ClosingAggregateConcurrencyIntegrationTest.Pause;
import com.ho.account.closing.application.port.in.ClosingAdmissionQuery;
import com.ho.account.closing.application.port.in.ClosingTransitionRecoveryUseCase;
import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.ReopenApprovalPersistencePort;
import com.ho.account.closing.application.service.ClosingTransitionPendingException;
import com.ho.account.closing.application.service.ClosingTransitionTransactions;
import com.ho.account.closing.domain.ClosingAuditLog;
import com.ho.account.closing.domain.ClosingAuditLog.ActionType;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingCalendar.TransitionStage;
import com.ho.account.closing.domain.ClosingGate;
import com.ho.account.closing.domain.ClosingTask;
import com.ho.account.closing.domain.ClosingTask.ClosingTaskStatus;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.infrastructure.persistence.ClosingAuditLogRepository;
import com.ho.account.closing.web.ClosingController;
import com.ho.account.closing.web.ClosingExceptionHandler;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static com.ho.account.closing.ClosingAggregateConcurrencyIntegrationTest.newGate;
import static com.ho.account.closing.ClosingAggregateConcurrencyIntegrationTest.newTask;
import static com.ho.account.closing.ClosingAggregateConcurrencyIntegrationTest.outcome;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/** Synthetic fault injection crosses two independent durable H2 transaction boundaries. */
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
class ClosingTransitionRecoveryIntegrationTest {
    @Autowired ClosingUseCase closing;
    @Autowired ClosingTransitionRecoveryUseCase recovery;
    @Autowired ClosingAdmissionQuery admission;
    @Autowired ClosingCalendarPersistencePort calendars;
    @Autowired ReopenApprovalPersistencePort approvals;
    @Autowired ClosingAuditLogRepository audits;
    @Autowired ControlledMaster master;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired ClosingController controller;
    @Autowired ClosingTransitionTransactions transactions;

    @ParameterizedTest
    @ValueSource(strings = {"OPEN", "CLOSED"})
    void preparedOperationSurvivesCoordinatorExitAndCanDispatchExactlyOnce(String target) {
        int attemptsBefore = master.attempts.get();
        ClosingCalendar prepared;
        long periodId;
        if (target.equals("OPEN")) {
            Fixture fixture = closedFixture("2055", "01");
            periodId = fixture.periodId();
            prepared = transactions.prepareDecision(fixture.approvalId(), ReopenApprovalStatus.APPROVED, "approver").calendar();
            assertThat(approvals.findById(fixture.approvalId()).orElseThrow().getStatus()).isEqualTo(ReopenApprovalStatus.APPROVED);
        } else {
            periodId = master.create("2055", "02", "OPEN");
            ClosingCalendar calendar = newCalendar("2055", "02", ClosingCalendarStatus.IN_PROGRESS);
            completeChecklist(calendar);
            prepared = transactions.prepareClose(calendar.getId(), "closer");
        }
        // Returning from the proxied prepare helper commits intent. Skipping the coordinator's
        // next call models process exit before DISPATCHED, without mocking any persistence layer.
        assertThat(calendars.findById(prepared.getId()).orElseThrow().getTransitionStage()).isEqualTo(TransitionStage.PREPARED);
        assertThat(master.attempts.get()).isEqualTo(attemptsBefore);
        assertThat(master.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isNotEqualTo(target);

        recovery.recoverTransition(prepared.getId(), prepared.getTransitionId(), false, "prepared-recovery-operator");

        ClosingCalendar finished = calendars.findById(prepared.getId()).orElseThrow();
        assertThat(finished.getStatus().name()).isEqualTo(target);
        assertThat(finished.getTransitionId()).isNull();
        assertThat(master.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isEqualTo(target);
        assertThat(master.attempts.get()).isEqualTo(attemptsBefore + 1);
        ActionType terminal = target.equals("OPEN") ? ActionType.REOPEN_APPROVED : ActionType.CALENDAR_CLOSED;
        assertThat(calendarAudits(prepared.getId()).stream().filter(a -> a.getActionType() == terminal)).hasSize(1);
        assertThat(calendarAudits(prepared.getId()).stream()
                .filter(a -> a.getActionType() == ActionType.PERIOD_TRANSITION_RECOVERED)).singleElement().satisfies(audit -> {
                    assertThat(audit.getActionUser()).isEqualTo("prepared-recovery-operator");
                    assertThat(audit.getActionReason()).contains("prepared operation resumed before dispatch")
                            .doesNotContain("termination confirmed");
                });
        assertThatThrownBy(() -> recovery.recoverTransition(prepared.getId(), prepared.getTransitionId(), false, "duplicate"))
                .isInstanceOf(IllegalStateException.class);
        assertThat(master.attempts.get()).isEqualTo(attemptsBefore + 1);
    }

    @Test
    void concurrentPreparedRecoveriesClaimOnlyOneRemoteDispatch() throws Exception {
        Fixture fixture = closedFixture("2055", "03");
        ClosingCalendar prepared = transactions.prepareDecision(
                fixture.approvalId(), ReopenApprovalStatus.APPROVED, "approver").calendar();
        Pause put = master.pauseUpdate(fixture.periodId());
        var start = new java.util.concurrent.CyclicBarrier(2);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        int attemptsBefore = master.attempts.get();
        try {
            java.util.concurrent.Callable<Throwable> resume = () -> {
                start.await(5, TimeUnit.SECONDS);
                return outcome(() -> recovery.recoverTransition(
                        fixture.calendarId(), prepared.getTransitionId(), false, "recovery-operator"));
            };
            Future<Throwable> first = workers.submit(resume);
            Future<Throwable> second = workers.submit(resume);
            put.awaitEntered();
            put.release();
            List<Throwable> results = java.util.Arrays.asList(first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS));
            assertThat(results.stream().filter(java.util.Objects::isNull)).hasSize(1);
            assertThat(results.stream().filter(java.util.Objects::nonNull)).singleElement()
                    .isInstanceOf(RuntimeException.class);
            assertThat(master.attempts.get()).isEqualTo(attemptsBefore + 1);
            assertRecoveredReopen(fixture, prepared.getTransitionId());
            assertThat(calendarAudits(fixture.calendarId()).stream()
                    .filter(a -> a.getActionType() == ActionType.PERIOD_TRANSITION_RECOVERED)).singleElement()
                    .satisfies(audit -> assertThat(audit.getActionReason())
                            .contains("prepared operation resumed before dispatch").doesNotContain("termination confirmed"));
        } finally {
            put.release();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void ambiguousHttpDecisionReturns503AndItsDurableWinnerMakesRejection409() throws Exception {
        Fixture fixture = closedFixture("2053", "08");
        master.failNextUpdate(fixture.periodId(), Fault.LOST_RESPONSE);
        var http = MockMvcBuilders.standaloneSetup(controller).setControllerAdvice(new ClosingExceptionHandler()).build();

        http.perform(put("/api/closing/reopen-approvals/{id}/status", fixture.approvalId())
                        .header("X-Auth-User", "approver")
                        .header("X-Auth-Roles", "CLOSING_MANAGER")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\",\"approvedBy\":\"approver\"}"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PERIOD_TRANSITION_RECOVERY_REQUIRED"));

        ClosingCalendar pending = assertPendingReopen(fixture, "OPEN");
        http.perform(put("/api/closing/reopen-approvals/{id}/status", fixture.approvalId())
                        .header("X-Auth-User", "rejector")
                        .header("X-Auth-Roles", "CLOSING_MANAGER")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"REJECTED\",\"approvedBy\":\"rejector\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("WORKFLOW_STATE_CONFLICT"));

        recovery.recoverTransition(fixture.calendarId(), pending.getTransitionId(), true, "recovery-operator");
        assertRecoveredReopen(fixture, pending.getTransitionId());
    }

    @ParameterizedTest
    @EnumSource(value = Fault.class, names = {"LOST_RESPONSE", "LOCAL_FINALIZE_ROLLBACK", "WRONG_RESPONSE_ID"})
    void committedMasterReopenRetainsDecisionAndRecoversWithoutAnotherWrite(Fault fault) {
        Fixture fixture = closedFixture("2052", String.format(java.util.Locale.ROOT, "%02d", fault.ordinal() + 1));
        int attemptsBefore = master.attempts.get();
        int writesBefore = master.writes.get();
        master.failNextUpdate(fixture.periodId(), fault);

        assertThatThrownBy(() -> approve(fixture)).isInstanceOf(ClosingTransitionPendingException.class);

        ClosingCalendar pending = assertPendingReopen(fixture, "OPEN");
        String operationId = pending.getTransitionId();
        assertThat(master.writes.get()).isEqualTo(writesBefore + 1);
        assertThat(terminalAudits(fixture.calendarId())).isEmpty();
        assertThatThrownBy(() -> recovery.recoverTransition(fixture.calendarId(), operationId, false, "recovery-operator"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("terminated");
        assertFencedMutations(fixture);

        recovery.recoverTransition(fixture.calendarId(), operationId, true, "recovery-operator");

        assertRecoveredReopen(fixture, operationId);
        assertThat(master.attempts.get()).isEqualTo(attemptsBefore + 1);
        assertThat(master.writes.get()).isEqualTo(writesBefore + 1);
        assertThatThrownBy(() -> recovery.recoverTransition(fixture.calendarId(), operationId, true, "late-retry"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> approve(fixture)).isInstanceOf(IllegalStateException.class);
        assertThat(master.attempts.get()).isEqualTo(attemptsBefore + 1);
        assertThat(terminalAudits(fixture.calendarId())).hasSize(1);
    }

    @Test
    void unresolvedDispatchedRequestRemainsFencedAndRecoveryNeverReplaysSourceState() {
        Fixture fixture = closedFixture("2053", "01");
        int attemptsBefore = master.attempts.get();
        int writesBefore = master.writes.get();
        master.failNextUpdate(fixture.periodId(), Fault.BEFORE_REMOTE_WRITE);
        assertThatThrownBy(() -> approve(fixture)).isInstanceOf(ClosingTransitionPendingException.class);
        ClosingCalendar pending = assertPendingReopen(fixture, "CLOSED");

        for (int retry = 0; retry < 2; retry++) {
            assertThatThrownBy(() -> recovery.recoverTransition(
                    fixture.calendarId(), pending.getTransitionId(), true, "recovery-operator"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("must be OPEN");
        }

        assertPendingReopen(fixture, "CLOSED");
        assertFencedMutations(fixture);
        assertThat(master.attempts.get()).isEqualTo(attemptsBefore + 1);
        assertThat(master.writes.get()).isEqualTo(writesBefore);
        assertThat(terminalAudits(fixture.calendarId())).isEmpty();
        assertThat(calendarAudits(fixture.calendarId()).stream()
                .filter(a -> a.getActionType() == ActionType.PERIOD_TRANSITION_RECOVERED)).isEmpty();
    }

    @Test
    void recoveryCannotOvertakeActivePutAndDoesNotReplayAfterLostResponse() throws Exception {
        Fixture fixture = closedFixture("2053", "02");
        Pause originalPut = master.pauseUpdate(fixture.periodId());
        master.failNextUpdate(fixture.periodId(), Fault.LOST_RESPONSE);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        int attemptsBefore = master.attempts.get();
        try {
            Future<Throwable> original = workers.submit(() -> outcome(() -> approve(fixture)));
            originalPut.awaitEntered();
            String operationId = calendars.findById(fixture.calendarId()).orElseThrow().getTransitionId();
            assertThat(operationId).isNotBlank();
            Future<Throwable> recover = workers.submit(() -> outcome(() -> recovery.recoverTransition(
                    fixture.calendarId(), operationId, true, "recovery-operator")));
            assertThatThrownBy(() -> recover.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            assertThat(master.findFiscalPeriodById(fixture.periodId()).orElseThrow().closingStatus()).isEqualTo("CLOSED");
            originalPut.release();
            assertThat(original.get(10, TimeUnit.SECONDS)).isInstanceOf(ClosingTransitionPendingException.class);
            assertThat(recover.get(10, TimeUnit.SECONDS)).isNull();
            assertRecoveredReopen(fixture, operationId);
            assertThat(master.attempts.get()).isEqualTo(attemptsBefore + 1);
        } finally {
            originalPut.release();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void concurrentRecoveriesAppendOnlyOneTerminalDecisionAndRecoveryAudit() throws Exception {
        Fixture fixture = closedFixture("2053", "03");
        master.failNextUpdate(fixture.periodId(), Fault.LOST_RESPONSE);
        assertThatThrownBy(() -> approve(fixture)).isInstanceOf(ClosingTransitionPendingException.class);
        String operationId = calendars.findById(fixture.calendarId()).orElseThrow().getTransitionId();
        Pause firstRead = master.pauseRead(fixture.periodId(), "first-recovery", 1);
        ExecutorService workers = Executors.newFixedThreadPool(2);
        int attemptsAfterOriginal = master.attempts.get();
        try {
            Future<Throwable> first = workers.submit(() -> {
                Thread.currentThread().setName("first-recovery");
                return outcome(() -> recovery.recoverTransition(fixture.calendarId(), operationId, true, "recovery-operator"));
            });
            firstRead.awaitEntered();
            Future<Throwable> second = workers.submit(() -> outcome(() -> recovery.recoverTransition(
                    fixture.calendarId(), operationId, true, "second-recovery")));
            assertThatThrownBy(() -> second.get(300, TimeUnit.MILLISECONDS))
                    .isInstanceOf(java.util.concurrent.TimeoutException.class);
            firstRead.release();
            assertThat(first.get(10, TimeUnit.SECONDS)).isNull();
            assertThat(second.get(10, TimeUnit.SECONDS)).isInstanceOf(IllegalStateException.class);
            assertRecoveredReopen(fixture, operationId);
            assertThat(master.attempts.get()).isEqualTo(attemptsAfterOriginal);
        } finally {
            firstRead.release();
            workers.shutdownNow();
            assertThat(workers.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
    }

    @Test
    void wrongMasterIdentityCannotClearTheDurableFence() {
        Fixture fixture = closedFixture("2053", "04");
        master.failNextUpdate(fixture.periodId(), Fault.LOST_RESPONSE);
        assertThatThrownBy(() -> approve(fixture)).isInstanceOf(ClosingTransitionPendingException.class);
        String operationId = calendars.findById(fixture.calendarId()).orElseThrow().getTransitionId();
        master.wrongReadIdentityPeriod = fixture.periodId();
        try {
            assertThatThrownBy(() -> recovery.recoverTransition(fixture.calendarId(), operationId, true, "recovery-operator"))
                    .isInstanceOf(IllegalStateException.class).hasMessageContaining("another fiscal period");
            assertThat(calendars.findById(fixture.calendarId()).orElseThrow().getTransitionId()).isEqualTo(operationId);
            assertThat(terminalAudits(fixture.calendarId())).isEmpty();
        } finally {
            master.wrongReadIdentityPeriod = 0;
        }
        recovery.recoverTransition(fixture.calendarId(), operationId, true, "recovery-operator");
        assertRecoveredReopen(fixture, operationId);
    }

    @Test
    void completedOldOperationCannotFinalizeALaterReopenCycle() {
        Fixture first = closedFixture("2053", "05");
        master.failNextUpdate(first.periodId(), Fault.LOST_RESPONSE);
        assertThatThrownBy(() -> approve(first)).isInstanceOf(ClosingTransitionPendingException.class);
        String oldOperation = calendars.findById(first.calendarId()).orElseThrow().getTransitionId();
        recovery.recoverTransition(first.calendarId(), oldOperation, true, "recovery-operator");

        ClosingCalendar calendar = calendars.findById(first.calendarId()).orElseThrow();
        closing.updateClosingCalendarStatus(calendar.getId(), ClosingCalendarStatus.IN_PROGRESS, "closer");
        completeChecklist(calendar);
        closing.determineClosingStatus(calendar.getId(), "closer");
        ReopenApproval next = closing.requestPeriodReopen(first.periodId(), "next-requester", "second correction");
        Fixture second = new Fixture(first.periodId(), first.calendarId(), next.getId());
        master.failNextUpdate(second.periodId(), Fault.LOST_RESPONSE);
        assertThatThrownBy(() -> approve(second)).isInstanceOf(ClosingTransitionPendingException.class);
        String currentOperation = calendars.findById(second.calendarId()).orElseThrow().getTransitionId();
        int attemptsAfterSecond = master.attempts.get();
        assertThat(currentOperation).isNotEqualTo(oldOperation);

        assertThatThrownBy(() -> recovery.recoverTransition(second.calendarId(), oldOperation, true, "late-old-worker"))
                .isInstanceOf(IllegalStateException.class);

        assertThat(calendars.findById(second.calendarId()).orElseThrow().getTransitionId()).isEqualTo(currentOperation);
        assertThat(master.attempts.get()).isEqualTo(attemptsAfterSecond);
        recovery.recoverTransition(second.calendarId(), currentOperation, true, "recovery-operator");
        assertThat(calendars.findById(second.calendarId()).orElseThrow().getStatus()).isEqualTo(ClosingCalendarStatus.OPEN);
        assertThat(master.findFiscalPeriodById(second.periodId()).orElseThrow().closingStatus()).isEqualTo("OPEN");
        assertThat(terminalAudits(second.calendarId())).hasSize(2);
        assertThat(master.attempts.get()).isEqualTo(attemptsAfterSecond);
    }

    @ParameterizedTest
    @EnumSource(value = Fault.class, names = {"LOST_RESPONSE", "LOCAL_FINALIZE_ROLLBACK"})
    void closeFailureRecoversTheSameCommittedMasterTargetWithoutReplay(Fault fault) {
        String period = String.format(java.util.Locale.ROOT, "%02d", fault.ordinal() + 1);
        long periodId = master.create("2054", period, "OPEN");
        ClosingCalendar calendar = newCalendar("2054", period, ClosingCalendarStatus.IN_PROGRESS);
        completeChecklist(calendar);
        master.failNextUpdate(periodId, fault);
        int attemptsBefore = master.attempts.get();

        assertThatThrownBy(() -> closing.determineClosingStatus(calendar.getId(), "closer"))
                .isInstanceOf(ClosingTransitionPendingException.class);
        ClosingCalendar pending = calendars.findById(calendar.getId()).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(ClosingCalendarStatus.IN_PROGRESS);
        assertThat(pending.getTransitionStage()).isEqualTo(TransitionStage.DISPATCHED);
        assertThat(pending.getTransitionTarget()).isEqualTo("CLOSED");
        assertThat(master.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isEqualTo("CLOSED");
        assertThat(admission.isClosed(LocalDate.of(2054, Integer.parseInt(period), 15))).isTrue();
        assertThat(calendarAudits(calendar.getId()).stream().filter(a -> a.getActionType() == ActionType.CALENDAR_CLOSED)).isEmpty();

        recovery.recoverTransition(calendar.getId(), pending.getTransitionId(), true, "recovery-operator");

        ClosingCalendar recovered = calendars.findById(calendar.getId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(ClosingCalendarStatus.CLOSED);
        assertThat(recovered.getTransitionId()).isNull();
        assertThat(recovered.getClosedBy()).isEqualTo("closer");
        assertThat(master.attempts.get()).isEqualTo(attemptsBefore + 1);
        assertThat(calendarAudits(calendar.getId()).stream().filter(a -> a.getActionType() == ActionType.CALENDAR_CLOSED)).hasSize(1);
    }

    @Test
    void ambientTransactionCannotHideAnIrrevocableRemoteDecisionBehindLaterRollback() {
        Fixture fixture = closedFixture("2053", "06");
        int attemptsBefore = master.attempts.get();
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> approve(fixture)))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(approvals.findById(fixture.approvalId()).orElseThrow().getStatus()).isEqualTo(ReopenApprovalStatus.PENDING);
        assertThat(calendars.findById(fixture.calendarId()).orElseThrow().getTransitionId()).isNull();
        assertThat(master.attempts.get()).isEqualTo(attemptsBefore);

        long periodId = master.create("2053", "07", "OPEN");
        ClosingCalendar calendar = newCalendar("2053", "07", ClosingCalendarStatus.IN_PROGRESS);
        completeChecklist(calendar);
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            calendars.findById(calendar.getId());
            closing.findClosingTasksByCalendarId(calendar.getId());
            closing.determineClosingStatus(calendar.getId(), "closer");
        })).isInstanceOf(IllegalTransactionStateException.class);
        assertThat(master.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isEqualTo("OPEN");
        assertThat(calendars.findById(calendar.getId()).orElseThrow().getTransitionId()).isNull();
        assertThat(master.attempts.get()).isEqualTo(attemptsBefore);
    }

    private Fixture closedFixture(String year, String period) {
        long periodId = master.create(year, period, "CLOSED");
        ClosingCalendar calendar = newCalendar(year, period, ClosingCalendarStatus.CLOSED);
        ReopenApproval approval = closing.requestPeriodReopen(periodId, "requester", "synthetic correction");
        return new Fixture(periodId, calendar.getId(), approval.getId());
    }

    private ClosingCalendar newCalendar(String year, String period, ClosingCalendarStatus state) {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear(year);
        calendar.setFiscalPeriod(period);
        calendar.setStatus(state);
        calendar.setAuditUser("synthetic-fixture");
        return calendars.save(calendar);
    }

    private void completeChecklist(ClosingCalendar calendar) {
        ClosingTask task = closing.createClosingTask(newTask(calendar, true));
        closing.updateClosingTaskStatus(task.getId(), ClosingTaskStatus.IN_PROGRESS, "task-worker");
        closing.updateClosingTaskStatus(task.getId(), ClosingTaskStatus.COMPLETED, "task-worker");
        ClosingGate gate = closing.createClosingGate(newGate(calendar));
        closing.checkAndPassClosingGate(gate.getId(), "gate-reviewer");
    }

    private void approve(Fixture fixture) {
        closing.updateReopenApprovalStatus(fixture.approvalId(), ReopenApprovalStatus.APPROVED, "approver");
    }

    private ClosingCalendar assertPendingReopen(Fixture fixture, String masterStatus) {
        ClosingCalendar pending = calendars.findById(fixture.calendarId()).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(ClosingCalendarStatus.CLOSED);
        assertThat(pending.getTransitionId()).isNotBlank();
        assertThat(pending.getTransitionStage()).isEqualTo(TransitionStage.DISPATCHED);
        assertThat(pending.getTransitionTarget()).isEqualTo("OPEN");
        assertThat(pending.getTransitionApprovalId()).isEqualTo(fixture.approvalId());
        assertThat(pending.getTransitionActor()).isEqualTo("approver");
        ReopenApproval approval = approvals.findById(fixture.approvalId()).orElseThrow();
        assertThat(approval.getStatus()).isEqualTo(ReopenApprovalStatus.APPROVED);
        assertThat(approval.getApprovedBy()).isEqualTo("approver");
        assertThat(master.findFiscalPeriodById(fixture.periodId()).orElseThrow().closingStatus()).isEqualTo(masterStatus);
        assertThat(admission.isClosed(date(pending))).isTrue();
        return pending;
    }

    private void assertFencedMutations(Fixture fixture) {
        ClosingCalendar calendar = calendars.findById(fixture.calendarId()).orElseThrow();
        assertThatThrownBy(() -> closing.updateReopenApprovalStatus(fixture.approvalId(), ReopenApprovalStatus.REJECTED, "rejector"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> closing.updateClosingCalendarStatus(fixture.calendarId(), ClosingCalendarStatus.IN_PROGRESS, "rival"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> closing.createClosingTask(newTask(calendar, true))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> closing.createClosingGate(newGate(calendar))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> closing.determineClosingStatus(fixture.calendarId(), "rival"))
                .isInstanceOf(IllegalStateException.class);
    }

    private void assertRecoveredReopen(Fixture fixture, String operationId) {
        ClosingCalendar recovered = calendars.findById(fixture.calendarId()).orElseThrow();
        assertThat(recovered.getStatus()).isEqualTo(ClosingCalendarStatus.OPEN);
        assertThat(recovered.getTransitionId()).isNull();
        assertThat(recovered.getReopenedBy()).isEqualTo("approver");
        assertThat(approvals.findById(fixture.approvalId()).orElseThrow().getStatus()).isEqualTo(ReopenApprovalStatus.APPROVED);
        assertThat(master.findFiscalPeriodById(fixture.periodId()).orElseThrow().closingStatus()).isEqualTo("OPEN");
        assertThat(admission.isClosed(date(recovered))).isFalse();
        assertThat(terminalAudits(fixture.calendarId())).singleElement().satisfies(audit -> {
            assertThat(audit.getActionUser()).isEqualTo("approver");
            assertThat(audit.getActionReason()).contains(operationId).contains("decisionActor=approver");
        });
        assertThat(calendarAudits(fixture.calendarId()).stream()
                .filter(a -> a.getActionType() == ActionType.PERIOD_TRANSITION_RECOVERED)).singleElement().satisfies(audit -> {
                    assertThat(audit.getActionUser()).isEqualTo("recovery-operator");
                    assertThat(audit.getActionReason()).contains(operationId).contains("decisionActor=approver");
                });
    }

    private List<ClosingAuditLog> terminalAudits(long calendarId) {
        return calendarAudits(calendarId).stream().filter(a -> a.getActionType() == ActionType.REOPEN_APPROVED).toList();
    }

    private List<ClosingAuditLog> calendarAudits(long calendarId) {
        return audits.findAll().stream().filter(a -> a.getClosingCalendar().getId().equals(calendarId)).toList();
    }

    private LocalDate date(ClosingCalendar calendar) {
        return LocalDate.of(Integer.parseInt(calendar.getFiscalYear()), Integer.parseInt(calendar.getFiscalPeriod()), 15);
    }
}
