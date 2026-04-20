package com.ho.account.shared;

import java.util.List;
import java.util.Optional;

public interface ServiceDiscoveryRegistry {

    List<ServiceDescriptor> getServiceDescriptors();

    Optional<ServiceDescriptor> findDescriptor(String serviceName);

    List<ServiceDescriptor> findByContext(BoundedContext context);

    List<ServiceDescriptor> findByCapability(ServiceCapability capability);

    <T extends DiscoverableService> List<T> getServices(Class<T> serviceType);
}
