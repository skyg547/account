package com.ho.account.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ho.account.audit.application.port.out.AuditLogPersistencePort;
import com.ho.account.audit.domain.AuditLog;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class TracingServiceTest {

    @Test
    void extractTraceabilityPackage_readsLogsThroughPersistencePort() {
        AuditLogPersistencePort auditLogPort = Mockito.mock(AuditLogPersistencePort.class);
        TracingService service = new TracingService(auditLogPort);
        AuditLog journalLog = auditLog("JOURNAL_ENTRY", "JE-100");

        when(auditLogPort.findByTargetEntityAndTargetId("JOURNAL_ENTRY", "JE-100"))
                .thenReturn(List.of(journalLog));

        List<AuditLog> logs = service.extractTraceabilityPackage("JOURNAL_ENTRY", "JE-100");

        assertThat(logs).containsExactly(journalLog);
        verify(auditLogPort).findByTargetEntityAndTargetId("JOURNAL_ENTRY", "JE-100");
    }

    private AuditLog auditLog(String targetEntity, String targetId) {
        AuditLog log = new AuditLog();
        log.setEventType("POST");
        log.setUserId("tester");
        log.setTargetEntity(targetEntity);
        log.setTargetId(targetId);
        log.setStatus("SUCCESS");
        return log;
    }
}
