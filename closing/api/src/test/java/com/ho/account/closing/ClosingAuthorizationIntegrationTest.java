package com.ho.account.closing;

import com.fasterxml.jackson.databind.JsonNode;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.ReopenApprovalPersistencePort;
import com.ho.account.closing.domain.ClosingAuditLog;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ReopenApproval;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.closing.infrastructure.persistence.ClosingAuditLogRepository;
import com.ho.account.closing.infrastructure.persistence.ClosingAuditLogEntity;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionCommand;
import com.ho.account.contracts.masterdata.FiscalPeriodTransitionReceipt;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

/**
 * Issue #771 regression: caller identity must cross the HTTP boundary from trusted gateway headers.
 * The Closing application and its JPA adapters are real; only the remote master-data service is
 * represented by an independently stateful control port.
 */
@ActiveProfiles("local")
@SpringBootTest(
        classes = {ClosingApplication.class, ClosingAuthorizationIntegrationTest.Configuration.class},
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:closing-authorization-http;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
class ClosingAuthorizationIntegrationTest {

    private static final String TRUSTED_ACTOR = "single-operator";
    private static final String CLOSING_ROLE = "ROLE_CLOSING_MANAGER";

    @Autowired
    private TestRestTemplate http;
    @Autowired
    private SyntheticFiscalPeriodControl fiscalPeriods;
    @Autowired
    private ClosingCalendarPersistencePort calendars;
    @Autowired
    private ReopenApprovalPersistencePort approvals;
    @Autowired
    private ClosingAuditLogRepository audits;

    @Test
    void oneTrustedPrincipalCannotRequestAndApproveReopenByForgingBodyActors() {
        FiscalPeriodRef period = fiscalPeriods.createClosed("2042", "03");
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear(period.fiscalYear());
        calendar.setFiscalPeriod(period.fiscalPeriod());
        calendar.setStatus(ClosingCalendarStatus.CLOSED);
        calendar.setAuditUser("fixture");
        calendar = calendars.save(calendar);
        long calendarId = calendar.getId();

        ResponseEntity<JsonNode> request = exchange(
                HttpMethod.POST,
                "/api/closing/reopen-approvals",
                Map.of(
                        "fiscalPeriodId", period.id(),
                        "requestedBy", "USER_A",
                        "reason", "Correct a material closing error"));

        assertThat(request.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(request.getBody()).isNotNull();
        long approvalId = request.getBody().path("id").asLong();

        ResponseEntity<JsonNode> decision = exchange(
                HttpMethod.PUT,
                "/api/closing/reopen-approvals/" + approvalId + "/status",
                Map.of(
                        "status", "APPROVED",
                        "approvedBy", "USER_B_MANAGER"));

        ReopenApproval persisted = approvals.findById(approvalId).orElseThrow();
        ClosingCalendar persistedCalendar = calendars.findById(calendar.getId()).orElseThrow();
        ClosingAuditLogEntity requestAudit = audits.findAll().stream()
                .filter(audit -> audit.getClosingCalendar().getId().equals(calendarId)
                        && audit.getActionType() == ClosingAuditLog.ActionType.REOPEN_REQUEST)
                .findFirst()
                .orElseThrow();

        assertSoftly(softly -> {
            softly.assertThat(decision.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            softly.assertThat(persisted.getRequestedBy()).isEqualTo(TRUSTED_ACTOR);
            softly.assertThat(persisted.getStatus()).isEqualTo(ReopenApprovalStatus.PENDING);
            softly.assertThat(persisted.getApprovedBy()).isNull();
            softly.assertThat(persistedCalendar.getStatus()).isEqualTo(ClosingCalendarStatus.CLOSED);
            softly.assertThat(fiscalPeriods.findFiscalPeriodById(period.id()).orElseThrow().closingStatus())
                    .isEqualTo("CLOSED");
            softly.assertThat(requestAudit.getActionUser()).isEqualTo(TRUSTED_ACTOR);
        });
    }

    @Test
    void closingManagerCanApproveWithTheMaximumBoundedActorIdentity() {
        FiscalPeriodRef period = fiscalPeriods.createClosed("2042", "04");
        ClosingCalendar calendar = closedCalendar(period);
        long approvalId = requestReopen(period.id(), "requester");
        String approver = "a".repeat(42);

        ResponseEntity<JsonNode> decision = exchangeAs(approver, HttpMethod.PUT,
                "/api/closing/reopen-approvals/" + approvalId + "/status",
                Map.of("status", "APPROVED", "approvedBy", "forged-body-actor"));

        assertThat(decision.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(approvals.findById(approvalId).orElseThrow().getApprovedBy()).isEqualTo(approver);
        assertThat(calendars.findById(calendar.getId()).orElseThrow().getStatus()).isEqualTo(ClosingCalendarStatus.OPEN);
        assertThat(fiscalPeriods.findFiscalPeriodById(period.id()).orElseThrow().closingStatus()).isEqualTo("OPEN");
        assertThat(fiscalPeriods.receipts.values()).singleElement().satisfies(receipt -> {
            assertThat(receipt.auditUser()).isEqualTo(approver);
            assertThat(receipt.phase()).isEqualTo(FiscalPeriodTransitionReceipt.Phase.CONFIRMED);
        });
    }

    @Test
    void overlongActorIsRejectedBeforeLocalReopenDecisionPersists() {
        FiscalPeriodRef period = fiscalPeriods.createClosed("2042", "05");
        ClosingCalendar calendar = closedCalendar(period);
        long approvalId = requestReopen(period.id(), "requester");
        int receiptsBefore = fiscalPeriods.receipts.size();

        ResponseEntity<JsonNode> decision = exchangeAs("a".repeat(43), HttpMethod.PUT,
                "/api/closing/reopen-approvals/" + approvalId + "/status",
                Map.of("status", "APPROVED", "approvedBy", "forged-body-actor"));

        assertThat(decision.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(approvals.findById(approvalId).orElseThrow().getStatus()).isEqualTo(ReopenApprovalStatus.PENDING);
        assertThat(calendars.findById(calendar.getId()).orElseThrow().getStatus()).isEqualTo(ClosingCalendarStatus.CLOSED);
        assertThat(calendars.findById(calendar.getId()).orElseThrow().getTransitionId()).isNull();
        assertThat(fiscalPeriods.findFiscalPeriodById(period.id()).orElseThrow().closingStatus()).isEqualTo("CLOSED");
        assertThat(fiscalPeriods.receipts).hasSize(receiptsBefore);
    }

    private ClosingCalendar closedCalendar(FiscalPeriodRef period) {
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear(period.fiscalYear());
        calendar.setFiscalPeriod(period.fiscalPeriod());
        calendar.setStatus(ClosingCalendarStatus.CLOSED);
        calendar.setAuditUser("fixture");
        return calendars.save(calendar);
    }

    private long requestReopen(long fiscalPeriodId, String actor) {
        ResponseEntity<JsonNode> response = exchangeAs(actor, HttpMethod.POST,
                "/api/closing/reopen-approvals",
                Map.of("fiscalPeriodId", fiscalPeriodId, "requestedBy", "forged-body-actor", "reason", "correction"));
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return response.getBody().path("id").asLong();
    }

    private ResponseEntity<JsonNode> exchange(HttpMethod method, String path, Object body) {
        return exchangeAs(TRUSTED_ACTOR, method, path, body);
    }

    private ResponseEntity<JsonNode> exchangeAs(String actor, HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Auth-User", actor);
        headers.set("X-Auth-Roles", CLOSING_ROLE);
        return http.exchange(path, method, new HttpEntity<>(body, headers), JsonNode.class);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean
        @Primary
        SyntheticFiscalPeriodControl syntheticFiscalPeriodControl() {
            return new SyntheticFiscalPeriodControl();
        }
    }

    static final class SyntheticFiscalPeriodControl implements FiscalPeriodControlPort {
        private final AtomicLong ids = new AtomicLong();
        private final Map<Long, FiscalPeriodRef> periods = new ConcurrentHashMap<>();
        private final Map<String, FiscalPeriodTransitionReceipt> receipts = new ConcurrentHashMap<>();

        FiscalPeriodRef createClosed(String year, String period) {
            long id = ids.incrementAndGet();
            LocalDate start = LocalDate.of(Integer.parseInt(year), Integer.parseInt(period), 1);
            FiscalPeriodRef created = new FiscalPeriodRef(
                    id, year, period, start, start.withDayOfMonth(start.lengthOfMonth()), "CLOSED");
            periods.put(id, created);
            return created;
        }

        @Override
        public Optional<FiscalPeriodRef> findFiscalPeriodById(Long id) {
            return Optional.ofNullable(periods.get(id));
        }

        @Override
        public Optional<FiscalPeriodRef> findFiscalPeriod(String fiscalYear, String fiscalPeriod) {
            return periods.values().stream()
                    .filter(period -> period.fiscalYear().equals(fiscalYear)
                            && period.fiscalPeriod().equals(fiscalPeriod))
                    .findFirst();
        }

        @Override
        public FiscalPeriodRef updateClosingStatus(Long id, String closingStatus, String auditUser) {
            return periods.compute(id, (ignored, current) -> {
                if (current == null) {
                    throw new IllegalStateException("Synthetic fiscal period not found: " + id);
                }
                return new FiscalPeriodRef(
                        current.id(),
                        current.fiscalYear(),
                        current.fiscalPeriod(),
                        current.startDate(),
                        current.endDate(),
                        closingStatus);
            });
        }

        @Override
        public synchronized FiscalPeriodTransitionReceipt prepareClosingTransition(FiscalPeriodTransitionCommand command) {
            FiscalPeriodTransitionReceipt existing = receipts.get(command.operationId());
            if (existing != null) {
                if (!existing.matches(command)) throw new IllegalStateException("Operation payload changed");
                return existing;
            }
            FiscalPeriodRef period = periods.get(command.fiscalPeriodId());
            if (period == null || !period.closingStatus().equals(command.expectedStatus().name())) {
                throw new IllegalStateException("Expected fiscal period state changed");
            }
            FiscalPeriodTransitionReceipt prepared = new FiscalPeriodTransitionReceipt(command.operationId(),
                    command.fiscalPeriodId(), command.expectedStatus(), command.targetStatus(), command.auditUser(),
                    FiscalPeriodTransitionReceipt.Phase.PREPARED);
            receipts.put(command.operationId(), prepared);
            return prepared;
        }

        @Override
        public synchronized FiscalPeriodTransitionReceipt applyClosingTransition(String operationId) {
            FiscalPeriodTransitionReceipt receipt = receipts.get(operationId);
            if (receipt == null) throw new IllegalStateException("Transition was not prepared");
            if (receipt.phase() == FiscalPeriodTransitionReceipt.Phase.APPLIED) return receipt;
            FiscalPeriodRef current = periods.get(receipt.fiscalPeriodId());
            if (current == null || !current.closingStatus().equals(receipt.expectedStatus().name())) {
                throw new IllegalStateException("Expected fiscal period state changed");
            }
            if (receipt.targetStatus() == com.ho.account.contracts.masterdata.FiscalPeriodClosingStatus.CLOSED) {
                updateClosingStatus(receipt.fiscalPeriodId(), receipt.targetStatus().name(), receipt.auditUser());
            }
            FiscalPeriodTransitionReceipt applied = new FiscalPeriodTransitionReceipt(receipt.operationId(),
                    receipt.fiscalPeriodId(), receipt.expectedStatus(), receipt.targetStatus(), receipt.auditUser(),
                    FiscalPeriodTransitionReceipt.Phase.APPLIED);
            receipts.put(operationId, applied);
            return applied;
        }

        @Override
        public Optional<FiscalPeriodTransitionReceipt> findTransitionReceipt(String operationId) {
            return Optional.ofNullable(receipts.get(operationId));
        }

        @Override
        public synchronized FiscalPeriodTransitionReceipt confirmClosingTransition(String operationId) {
            FiscalPeriodTransitionReceipt receipt = receipts.get(operationId);
            if (receipt == null || receipt.phase() != FiscalPeriodTransitionReceipt.Phase.APPLIED) {
                throw new IllegalStateException("Applied reopen receipt is missing");
            }
            updateClosingStatus(receipt.fiscalPeriodId(), receipt.targetStatus().name(), receipt.auditUser());
            FiscalPeriodTransitionReceipt confirmed = new FiscalPeriodTransitionReceipt(receipt.operationId(),
                    receipt.fiscalPeriodId(), receipt.expectedStatus(), receipt.targetStatus(), receipt.auditUser(),
                    FiscalPeriodTransitionReceipt.Phase.CONFIRMED);
            receipts.put(operationId, confirmed);
            return confirmed;
        }
    }
}
