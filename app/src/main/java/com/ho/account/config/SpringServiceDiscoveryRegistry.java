package com.ho.account.config;

import com.ho.account.shared.BoundedContext;
import com.ho.account.shared.DiscoverableService;
import com.ho.account.shared.ServiceCapability;
import com.ho.account.shared.ServiceDescriptor;
import com.ho.account.shared.ServiceDiscoveryRegistry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class SpringServiceDiscoveryRegistry implements ServiceDiscoveryRegistry {

    private final List<DiscoverableService> discoverableServices;

    public SpringServiceDiscoveryRegistry(List<DiscoverableService> discoverableServices) {
        this.discoverableServices = deduplicate(discoverableServices);
    }

    @Override
    public List<ServiceDescriptor> getServiceDescriptors() {
        return discoverableServices.stream()
                .map(DiscoverableService::descriptor)
                .toList();
    }

    @Override
    public Optional<ServiceDescriptor> findDescriptor(String serviceName) {
        return getServiceDescriptors().stream()
                .filter(descriptor -> descriptor.serviceName().equals(serviceName))
                .findFirst();
    }

    @Override
    public List<ServiceDescriptor> findByContext(BoundedContext context) {
        return getServiceDescriptors().stream()
                .filter(descriptor -> descriptor.context() == context)
                .toList();
    }

    @Override
    public List<ServiceDescriptor> findByCapability(ServiceCapability capability) {
        return getServiceDescriptors().stream()
                .filter(descriptor -> descriptor.supports(capability))
                .toList();
    }

    @Override
    public <T extends DiscoverableService> List<T> getServices(Class<T> serviceType) {
        return discoverableServices.stream()
                .filter(serviceType::isInstance)
                .map(serviceType::cast)
                .toList();
    }

    private static List<DiscoverableService> deduplicate(List<DiscoverableService> discoverableServices) {
        Map<String, DiscoverableService> servicesByName = new LinkedHashMap<>();
        for (DiscoverableService discoverableService : discoverableServices) {
            servicesByName.putIfAbsent(discoverableService.serviceName(), discoverableService);
        }
        return List.copyOf(servicesByName.values());
    }
}
