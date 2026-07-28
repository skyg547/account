package com.ho.account.audit.service;

import com.ho.account.audit.application.port.in.AuditLogUseCase;
import com.ho.account.shared.audit.AuditLogPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LocalAuditLogPublisher implements AuditLogPublisher {

    private final AuditLogUseCase auditLogUseCase;

    @Override
    public void publish(LogCommand command) {
        auditLogUseCase.logEvent(new AuditLogUseCase.LogCommand(
                command.eventType(),
                command.userId(),
                command.source(),
                command.eventName(),
                command.beforeData(),
                command.afterData(),
                command.status(),
                command.remarks(),
                command.ipAddress()
        ));
    }
}
