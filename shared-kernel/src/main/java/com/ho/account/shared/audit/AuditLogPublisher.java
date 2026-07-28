package com.ho.account.shared.audit;

public interface AuditLogPublisher {
    void publish(LogCommand command);

    record LogCommand(
            String eventType,
            String userId,
            String source,
            String eventName,
            String beforeData,
            String afterData,
            String status,
            String remarks,
            String ipAddress
    ) {}
}
