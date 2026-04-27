package com.ho.account.shared.infrastructure;

import com.ho.account.shared.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * Spring Bean 기반의 서비스 탐색 레지스트리 구현체.
 * ApplicationContext에서 DiscoverableService를 구현한 모든 빈을 수집합니다.
 */
@Component
public class SpringServiceDiscoveryRegistry implements ServiceDiscoveryRegistry {

    private final ApplicationContext applicationContext;

    // Lombok 대신 직접 생성자 작성
    public SpringServiceDiscoveryRegistry(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Override
    public List<ServiceDescriptor> getServiceDescriptors() {
        return applicationContext.getBeansOfType(DiscoverableService.class)
                .values().stream()
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
        return applicationContext.getBeansOfType(serviceType)
                .values().stream()
                .toList();
    }
}
