package com.ho.account.masterdata.api.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import com.ho.account.closing.application.port.in.ClosingAdmissionQuery;
import com.ho.account.closing.application.port.out.ClosingAggregatePersistencePort;
import com.ho.account.closing.application.port.out.ClosingAuditLogPersistencePort;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.ClosingGatePersistencePort;
import com.ho.account.closing.application.port.out.ClosingTaskPersistencePort;
import com.ho.account.closing.application.port.out.ReopenApprovalPersistencePort;
import com.ho.account.closing.application.service.ClosingAdmissionService;
import com.ho.account.closing.application.service.ClosingPeriodTransitionService;
import com.ho.account.closing.application.service.ClosingTransitionPendingException;
import com.ho.account.closing.application.service.ClosingTransitionTransactions;
import com.ho.account.closing.domain.ClosingAuditLog.ActionType;
import com.ho.account.closing.domain.ClosingAuditLog;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingCalendar.TransitionStage;
import com.ho.account.closing.domain.ClosingGate;
import com.ho.account.closing.domain.ClosingTask;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.infrastructure.external.HttpFiscalPeriodControlAdapter;
import com.ho.account.closing.infrastructure.persistence.ClosingAuditLogPersistenceAdapter;
import com.ho.account.closing.infrastructure.persistence.ClosingAuditLogRepository;
import com.ho.account.closing.infrastructure.persistence.ClosingCalendarRepository;
import com.ho.account.closing.infrastructure.persistence.ClosingTaskRepository;
import com.ho.account.closing.infrastructure.persistence.ClosingGateRepository;
import com.ho.account.closing.infrastructure.persistence.ReopenApprovalRepository;
import com.ho.account.closing.infrastructure.persistence.JpaClosingAggregatePersistenceAdapter;
import com.ho.account.closing.infrastructure.persistence.JpaClosingCalendarPersistenceAdapter;
import com.ho.account.closing.infrastructure.persistence.JpaClosingGatePersistenceAdapter;
import com.ho.account.closing.infrastructure.persistence.JpaClosingTaskPersistenceAdapter;
import com.ho.account.closing.infrastructure.persistence.JpaPeriodLockPersistenceAdapter;
import com.ho.account.closing.infrastructure.persistence.JpaReopenApprovalPersistenceAdapter;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionReceipt;
import com.ho.account.masterdata.core.application.service.FiscalPeriodTransitionService;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import com.ho.account.masterdata.core.infrastructure.adapter.MonolithFiscalPeriodControlAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaFiscalPeriodPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaFiscalPeriodTransitionAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaMasterDataAuditAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.mapper.FiscalPeriodMapper;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodRepository;
import com.ho.account.masterdata.core.infrastructure.persistence.repository.FiscalPeriodTransitionRepository;
import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.client.MockMvcClientHttpRequestFactory;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClient;

/** Two real JPA databases joined only by signed HTTP serialization through an in-memory servlet. */
@DataJpaTest(properties = {"spring.config.name=fiscal-transition-http-test",
        "spring.jpa.hibernate.ddl-auto=create-drop", "spring.flyway.enabled=false",
        "spring.cloud.config.enabled=false", "spring.cloud.config.import-check.enabled=false"})
@ContextConfiguration(classes = FiscalPeriodTransitionHttpIntegrationTest.TestApplication.class)
@Import({JpaFiscalPeriodPersistenceAdapter.class, FiscalPeriodMapper.class, MonolithFiscalPeriodControlAdapter.class,
        FiscalPeriodTransitionService.class, JpaFiscalPeriodTransitionAdapter.class, JpaMasterDataAuditAdapter.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ClosingFiscalTransitionHttpRecoveryTest {
    private static final String SECRET = "synthetic-real-closing-to-master-signing-key-773";
    @Autowired private FiscalPeriodControlPort provider;
    @Autowired private JpaFiscalPeriodPersistenceAdapter periods;
    @Autowired private FiscalPeriodRepository periodRepository;
    @Autowired private FiscalPeriodTransitionRepository receipts;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private DataSource dataSource;
    private Long periodId;
    private AnnotationConfigApplicationContext closingContext;
    private FaultSwitch faults;

    enum Fault { PREPARE_ACK_LOST, APPLY_ACK_LOST, CONFIRM_ACK_LOST,
        DECISION_APPROVAL_SAVE, DECISION_CALENDAR_SAVE, DECISION_AUDIT_SAVE, DECISION_COMMIT,
        DISPATCH_SAVE, DISPATCH_COMMIT,
        AUTHORIZE_TASK_SAVE, AUTHORIZE_GATE_SAVE, AUTHORIZE_AUDIT_SAVE, AUTHORIZE_SAVE, AUTHORIZE_COMMIT,
        READY_SAVE, READY_COMMIT, CLEAR_AUDIT_SAVE, CLEAR_SAVE, CLEAR_COMMIT }

    @BeforeEach
    void connectIndependentContextsThroughRealClient() {
        periodId = new TransactionTemplate(transactionManager).execute(status -> periods.save(FiscalPeriod.reconstitute(
                null, "2026", "11", LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30),
                FiscalPeriod.ClosingStatus.CLOSED, null, null, "prior")).getId());
        var mvc = MockMvcBuilders.standaloneSetup(new InternalFiscalPeriodController(provider),
                        new FiscalPeriodController(provider))
                .setControllerAdvice(new MasterDataExceptionHandler())
                .addFilters(new MasterDataIngressAuthenticationFilter(
                        "different-synthetic-user-signing-key-773", "auth-service", "account-api", SECRET)).build();
        faults = new FaultSwitch();
        RestClient.Builder builder = spy(RestClient.builder()
                .requestFactory(new MockMvcClientHttpRequestFactory(mvc))
                .requestInterceptor((request, body, execution) -> {
                    var response = execution.execute(request, body);
                    // execute returns only after the real provider transaction committed. Lose only the ACK.
                    if (response.getStatusCode().is2xxSuccessful()
                            && faults.loseResponse(request.getMethod().name(), request.getURI().getPath())) {
                        response.close();
                        throw new IOException("synthetic committed response lost");
                    }
                    return response;
                }));
        doReturn(builder).when(builder).requestFactory(any(ClientHttpRequestFactory.class));
        var client = new HttpFiscalPeriodControlAdapter(builder, "http://master-data.test",
                Duration.ofSeconds(2), Duration.ofSeconds(5), SECRET);
        closingContext = new AnnotationConfigApplicationContext();
        closingContext.registerBean(FiscalPeriodControlPort.class, () -> client);
        closingContext.registerBean(FaultSwitch.class, () -> faults);
        closingContext.register(ClosingDatabaseConfiguration.class);
        closingContext.refresh();
        assertThat(closingContext.getBean(DataSource.class)).isNotSameAs(dataSource);
        assertThat(closingContext.getBean(EntityManagerFactory.class))
                .isNotSameAs(((JpaTransactionManager) transactionManager).getEntityManagerFactory());
    }

    @AfterEach
    void closeOwnedContextAndDeleteMasterFixtures() {
        if (closingContext != null) closingContext.close();
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            receipts.deleteAll();
            periodRepository.deleteById(periodId);
        });
    }

    @ParameterizedTest
    @EnumSource(value = Fault.class, names = {"DECISION_APPROVAL_SAVE", "DECISION_CALENDAR_SAVE",
            "DECISION_AUDIT_SAVE", "DECISION_COMMIT", "DISPATCH_SAVE", "DISPATCH_COMMIT"},
            mode = EnumSource.Mode.EXCLUDE)
    void realCoordinatorRecoversIndependentCommitsWithoutReplayingBusinessEffects(Fault fault) {
        Fixture fixture = createClosedFixture();
        var calendars = closingContext.getBean(ClosingCalendarPersistencePort.class);
        var approvals = closingContext.getBean(ReopenApprovalPersistencePort.class);
        var tasks = closingContext.getBean(ClosingTaskPersistencePort.class);
        var gates = closingContext.getBean(ClosingGatePersistencePort.class);
        var coordinator = closingContext.getBean(ClosingPeriodTransitionService.class);
        // Recovery audit is only written on a recovery invocation. First leave a real confirmed
        // receipt with its ACK lost, then fail the actual audit INSERT during that recovery.
        faults.pending.set(fault == Fault.CLEAR_AUDIT_SAVE ? Fault.CONFIRM_ACK_LOST : fault);

        assertThatThrownBy(() -> coordinator.decide(fixture.approvalId(), ReopenApprovalStatus.APPROVED, "approver"))
                .isInstanceOf(ClosingTransitionPendingException.class);
        if (fault == Fault.CLEAR_AUDIT_SAVE) {
            String retainedId = calendars.findById(fixture.calendarId()).orElseThrow().getTransitionId();
            faults.pending.set(fault);
            assertThatThrownBy(() -> coordinator.recoverTransition(
                    fixture.calendarId(), retainedId, false, "recovery-operator"))
                    .isInstanceOf(ClosingTransitionPendingException.class);
        }
        assertThat(faults.pending.get()).isNull();
        ClosingCalendar pending = calendars.findById(fixture.calendarId()).orElseThrow();
        String operationId = pending.getTransitionId();
        assertThat(operationId).isNotBlank();
        var remote = provider.findTransitionReceipt(operationId).orElseThrow();
        assertThat(remote.fiscalPeriodId()).isEqualTo(periodId);
        assertThat(remote.auditUser()).isEqualTo("approver");
        assertThat(approvals.findById(fixture.approvalId()).orElseThrow().getStatus()).isEqualTo(ReopenApprovalStatus.APPROVED);
        assertThat(closingContext.getBean(ClosingAdmissionQuery.class).isClosed(LocalDate.of(2026, 11, 15))).isTrue();

        boolean beforeAuthorization = fault == Fault.PREPARE_ACK_LOST || fault == Fault.AUTHORIZE_SAVE
                || fault == Fault.AUTHORIZE_COMMIT || fault == Fault.AUTHORIZE_TASK_SAVE
                || fault == Fault.AUTHORIZE_GATE_SAVE || fault == Fault.AUTHORIZE_AUDIT_SAVE;
        boolean afterConfirmation = fault == Fault.CONFIRM_ACK_LOST || fault == Fault.CLEAR_SAVE
                || fault == Fault.CLEAR_COMMIT || fault == Fault.CLEAR_AUDIT_SAVE;
        assertThat(remote.phase()).isEqualTo(beforeAuthorization ? FiscalPeriodTransitionReceipt.Phase.PREPARED
                : afterConfirmation ? FiscalPeriodTransitionReceipt.Phase.CONFIRMED : FiscalPeriodTransitionReceipt.Phase.APPLIED);
        assertThat(pending.getTransitionStage()).isEqualTo(beforeAuthorization ? TransitionStage.DISPATCHED
                : afterConfirmation ? TransitionStage.READY_TO_CONFIRM : TransitionStage.AUTHORIZED);
        assertThat(pending.getStatus()).isEqualTo(beforeAuthorization ? ClosingCalendarStatus.CLOSED : ClosingCalendarStatus.OPEN);
        assertThat(pending.getCycleNumber()).isEqualTo(beforeAuthorization ? 1 : 2);
        assertThat(tasks.findByClosingCalendar(pending)).hasSize(beforeAuthorization ? 1 : 2);
        assertThat(gates.findByClosingCalendar(pending)).hasSize(beforeAuthorization ? 1 : 2);
        assertAuditCounts(1, beforeAuthorization ? 0 : 1, 0);
        assertThat(provider.findFiscalPeriodById(periodId).orElseThrow().closingStatus())
                .isEqualTo(afterConfirmation ? "OPEN" : "CLOSED");
        assertThat(periods.findById(periodId).orElseThrow().getClosingStatus())
                .isEqualTo(beforeAuthorization ? FiscalPeriod.ClosingStatus.CLOSED : FiscalPeriod.ClosingStatus.OPEN);

        coordinator.recoverTransition(fixture.calendarId(), operationId, false, "recovery-operator");
        assertRecovered(fixture, operationId, 1);
    }

    @ParameterizedTest
    @EnumSource(value = Fault.class, names = {"DECISION_APPROVAL_SAVE", "DECISION_CALENDAR_SAVE",
            "DECISION_AUDIT_SAVE", "DECISION_COMMIT", "DISPATCH_SAVE", "DISPATCH_COMMIT"})
    void localIntentFailuresRollBackBeforeAnyRemoteReceiptAndRetrySafely(Fault fault) {
        Fixture fixture = createClosedFixture();
        var calendars = closingContext.getBean(ClosingCalendarPersistencePort.class);
        var approvals = closingContext.getBean(ReopenApprovalPersistencePort.class);
        var coordinator = closingContext.getBean(ClosingPeriodTransitionService.class);
        boolean decisionFailure = fault.name().startsWith("DECISION_");
        faults.pending.set(fault);

        assertThatThrownBy(() -> coordinator.decide(fixture.approvalId(), ReopenApprovalStatus.APPROVED, "approver"))
                .isInstanceOf(decisionFailure ? IllegalStateException.class : ClosingTransitionPendingException.class);

        assertThat(faults.pending.get()).isNull();
        ClosingCalendar pending = calendars.findById(fixture.calendarId()).orElseThrow();
        assertThat(pending.getStatus()).isEqualTo(ClosingCalendarStatus.CLOSED);
        assertThat(pending.getCycleNumber()).isEqualTo(1);
        assertThat(approvals.findById(fixture.approvalId()).orElseThrow().getStatus())
                .isEqualTo(decisionFailure ? ReopenApprovalStatus.PENDING : ReopenApprovalStatus.APPROVED);
        assertThat(closingContext.getBean(ClosingTaskPersistencePort.class).findByClosingCalendar(pending)).hasSize(1);
        assertThat(closingContext.getBean(ClosingGatePersistencePort.class).findByClosingCalendar(pending)).hasSize(1);
        assertAuditCounts(decisionFailure ? 0 : 1, 0, 0);
        assertThat(receipts.count()).isZero();
        assertThat(periods.findById(periodId).orElseThrow().getClosingStatus()).isEqualTo(FiscalPeriod.ClosingStatus.CLOSED);
        assertThat(provider.findFiscalPeriodById(periodId).orElseThrow().closingStatus()).isEqualTo("CLOSED");
        assertThat(closingContext.getBean(ClosingAdmissionQuery.class).isClosed(LocalDate.of(2026, 11, 15))).isTrue();
        String operationId;
        if (decisionFailure) {
            // The original decision did not commit, so no operation identity exists to recover.
            assertThat(pending.getTransitionId()).isNull();
            assertThat(pending.getTransitionStage()).isNull();
            coordinator.decide(fixture.approvalId(), ReopenApprovalStatus.APPROVED, "approver");
            operationId = receipts.findAll().get(0).getOperationId();
        } else {
            operationId = pending.getTransitionId();
            assertThat(operationId).isNotBlank();
            assertThat(pending.getTransitionStage()).isEqualTo(TransitionStage.PREPARED);
            assertThat(provider.findTransitionReceipt(operationId)).isEmpty();
            coordinator.recoverTransition(fixture.calendarId(), operationId, false, "recovery-operator");
        }
        assertRecovered(fixture, operationId, decisionFailure ? 0 : 1);
    }

    private void assertRecovered(Fixture fixture, String operationId, int recoveryAudits) {
        var tasks = closingContext.getBean(ClosingTaskPersistencePort.class);
        var gates = closingContext.getBean(ClosingGatePersistencePort.class);
        ClosingCalendar recovered = closingContext.getBean(ClosingCalendarPersistencePort.class)
                .findById(fixture.calendarId()).orElseThrow();
        assertThat(recovered.getTransitionId()).isNull();
        assertThat(recovered.getStatus()).isEqualTo(ClosingCalendarStatus.OPEN);
        assertThat(recovered.getCycleNumber()).isEqualTo(2);
        var confirmed = provider.findTransitionReceipt(operationId).orElseThrow();
        assertThat(confirmed.phase()).isEqualTo(FiscalPeriodTransitionReceipt.Phase.CONFIRMED);
        assertThat(confirmed.fiscalPeriodId()).isEqualTo(periodId);
        assertThat(confirmed.auditUser()).isEqualTo("approver");
        assertThat(confirmed.expectedStatus().name()).isEqualTo("CLOSED");
        assertThat(confirmed.targetStatus().name()).isEqualTo("OPEN");
        assertThat(receipts.count()).isEqualTo(1);
        assertThat(closingContext.getBean(ClosingAdmissionQuery.class).isClosed(LocalDate.of(2026, 11, 15))).isFalse();
        assertThat(tasks.findByClosingCalendar(recovered)).hasSize(2)
                .extracting(ClosingTask::getStatus).containsExactlyInAnyOrder(
                        ClosingTask.ClosingTaskStatus.COMPLETED, ClosingTask.ClosingTaskStatus.PENDING);
        assertThat(gates.findByClosingCalendar(recovered)).hasSize(2)
                .extracting(ClosingGate::getStatus).containsExactlyInAnyOrder(
                        ClosingGate.ClosingGateStatus.PASSED, ClosingGate.ClosingGateStatus.PENDING);
        assertThat(closingContext.getBean(ReopenApprovalPersistencePort.class)
                .findById(fixture.approvalId()).orElseThrow().getStatus()).isEqualTo(ReopenApprovalStatus.APPROVED);
        assertAuditCounts(1, 1, recoveryAudits);
        JdbcTemplate masterJdbc = new JdbcTemplate(dataSource);
        assertThat(masterJdbc.queryForObject("SELECT COUNT(*) FROM master_data_audit_events "
                + "WHERE target_key=? AND event_type='FISCAL_PERIOD_STATUS'", Integer.class, periodId.toString())).isEqualTo(1);
        assertThat(masterJdbc.queryForObject("SELECT COUNT(*) FROM master_data_audit_events "
                + "WHERE target_key=? AND event_type='FISCAL_PERIOD_TRANSITION_CONFIRMED'", Integer.class, periodId.toString())).isEqualTo(1);
    }

    private void assertAuditCounts(int prepared, int approved, int recovered) {
        var rows = closingContext.getBean(ClosingAuditLogRepository.class).findAll();
        assertThat(rows).hasSize(prepared + approved + recovered);
        assertThat(rows.stream().filter(a -> a.getActionType() == ActionType.PERIOD_TRANSITION_PREPARED)).hasSize(prepared);
        assertThat(rows.stream().filter(a -> a.getActionType() == ActionType.REOPEN_APPROVED)).hasSize(approved);
        assertThat(rows.stream().filter(a -> a.getActionType() == ActionType.PERIOD_TRANSITION_RECOVERED)).hasSize(recovered);
    }

    private record Fixture(Long calendarId, Long approvalId) {}

    private Fixture createClosedFixture() {
        var calendars = closingContext.getBean(ClosingCalendarPersistencePort.class);
        var approvals = closingContext.getBean(ReopenApprovalPersistencePort.class);
        var tasks = closingContext.getBean(ClosingTaskPersistencePort.class);
        var gates = closingContext.getBean(ClosingGatePersistencePort.class);
        var localTransaction = new TransactionTemplate(closingContext.getBean(PlatformTransactionManager.class));
        return localTransaction.execute(status -> {
            ClosingCalendar initial = new ClosingCalendar();
            initial.setFiscalYear("2026");
            initial.setFiscalPeriod("11");
            initial.setStatus(ClosingCalendarStatus.CLOSED);
            initial.setAuditUser("fixture");
            ClosingCalendar saved = calendars.save(initial);
            ClosingTask task = new ClosingTask();
            task.assignCycle(saved);
            task.setName("retained completed task");
            task.setStatus(ClosingTask.ClosingTaskStatus.COMPLETED);
            task.setTaskOrder(1);
            tasks.save(task);
            ClosingGate gate = new ClosingGate();
            gate.assignCycle(saved);
            gate.setName("retained passed gate");
            gate.setStatus(ClosingGate.ClosingGateStatus.PASSED);
            gates.save(gate);
            ReopenApproval approval = new ReopenApproval();
            approval.assignFiscalPeriod(periodId, "2026", "11");
            approval.request("requester", "synthetic correction");
            return new Fixture(saved.getId(), approvals.save(approval).getId());
        });
    }

    static final class FaultSwitch {
        final AtomicReference<Fault> pending = new AtomicReference<>();

        boolean loseResponse(String method, String path) {
            Fault target = method.equals("PUT") && path.contains("/transitions/") ? Fault.PREPARE_ACK_LOST
                    : method.equals("POST") && path.endsWith("/confirm") ? Fault.CONFIRM_ACK_LOST
                    : method.equals("POST") && path.contains("/transitions/") ? Fault.APPLY_ACK_LOST : null;
            return target != null && pending.compareAndSet(target, null);
        }

        void afterCalendarSave(ClosingCalendar calendar) {
            Fault fault = pending.get();
            boolean match = (calendar.getTransitionStage() == TransitionStage.PREPARED
                            && (fault == Fault.DECISION_CALENDAR_SAVE || fault == Fault.DECISION_COMMIT))
                    || (calendar.getTransitionStage() == TransitionStage.DISPATCHED
                            && (fault == Fault.DISPATCH_SAVE || fault == Fault.DISPATCH_COMMIT))
                    || (calendar.getTransitionStage() == TransitionStage.AUTHORIZED
                            && (fault == Fault.AUTHORIZE_SAVE || fault == Fault.AUTHORIZE_COMMIT))
                    || (calendar.getTransitionStage() == TransitionStage.READY_TO_CONFIRM
                            && (fault == Fault.READY_SAVE || fault == Fault.READY_COMMIT))
                    || (calendar.getTransitionId() == null && calendar.getStatus() == ClosingCalendarStatus.OPEN
                            && (fault == Fault.CLEAR_SAVE || fault == Fault.CLEAR_COMMIT));
            if (!match || !pending.compareAndSet(fault, null)) return;
            if (fault.name().endsWith("_COMMIT")) {
                // Throw only after real JPA writes flushed and immediately before Closing commits.
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override public void beforeCommit(boolean readOnly) { throw new IllegalStateException("synthetic local commit failure"); }
                });
            } else {
                throw new IllegalStateException("synthetic local save failure");
            }
        }

        void afterSave(Fault target) {
            if (pending.compareAndSet(target, null)) throw new IllegalStateException("synthetic local save failure: " + target);
        }

        void afterAuditSave(ClosingAuditLog audit) {
            switch (audit.getActionType()) {
                case PERIOD_TRANSITION_PREPARED -> afterSave(Fault.DECISION_AUDIT_SAVE);
                case REOPEN_APPROVED -> afterSave(Fault.AUTHORIZE_AUDIT_SAVE);
                case PERIOD_TRANSITION_RECOVERED -> afterSave(Fault.CLEAR_AUDIT_SAVE);
                default -> { }
            }
        }
    }

    @TestConfiguration(proxyBeanMethods = false)
    // Match Boot's class proxies: the coordinator also implements the narrower recovery interface.
    @EnableTransactionManagement(proxyTargetClass = true)
    @EnableJpaRepositories(basePackageClasses = ClosingCalendarRepository.class)
    @Import({JpaClosingAggregatePersistenceAdapter.class, JpaPeriodLockPersistenceAdapter.class,
            ClosingPeriodTransitionService.class, ClosingAdmissionService.class})
    static class ClosingDatabaseConfiguration {
        @Bean DataSource dataSource() {
            return new DriverManagerDataSource("jdbc:h2:mem:closing-http-" + UUID.randomUUID()
                    + ";MODE=PostgreSQL;DB_CLOSE_DELAY=-1", "sa", "");
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource source) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(source);
            factory.setPersistenceUnitName("closing-http-fixture");
            factory.setPackagesToScan("com.ho.account.closing.infrastructure.persistence");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean ClosingCalendarPersistencePort calendars(ClosingCalendarRepository repository, FaultSwitch faults) {
            var actual = new JpaClosingCalendarPersistenceAdapter(repository);
            return new ClosingCalendarPersistencePort() {
                @Override public ClosingCalendar save(ClosingCalendar calendar) {
                    ClosingCalendar saved = actual.save(calendar);
                    faults.afterCalendarSave(saved);
                    return saved;
                }
                @Override public Optional<ClosingCalendar> findById(Long id) { return actual.findById(id); }
                @Override public Optional<ClosingCalendar> findByFiscalYearAndFiscalPeriod(String year, String period) {
                    return actual.findByFiscalYearAndFiscalPeriod(year, period);
                }
            };
        }
        @Bean ReopenApprovalPersistencePort approvals(ReopenApprovalRepository repository, FaultSwitch faults) {
            var actual = spy(new JpaReopenApprovalPersistenceAdapter(repository));
            doAnswer(invocation -> {
                ReopenApproval saved = (ReopenApproval) invocation.callRealMethod();
                if (saved.getStatus() == ReopenApprovalStatus.APPROVED) faults.afterSave(Fault.DECISION_APPROVAL_SAVE);
                return saved;
            }).when(actual).save(any(ReopenApproval.class));
            return actual;
        }
        @Bean ClosingTaskPersistencePort tasks(ClosingTaskRepository repository,
                ClosingCalendarRepository calendars, FaultSwitch faults) {
            var actual = spy(new JpaClosingTaskPersistenceAdapter(repository, calendars));
            doAnswer(invocation -> {
                ClosingTask saved = (ClosingTask) invocation.callRealMethod();
                if (saved.getCycleNumber() == 2) faults.afterSave(Fault.AUTHORIZE_TASK_SAVE);
                return saved;
            }).when(actual).save(any(ClosingTask.class));
            return actual;
        }
        @Bean ClosingGatePersistencePort gates(ClosingGateRepository repository,
                ClosingCalendarRepository calendars, FaultSwitch faults) {
            var actual = spy(new JpaClosingGatePersistenceAdapter(repository, calendars));
            doAnswer(invocation -> {
                ClosingGate saved = (ClosingGate) invocation.callRealMethod();
                if (saved.getCycleNumber() == 2) faults.afterSave(Fault.AUTHORIZE_GATE_SAVE);
                return saved;
            }).when(actual).save(any(ClosingGate.class));
            return actual;
        }
        @Bean ClosingAuditLogPersistencePort audits(ClosingAuditLogRepository repository,
                ClosingCalendarRepository calendars, FaultSwitch faults) {
            var actual = new ClosingAuditLogPersistenceAdapter(repository, calendars);
            return audit -> {
                actual.save(audit);
                // Force the real audit INSERT before injection, including saves without saveAndFlush.
                repository.flush();
                faults.afterAuditSave(audit);
            };
        }
        @Bean ClosingTransitionTransactions transactions(ClosingAggregatePersistencePort aggregates,
                ClosingCalendarPersistencePort calendars, ReopenApprovalPersistencePort approvals,
                ClosingAuditLogPersistencePort audits, FiscalPeriodControlPort master,
                ClosingTaskPersistencePort tasks, ClosingGatePersistencePort gates) {
            // Reopen has no final-close evidence input; all persistence collaborators are real adapters.
            return new ClosingTransitionTransactions(aggregates, calendars, approvals, audits, master, null, tasks, gates);
        }
    }
}
