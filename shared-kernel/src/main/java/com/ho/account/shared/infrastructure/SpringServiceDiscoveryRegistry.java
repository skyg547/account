package com.ho.account.shared.infrastructure;

import com.ho.account.shared.BoundedContext;
import com.ho.account.shared.DiscoverableService;
import com.ho.account.shared.ServiceCapability;
import com.ho.account.shared.ServiceDescriptor;
import com.ho.account.shared.ServiceDiscoveryRegistry;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Spring Bean 기반의 로컬 capability 레지스트리 어댑터입니다.
 *
 * <p>ApplicationContext를 매 조회마다 탐색하지 않고 생성자에서 주입된 Bean을 불변 스냅샷으로
 * 보관합니다. 같은 서비스 이름이 두 번 등록되면 어떤 구현을 선택할지 모호하므로 시작 시 실패합니다.</p>
 */
@Component
public class SpringServiceDiscoveryRegistry implements ServiceDiscoveryRegistry {

    private final List<DiscoverableService> services;
    private final List<ServiceDescriptor> descriptors;

    public SpringServiceDiscoveryRegistry(List<DiscoverableService> services) {
        this.services = List.copyOf(Objects.requireNonNull(services, "services must not be null"));
        this.descriptors = this.services.stream()
                .map(DiscoverableService::descriptor)
                .sorted((left, right) -> left.serviceName().compareTo(right.serviceName()))
                .toList();
        validateUniqueServiceNames(descriptors);
    }

    @Override
    public List<ServiceDescriptor> getServiceDescriptors() {
        return descriptors;
    }

    @Override
    public Optional<ServiceDescriptor> findDescriptor(String serviceName) {
        if (serviceName == null || serviceName.isBlank()) {
            return Optional.empty();
        }
        String normalizedName = serviceName.trim();
        return descriptors.stream()
                .filter(descriptor -> descriptor.serviceName().equals(normalizedName))
                .findFirst();
    }

    @Override
    public List<ServiceDescriptor> findByContext(BoundedContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return descriptors.stream()
                .filter(descriptor -> descriptor.context() == context)
                .toList();
    }

    @Override
    public List<ServiceDescriptor> findByCapability(ServiceCapability capability) {
        Objects.requireNonNull(capability, "capability must not be null");
        return descriptors.stream()
                .filter(descriptor -> descriptor.supports(capability))
                .toList();
    }

    @Override
    public <T extends DiscoverableService> List<T> getServices(Class<T> serviceType) {
        Objects.requireNonNull(serviceType, "serviceType must not be null");
        return services.stream()
                .filter(serviceType::isInstance)
                .map(serviceType::cast)
                .toList();
    }

    private void validateUniqueServiceNames(List<ServiceDescriptor> serviceDescriptors) {
        Set<String> serviceNames = new HashSet<>();
        for (ServiceDescriptor descriptor : serviceDescriptors) {
            if (!serviceNames.add(descriptor.serviceName())) {
                throw new IllegalStateException(
                        "Duplicate discoverable service name: " + descriptor.serviceName());
            }
        }
    }
}