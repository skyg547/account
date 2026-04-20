package com.ho.account.shared;

import java.util.Objects;
import java.util.Set;

public record ServiceDescriptor(
        String serviceName,
        BoundedContext context,
        Set<ServiceCapability> capabilities,
        String description) {

    public ServiceDescriptor {
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(context, "context must not be null");
        capabilities = Set.copyOf(Objects.requireNonNull(capabilities, "capabilities must not be null"));
        Objects.requireNonNull(description, "description must not be null");
    }

    public ServiceDescriptor(String serviceName,
                             BoundedContext context,
                             String description) {
        this(serviceName, context, Set.of(), description);
    }

    public boolean supports(ServiceCapability capability) {
        return capabilities.contains(capability);
    }
}
