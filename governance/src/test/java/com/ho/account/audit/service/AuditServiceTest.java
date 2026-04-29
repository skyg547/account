package com.ho.account.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ho.account.audit.application.port.in.AuditLogUseCase;
import com.ho.account.audit.application.port.in.AuthorizationUseCase;
import com.ho.account.audit.application.port.out.AuditLogPersistencePort;
import com.ho.account.audit.application.port.out.AuthorizationPersistencePort;
import com.ho.account.audit.application.port.out.SystemRolePersistencePort;
import com.ho.account.audit.domain.AuditLog;
import com.ho.account.audit.domain.SystemRole;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class AuditServiceTest {

    @Test
    void logEvent_truncatesRemarksToMaxLength() {
        AuditLogPersistencePort auditLogPort = Mockito.mock(AuditLogPersistencePort.class);
        SystemRolePersistencePort rolePort = Mockito.mock(SystemRolePersistencePort.class);
        AuthorizationPersistencePort authPort = Mockito.mock(AuthorizationPersistencePort.class);
        AuditService service = new AuditService(auditLogPort, rolePort, authPort);

        when(auditLogPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0, AuditLog.class));

        String longRemarks = "x".repeat(1200);
        AuditLog saved = service.logEvent(new AuditLogUseCase.LogCommand(
                "UPDATE",
                "tester",
                "MASTER",
                "A-100",
                "{}",
                "{\"ok\":true}",
                "SUCCESS",
                longRemarks,
                "127.0.0.1"));

        assertThat(saved.getRemarks()).hasSize(1000);
    }

    @Test
    void createRole_throwsWhenRoleCodeAlreadyExists() {
        AuditLogPersistencePort auditLogPort = Mockito.mock(AuditLogPersistencePort.class);
        SystemRolePersistencePort rolePort = Mockito.mock(SystemRolePersistencePort.class);
        AuthorizationPersistencePort authPort = Mockito.mock(AuthorizationPersistencePort.class);
        AuditService service = new AuditService(auditLogPort, rolePort, authPort);

        when(rolePort.existsByRoleCode("FIN_APPROVER")).thenReturn(true);

        assertThatThrownBy(() -> service.createRole(new AuthorizationUseCase.CreateRoleCommand(
                "FIN_APPROVER",
                "Finance Approver",
                "approval role")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("이미 존재하는 역할 코드");
    }

    @Test
    void createRole_savesRoleWhenCodeIsNew() {
        AuditLogPersistencePort auditLogPort = Mockito.mock(AuditLogPersistencePort.class);
        SystemRolePersistencePort rolePort = Mockito.mock(SystemRolePersistencePort.class);
        AuthorizationPersistencePort authPort = Mockito.mock(AuthorizationPersistencePort.class);
        AuditService service = new AuditService(auditLogPort, rolePort, authPort);

        when(rolePort.existsByRoleCode("FIN_APPROVER")).thenReturn(false);
        when(rolePort.save(any())).thenAnswer(invocation -> invocation.getArgument(0, SystemRole.class));

        SystemRole created = service.createRole(new AuthorizationUseCase.CreateRoleCommand(
                "FIN_APPROVER",
                "Finance Approver",
                "approval role"));

        assertThat(created.getRoleCode()).isEqualTo("FIN_APPROVER");
        assertThat(created.getRoleName()).isEqualTo("Finance Approver");
    }
}

