package com.ho.account.shared;

import java.util.Objects;
import java.util.Set;

/**
 * 같은 애플리케이션 컨텍스트 안에서 제공되는 업무 capability의 명함입니다.
 */
public record ServiceDescriptor(
        String serviceName,
        BoundedContext context,
        Set<ServiceCapability> capabilities,
        String description) {

    public ServiceDescriptor {
        serviceName = requireText(serviceName, "serviceName");
        context = Objects.requireNonNull(context, "context must not be null");
        capabilities = Set.copyOf(
                Objects.requireNonNull(capabilities, "capabilities must not be null"));
        description = requireText(description, "description");
    }

    public ServiceDescriptor(
            String serviceName,
            BoundedContext context,
            String description) {
        this(serviceName, context, Set.of(), description);
    }

    public boolean supports(ServiceCapability capability) {
        return capabilities.contains(
                Objects.requireNonNull(capability, "capability must not be null"));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value.trim();
    }
}