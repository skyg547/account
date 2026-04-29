package com.ho.account.audit.infrastructure.security;

import com.ho.account.audit.application.model.AuditActor;
import com.ho.account.audit.application.port.out.AuditActorProviderPort;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class RequestAuditActorAdapter implements AuditActorProviderPort {

    private static final String DEFAULT_USER = "SYSTEM";
    private static final String DEFAULT_IP = "0.0.0.0";

    @Override
    public AuditActor currentActor() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return new AuditActor(DEFAULT_USER, DEFAULT_IP);
        }

        HttpServletRequest request = attributes.getRequest();
        String ipAddress = resolveIpAddress(request);
        String userId = resolveUserId(request);
        return new AuditActor(userId, ipAddress);
    }

    private String resolveUserId(HttpServletRequest request) {
        String headerUser = normalize(request.getHeader("X-User-ID"));
        if (headerUser != null) {
            return headerUser;
        }

        String remoteUser = normalize(request.getRemoteUser());
        if (remoteUser != null) {
            return remoteUser;
        }

        Principal principal = request.getUserPrincipal();
        if (principal != null) {
            String principalName = normalize(principal.getName());
            if (principalName != null) {
                return principalName;
            }
        }
        return DEFAULT_USER;
    }

    private String resolveIpAddress(HttpServletRequest request) {
        String forwardedFor = normalize(request.getHeader("X-Forwarded-For"));
        if (forwardedFor != null) {
            int commaIndex = forwardedFor.indexOf(',');
            return commaIndex > 0 ? forwardedFor.substring(0, commaIndex).trim() : forwardedFor;
        }

        String remoteAddress = normalize(request.getRemoteAddr());
        return remoteAddress != null ? remoteAddress : DEFAULT_IP;
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}

