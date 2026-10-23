package com.ho.account.closing;

import com.fasterxml.jackson.databind.JsonNode;
import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.application.port.in.FinalCloseEvidenceUseCase;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.PeriodLockPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ClosingGate;
import com.ho.account.closing.domain.ClosingTask;
import com.ho.account.closing.domain.ClosingTask.ClosingTaskStatus;
import com.ho.account.closing.domain.PeriodLock.PeriodLockType;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.infrastructure.persistence.ClosingAuditLogRepository;
import com.ho.account.contracts.closing.AccountingPeriodStatusPort;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.masterdata.core.application.port.out.FiscalPeriodPersistencePort;
import com.ho.account.masterdata.core.domain.model.FiscalPeriod;
import java.time.LocalDate;
import java.util.Locale;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.ClassUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Real Closing HTTP requests read committed synthetic H2 state through the application ports.
 * No enclosing test transaction is used: each command commits before a fresh HTTP request.
 * Journal consumers are tested separately in core; this runtime deliberately has no Journal classes.
 */
@ActiveProfiles("local")
@SpringBootTest(
        classes = ClosingApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:closing-admission-http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
class ClosingAdmissionPersistenceIntegrationTest {

    @Autowired
    private TestRestTemplate http;
    @Autowired
    private ClosingUseCase closing;
    @Autowired
    private AccountingPeriodStatusPort statusPort;
    @Autowired
    private FiscalPeriodPersistencePort fiscalPeriods;
    @Autowired
    private FiscalPeriodControlPort fiscalControl;
    @Autowired
    private ClosingCalendarPersistencePort calendars;
    @Autowired
    private PeriodLockPersistencePort locks;
    @Autowired
    private ClosingAuditLogRepository audits;
    @Autowired
    private FinalCloseEvidenceUseCase finalCloseEvidence;

    @ParameterizedTest
    @EnumSource(PeriodLockType.class)
    void committedLockAndUnlockAreVisibleToHttpAndStatusPort(PeriodLockType type) {
        LocalDate date = LocalDate.of(2041, type.ordinal() + 1, 15);
        FiscalPeriod period = createPeriod(date);
        createCalendar(period);
        assertAdmission(date, true);

        closing.lockPeriod(period.getId(), type, "closing-operator", "synthetic lock");

        assertThat(locks.findByFiscalPeriodId(period.getId()).orElseThrow().getLockType()).isEqualTo(type);
        assertAdmission(date, false);

        closing.unlockPeriod(period.getId(), "closing-operator");

        assertThat(locks.findByFiscalPeriodId(period.getId())).isEmpty();
        assertAdmission(date, true);
    }

    @Test
    void closingAndApprovedReopenStayDeniedUntilTheIndependentLockIsReleased() {
        LocalDate date = LocalDate.of(2041, 5, 15);
        FiscalPeriod period = createPeriod(date);
        ClosingCalendar calendar = createCalendar(period);
        assertThat(AopUtils.isAopProxy(closing)).isTrue();
        assertAdmission(date, true);

        closing.updateClosingCalendarStatus(calendar.getId(), ClosingCalendarStatus.IN_PROGRESS, "closer");

        assertThat(fiscalControl.findFiscalPeriodById(period.getId()).orElseThrow().closingStatus()).isEqualTo("OPEN");
        assertThat(calendars.findById(calendar.getId()).orElseThrow().getStatus())
                .isEqualTo(ClosingCalendarStatus.IN_PROGRESS);
        assertAdmission(date, false);

        completeRequiredChecklist(calendar);
        closing.determineClosingStatus(calendar.getId(), "closer");

        assertThat(fiscalControl.findFiscalPeriodById(period.getId()).orElseThrow().closingStatus()).isEqualTo("CLOSED");
        assertThat(calendars.findById(calendar.getId()).orElseThrow().getStatus())
                .isEqualTo(ClosingCalendarStatus.CLOSED);
        assertAdmission(date, false);

        closing.lockPeriod(period.getId(), PeriodLockType.PARTIAL_LOCK, "lock-operator", "synthetic independent lock");
        ReopenApproval request = closing.requestPeriodReopen(period.getId(), "requester", "synthetic correction");
        closing.updateReopenApprovalStatus(request.getId(), ReopenApprovalStatus.APPROVED, "independent-approver");

        assertThat(fiscalControl.findFiscalPeriodById(period.getId()).orElseThrow().closingStatus()).isEqualTo("OPEN");
        assertThat(calendars.findById(calendar.getId()).orElseThrow().getStatus())
                .isEqualTo(ClosingCalendarStatus.OPEN);
        assertThat(locks.findByFiscalPeriodId(period.getId())).isPresent();
        assertAdmission(date, false);

        closing.unlockPeriod(period.getId(), "lock-operator");

        assertAdmission(date, true);
    }

    @Test
    void missingCalendarIsDeniedAndMissingMasterIsUnavailable() {
        LocalDate noCalendarDate = LocalDate.of(2041, 10, 15);
        createPeriod(noCalendarDate);
        assertAdmission(noCalendarDate, false);

        long auditCount = audits.count();
        ResponseEntity<JsonNode> response = getAdmission(LocalDate.of(2041, 11, 15));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().path("code").asText()).isEqualTo("CLOSING_ADMISSION_UNAVAILABLE");
        assertThat(response.getBody().has("ordinaryPostingAllowed")).isFalse();
        assertThat(audits.count()).isEqualTo(auditCount);
    }

    @Test
    void closingApiRuntimeDoesNotAcquireJournalTestDependency() {
        assertThat(ClassUtils.isPresent(
                "com.ho.account.journalledger.application.service.ledger.PostingService",
                getClass().getClassLoader())).isFalse();
    }

    private FiscalPeriod createPeriod(LocalDate date) {
        FiscalPeriod period = new FiscalPeriod();
        period.setFiscalYear(Integer.toString(date.getYear()));
        period.setFiscalPeriod(String.format(Locale.ROOT, "%02d", date.getMonthValue()));
        period.setStartDate(date.withDayOfMonth(1));
        period.setEndDate(date.withDayOfMonth(date.lengthOfMonth()));
        period.changeClosingStatus(FiscalPeriod.ClosingStatus.OPEN, "synthetic-fixture");
        return fiscalPeriods.save(period);
    }

    private ClosingCalendar createCalendar(FiscalPeriod period) {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear(period.getFiscalYear());
        calendar.setFiscalPeriod(period.getFiscalPeriod());
        calendar.setAuditUser("synthetic-fixture");
        return closing.createClosingCalendar(calendar);
    }

    private void completeRequiredChecklist(ClosingCalendar calendar) {
        ClosingTask task = new ClosingTask();
        task.setClosingCalendar(calendar);
        task.setName("Synthetic reconciliation");
        task.setMandatory(true);
        task.setTaskOrder(1);
        ClosingTask savedTask = closing.createClosingTask(task);
        closing.updateClosingTaskStatus(savedTask.getId(), ClosingTaskStatus.IN_PROGRESS, "task-operator");
        closing.updateClosingTaskStatus(savedTask.getId(), ClosingTaskStatus.COMPLETED, "task-operator");

        ClosingGate gate = new ClosingGate();
        gate.setClosingCalendar(calendar);
        gate.setName("Synthetic closing evidence");
        ClosingGate savedGate = closing.createClosingGate(gate);
        closing.checkAndPassClosingGate(savedGate.getId(), "gate-operator");
        var period = fiscalControl.findFiscalPeriod(calendar.getFiscalYear(), calendar.getFiscalPeriod()).orElseThrow();
        FinalCloseEvidenceFixtures.recordValid(finalCloseEvidence, calendar, period.id(), period.endDate());
    }

    private void assertAdmission(LocalDate date, boolean allowed) {
        // Separate HTTP execution and repository reloads cannot reuse the command's managed entities.
        assertThat(TransactionSynchronizationManager.isActualTransactionActive()).isFalse();
        long auditCount = audits.count();
        ResponseEntity<JsonNode> response = getAdmission(date);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("no-store");
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().size()).isEqualTo(2);
        assertThat(response.getBody().path("accountingDate").asText()).isEqualTo(date.toString());
        assertThat(response.getBody().path("ordinaryPostingAllowed").isBoolean()).isTrue();
        assertThat(response.getBody().path("ordinaryPostingAllowed").asBoolean()).isEqualTo(allowed);
        assertThat(statusPort.isClosed(date)).isEqualTo(!allowed);
        assertThat(audits.count()).isEqualTo(auditCount);
    }

    private ResponseEntity<JsonNode> getAdmission(LocalDate date) {
        return http.getForEntity("/api/closing/admission?accountingDate={date}", JsonNode.class, date);
    }
}
