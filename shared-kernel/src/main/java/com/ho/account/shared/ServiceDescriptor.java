package com.ho.account.shared;

public record ServiceDescriptor(
        String serviceName,
        BoundedContext context,
        String description) {
}
