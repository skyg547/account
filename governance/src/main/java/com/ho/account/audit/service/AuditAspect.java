package com.ho.account.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.audit.application.model.AuditActor;
import com.ho.account.audit.application.port.in.AuditLogUseCase;
import com.ho.account.audit.application.port.out.AuditActorProviderPort;
import com.ho.account.audit.domain.AuditLoggable;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogUseCase auditLogUseCase;
    private final AuditActorProviderPort auditActorProviderPort;
    private final ObjectMapper objectMapper;

    @Around("@annotation(auditLoggable)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditLoggable auditLoggable) throws Throwable {
        String eventType = auditLoggable.eventType();
        String eventName = auditLoggable.eventName();
        if (eventName.isEmpty()) {
            eventName = joinPoint.getSignature().getName();
        }

        AuditActor actor = auditActorProviderPort.currentActor();

        String beforeData = null;
        Object[] args = joinPoint.getArgs();
        if (args.length > 0) {
            try {
                beforeData = objectMapper.writeValueAsString(args[0]);
            } catch (Exception e) {
                beforeData = "Error serializing arguments";
            }
        }

        Object result;
        String status = "SUCCESS";
        String remarks = null;
        String afterData = null;

        try {
            result = joinPoint.proceed();
            if (result != null) {
                try {
                    afterData = objectMapper.writeValueAsString(result);
                } catch (Exception e) {
                    afterData = "Error serializing result";
                }
            }
        } catch (Throwable throwable) {
            status = "FAIL";
            remarks = throwable.getMessage();
            throw throwable;
        } finally {
            auditLogUseCase.logEvent(new AuditLogUseCase.LogCommand(
                    eventType,
                    actor.userId(),
                    "SERVICE_METHOD",
                    eventName,
                    beforeData,
                    afterData,
                    status,
                    remarks,
                    actor.ipAddress()));
        }

        return result;
    }
}
