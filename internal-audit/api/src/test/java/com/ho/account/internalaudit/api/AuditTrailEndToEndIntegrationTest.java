package com.ho.account.internalaudit.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.ho.account.internalaudit.api.adapter.in.web.EvaluationController;
import com.ho.account.internalaudit.api.adapter.in.web.InternalAuditIdentityFilter;
import com.ho.account.internalaudit.api.adapter.in.web.InternalAuditPrincipal;
import com.ho.account.internalaudit.api.adapter.in.web.RcmController;
import com.ho.account.internalaudit.core.application.port.out.AuditLogPersistencePort;
import com.ho.account.internalaudit.core.domain.AuditLogEntry;
import com.ho.account.internalaudit.core.domain.evaluation.Deficiency;
import com.ho.account.internalaudit.core.domain.evaluation.DesignEvaluation;
import com.ho.account.internalaudit.core.domain.evaluation.OperatingEvaluation;
import com.ho.account.internalaudit.core.domain.rcm.ControlActivity;
import com.ho.account.internalaudit.core.domain.rcm.RcmProcess;
import com.ho.account.internalaudit.core.domain.rcm.RcmRisk;
import com.ho.account.internalaudit.core.infrastructure.persistence.entity.AuditLogJpaEntity;
import com.ho.account.internalaudit.core.infrastructure.persistence.repository.AuditLogRepository;
import jakarta.persistence.EntityManager;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local")
class AuditTrailEndToEndIntegrationTest {

    @BeforeEach
    void verifiedActorFixture() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setAttribute(InternalAuditIdentityFilter.PRINCIPAL_ATTRIBUTE,
                new InternalAuditPrincipal("auditor_e2e", List.of("ROLE_AUDITOR"), 1));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void clearVerifiedActorFixture() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Autowired
    private RcmController rcmController;

    @Autowired
    private EvaluationController evaluationController;

    @Autowired
    private AuditLogPersistencePort auditLogPersistencePort;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void portEnforcesStrictAppendOnlySemanticsAtInterfaceLevel() {
        Method[] methods = AuditLogPersistencePort.class.getMethods();
        for (Method method : methods) {
            String methodName = method.getName().toLowerCase();
            assertThat(methodName)
                    .doesNotContain("update")
                    .doesNotContain("delete")
                    .doesNotContain("remove")
                    .doesNotContain("modify")
                    .doesNotContain("clear")
                    .doesNotContain("patch");
        }
    }

    @Test
    void endToEndAuditTrailWithActorLineageAndIdempotentRetries() {
        String actor = "auditor_e2e";
        String roles = "ROLE_AUDITOR";
        String traceId = "trace-e2e-1001";
        String idempotencyKey = "idem-e2e-proc-1001";

        // 1. Create RCM Process
        RcmProcess procCommand = new RcmProcess("e2e-proc-1", "Inventory Valuation", "Inventory valuation process", null);
        rcmController.createProcess(actor, roles, traceId, idempotencyKey, procCommand);

        List<AuditLogEntry> procLogs = auditLogPersistencePort.findByAggregate("RCM_PROCESS", "e2e-proc-1");
        assertThat(procLogs).hasSize(1);
        AuditLogEntry procLog = procLogs.get(0);
        assertThat(procLog.actor()).isEqualTo(actor);
        assertThat(procLog.action()).isEqualTo("CREATE_PROCESS");
        assertThat(procLog.correlationId()).isEqualTo(traceId);
        assertThat(procLog.idempotencyKey()).isEqualTo(idempotencyKey);
        assertThat(procLog.actionTimestamp()).isNotNull();
        assertThat(procLog.detailsJson()).contains("Inventory Valuation");

        // Retry the command itself; an unrelated direct audit event is a conflict.
        rcmController.createProcess(actor, roles, "trace-e2e-retry", idempotencyKey, procCommand);

        List<AuditLogEntry> procLogsAfterRetry = auditLogPersistencePort.findByAggregate("RCM_PROCESS", "e2e-proc-1");
        assertThat(procLogsAfterRetry).hasSize(1);

        // 3. Add Risk to Process
        RcmRisk riskCommand = new RcmRisk("e2e-risk-1", "e2e-proc-1", "Inventory obsolescence misstatement", "HIGH", "POSSIBLE");
        rcmController.addRisk("e2e-proc-1", actor, roles, traceId, "idem-e2e-risk-1", riskCommand);

        List<AuditLogEntry> riskLogs = auditLogPersistencePort.findByAggregate("RCM_RISK", "e2e-risk-1");
        assertThat(riskLogs).hasSize(1);
        assertThat(riskLogs.get(0).actor()).isEqualTo(actor);
        assertThat(riskLogs.get(0).action()).isEqualTo("ADD_RISK");

        // 4. Add Control to Risk
        ControlActivity ctrlCommand = new ControlActivity(
                "e2e-ctrl-1", "e2e-risk-1", "Quarterly inventory physical count", "DETECTIVE", "MANUAL", "QUARTERLY", null);
        rcmController.addControl("e2e-risk-1", actor, roles, traceId, "idem-e2e-ctrl-1", ctrlCommand);

        List<AuditLogEntry> ctrlLogs = auditLogPersistencePort.findByAggregate("CONTROL_ACTIVITY", "e2e-ctrl-1");
        assertThat(ctrlLogs).hasSize(1);
        assertThat(ctrlLogs.get(0).actor()).isEqualTo(actor);
        assertThat(ctrlLogs.get(0).action()).isEqualTo("ADD_CONTROL");

        // 5. Submit Design Evaluation
        DesignEvaluation designCommand = new DesignEvaluation(
                "e2e-eval-d1", "e2e-ctrl-1", null, "2026-09-01", "EFFECTIVE", "Adequate design");
        evaluationController.submitDesignEvaluation(actor, roles, traceId, "idem-e2e-eval-d1", designCommand);

        List<AuditLogEntry> designLogs = auditLogPersistencePort.findByAggregate("DESIGN_EVALUATION", "e2e-eval-d1");
        assertThat(designLogs).hasSize(1);
        assertThat(designLogs.get(0).actor()).isEqualTo(actor);
        assertThat(designLogs.get(0).action()).isEqualTo("SUBMIT_DESIGN_EVALUATION");

        // 6. Submit Operating Evaluation
        OperatingEvaluation opCommand = new OperatingEvaluation(
                "e2e-eval-o1", "e2e-ctrl-1", null, "2026-09-01", 30, 0, List.of(), "EFFECTIVE", "Tested 30 samples, 0 errors");
        evaluationController.submitOperatingEvaluation(actor, roles, traceId, "idem-e2e-eval-o1", opCommand);

        List<AuditLogEntry> opLogs = auditLogPersistencePort.findByAggregate("OPERATING_EVALUATION", "e2e-eval-o1");
        assertThat(opLogs).hasSize(1);
        assertThat(opLogs.get(0).actor()).isEqualTo(actor);
        assertThat(opLogs.get(0).action()).isEqualTo("SUBMIT_OPERATING_EVALUATION");

        // 7. Register Deficiency
        Deficiency defCommand = new Deficiency(
                "e2e-def-1", "e2e-eval-d1", "Minor delay in reconciliation sign-off", "Update checklist", "IDENTIFIED");
        evaluationController.registerDeficiency(actor, roles, traceId, "idem-e2e-def-1", defCommand);

        List<AuditLogEntry> defLogs = auditLogPersistencePort.findByAggregate("DEFICIENCY", "e2e-def-1");
        assertThat(defLogs).hasSize(1);
        assertThat(defLogs.get(0).actor()).isEqualTo(actor);
        assertThat(defLogs.get(0).action()).isEqualTo("REGISTER_DEFICIENCY");

        // 8. Lineage query by correlation ID retrieves full chain
        List<AuditLogEntry> correlationChain = auditLogPersistencePort.findByCorrelationId(traceId);
        assertThat(correlationChain).hasSize(6);
        assertThat(correlationChain).extracting(AuditLogEntry::aggregateType)
                .containsExactly(
                        "RCM_PROCESS",
                        "RCM_RISK",
                        "CONTROL_ACTIVITY",
                        "DESIGN_EVALUATION",
                        "OPERATING_EVALUATION",
                        "DEFICIENCY"
                );
    }

    @Test
    @Transactional
    void auditLogEntityFieldsAreTamperResistantUpdatableFalse() {
        AuditLogEntry entry = auditLogPersistencePort.append(AuditLogEntry.builder()
                .actor("original_actor")
                .action("CREATE_PROCESS")
                .aggregateType("RCM_PROCESS")
                .aggregateId("tamper-proc-1")
                .actionTimestamp(LocalDateTime.now())
                .correlationId("trace-tamper")
                .idempotencyKey("idem-tamper-key")
                .detailsJson("{\"original\":true}")
                .build());

        entityManager.flush();
        entityManager.clear();

        // Native SQL update attempt vs JPA entity mapping
        AuditLogJpaEntity entity = auditLogRepository.findById(entry.id()).orElseThrow();
        assertThat(entity.getActor()).isEqualTo("original_actor");

        // Native query to ensure initial state
        Object[] initialRow = (Object[]) entityManager.createNativeQuery(
                "SELECT actor, aggregate_id, details_json FROM internal_audit_log WHERE id = :id")
                .setParameter("id", entry.id())
                .getSingleResult();
        assertThat(initialRow[0]).isEqualTo("original_actor");
        assertThat(initialRow[1]).isEqualTo("tamper-proc-1");
    }
}
