package com.ho.account.closing;

import com.ho.account.closing.application.port.in.ClosingUseCase;
import com.ho.account.closing.application.port.out.ClosingCalendarPersistencePort;
import com.ho.account.closing.application.port.out.ReopenApprovalPersistencePort;
import com.ho.account.closing.domain.ClosingCalendar;
import com.ho.account.closing.domain.ClosingCalendar.ClosingCalendarStatus;
import com.ho.account.closing.domain.ReopenApproval.ReopenApprovalStatus;
import com.ho.account.contracts.masterdata.FiscalPeriodControlPort;
import com.ho.account.contracts.masterdata.FiscalPeriodRef;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * The same test compiles at audited a97d10a and at the remediated head. The synthetic provider
 * commits OPEN before losing its response, exactly as an independent Master deployment can do.
 */
@ActiveProfiles("local")
@SpringBootTest(classes = {ClosingApplication.class, FiscalTransitionRedBaselineTest.Configuration.class},
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.cloud.discovery.enabled=false",
                "spring.cloud.loadbalancer.enabled=false",
                "spring.cloud.vault.enabled=false",
                "eureka.client.enabled=false",
                "management.tracing.enabled=false",
                "spring.data.redis.repositories.enabled=false",
                "spring.datasource.url=jdbc:h2:mem:fiscal-transition-red-baseline;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
                "spring.datasource.username=sa",
                "spring.datasource.password=",
                "spring.jpa.hibernate.ddl-auto=create-drop",
                "spring.flyway.enabled=false"
        })
class FiscalTransitionRedBaselineTest {

    @Autowired private ClosingUseCase closing;
    @Autowired private ClosingCalendarPersistencePort calendars;
    @Autowired private ReopenApprovalPersistencePort approvals;
    @Autowired private StatefulRemote remote;

    @Test
    void lostResponseAfterRemoteOpenNeverLeavesAnUnapprovedLocalReopen() {
        FiscalPeriodRef period = remote.createClosed();
        ClosingCalendar calendar = new ClosingCalendar();
        calendar.setFiscalYear(period.fiscalYear());
        calendar.setFiscalPeriod(period.fiscalPeriod());
        calendar.setStatus(ClosingCalendarStatus.CLOSED);
        calendar.setAuditUser("fixture");
        calendar = calendars.save(calendar);

        long approvalId = closing.requestPeriodReopen(period.id(), "requester", "Correction").getId();

        assertThatThrownBy(() -> closing.updateReopenApprovalStatus(
                approvalId, ReopenApprovalStatus.APPROVED, "approver"))
                .isInstanceOf(RuntimeException.class);

        // The remote side effect survives a lost HTTP response. Local authorization must already
        // have committed before that side effect is allowed to occur.
        assertThat(remote.findFiscalPeriodById(period.id()).orElseThrow().closingStatus()).isEqualTo("OPEN");
        assertThat(approvals.findById(approvalId).orElseThrow().getStatus())
                .isEqualTo(ReopenApprovalStatus.APPROVED);
        assertThat(calendars.findById(calendar.getId()).orElseThrow().getStatus())
                .isIn(ClosingCalendarStatus.CLOSED, ClosingCalendarStatus.OPEN);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class Configuration {
        @Bean StatefulRemote statefulRemote() { return new StatefulRemote(); }

        @Bean @Primary
        FiscalPeriodControlPort fiscalPeriodControlPort(StatefulRemote remote) {
            // Reflection keeps this regression source compatible with the pre-receipt contract.
            return mock(FiscalPeriodControlPort.class, call -> remote.invoke(call.getMethod(), call.getArguments()));
        }
    }

    static final class StatefulRemote {
        private final Map<Long, FiscalPeriodRef> periods = new HashMap<>();
        private final Map<String, Object> receipts = new HashMap<>();
        private boolean lostResponse;

        FiscalPeriodRef createClosed() {
            FiscalPeriodRef period = new FiscalPeriodRef(773L, "2041", "09",
                    LocalDate.of(2041, 9, 1), LocalDate.of(2041, 9, 30), "CLOSED");
            periods.put(period.id(), period);
            return period;
        }

        Optional<FiscalPeriodRef> findFiscalPeriodById(Long id) {
            return Optional.ofNullable(periods.get(id));
        }

        Object invoke(Method method, Object[] args) throws Exception {
            return switch (method.getName()) {
                case "findFiscalPeriodById" -> findFiscalPeriodById((Long) args[0]);
                case "findFiscalPeriod" -> periods.values().stream()
                        .filter(p -> p.fiscalYear().equals(args[0]) && p.fiscalPeriod().equals(args[1]))
                        .findFirst();
                case "updateClosingStatus" -> {
                    FiscalPeriodRef changed = changeStatus((Long) args[0], (String) args[1]);
                    loseResponseOnce();
                    yield changed;
                }
                case "findTransitionReceipt" -> Optional.ofNullable(receipts.get(args[0]));
                case "prepareClosingTransition" -> {
                    Object command = args[0];
                    String operationId = (String) accessor(command, "operationId");
                    Object existing = receipts.get(operationId);
                    if (existing != null) {
                        yield existing;
                    }
                    Object prepared = receipt(command, "PREPARED");
                    receipts.put(operationId, prepared);
                    yield prepared;
                }
                case "applyClosingTransition" -> {
                    String operationId = (String) args[0];
                    Object prepared = receipts.get(operationId);
                    Long periodId = (Long) accessor(prepared, "fiscalPeriodId");
                    changeStatus(periodId, accessor(prepared, "targetStatus").toString());
                    Object applied = receipt(prepared, "APPLIED");
                    receipts.put(operationId, applied);
                    loseResponseOnce();
                    yield applied;
                }
                default -> throw new UnsupportedOperationException(method.getName());
            };
        }

        private FiscalPeriodRef changeStatus(Long id, String status) {
            FiscalPeriodRef old = periods.get(id);
            FiscalPeriodRef changed = new FiscalPeriodRef(old.id(), old.fiscalYear(),
                    old.fiscalPeriod(), old.startDate(), old.endDate(), status);
            periods.put(id, changed);
            return changed;
        }

        private void loseResponseOnce() {
            if (!lostResponse) {
                lostResponse = true;
                throw new IllegalStateException("Remote commit completed; HTTP response lost");
            }
        }

        private static Object accessor(Object record, String name) throws Exception {
            return record.getClass().getMethod(name).invoke(record);
        }

        @SuppressWarnings({"unchecked", "rawtypes"})
        private static Object receipt(Object command, String phaseName) throws Exception {
            Class<?> receiptType = Class.forName(
                    "com.ho.account.contracts.masterdata.FiscalPeriodTransitionReceipt");
            Class<?> statusType = Class.forName(
                    "com.ho.account.contracts.masterdata.FiscalPeriodClosingStatus");
            Class<?> phaseType = Class.forName(
                    "com.ho.account.contracts.masterdata.FiscalPeriodTransitionReceipt$Phase");
            Constructor<?> constructor = receiptType.getConstructor(String.class, Long.class,
                    statusType, statusType, String.class, phaseType);
            return constructor.newInstance(accessor(command, "operationId"),
                    accessor(command, "fiscalPeriodId"), accessor(command, "expectedStatus"),
                    accessor(command, "targetStatus"), accessor(command, "auditUser"),
                    Enum.valueOf((Class<Enum>) phaseType, phaseName));
        }
    }
}
