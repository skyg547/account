package com.ho.account.shared;

public interface DiscoverableService {

    ServiceDescriptor descriptor();

    default String serviceName() {
        return descriptor().serviceName();
    }

    default BoundedContext boundedContext() {
        return descriptor().context();
    }

    default boolean supports(ServiceCapability capability) {
        return descriptor().supports(capability);
    }
}
