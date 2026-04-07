package com.ho.account.audit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ho.account.audit.domain.AuditLoggable;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditAspect {

    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AuditAspect(AuditService auditService, ObjectMapper objectMapper) {
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Around("@annotation(auditLoggable)")
    public Object audit(ProceedingJoinPoint joinPoint, AuditLoggable auditLoggable) throws Throwable {
        String eventType = auditLoggable.eventType();
        String eventName = auditLoggable.eventName();
        if (eventName.isEmpty()) {
            eventName = joinPoint.getSignature().getName();
        }

        String userId = "SYSTEM"; // In a real system, get from SecurityContext
        String ipAddress = "0.0.0.0";

        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            ipAddress = request.getRemoteAddr();
            // userId = (String) request.getSession().getAttribute("userId"); // Placeholder
        }

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
            auditService.logEvent(eventType, userId, "SERVICE_METHOD", eventName, beforeData, afterData, status,
                    remarks, ipAddress);
        }

        return result;
    }
}
