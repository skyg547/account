package com.ho.account.shared;

import java.util.List;
import java.util.Optional;

/**
 * 현재 Spring 프로세스 안에 등록된 {@link DiscoverableService}를 capability 기준으로 찾는 로컬 레지스트리입니다.
 *
 * <p>Eureka/Consul처럼 다른 컨테이너의 주소를 찾는 네트워크 서비스 디스커버리가 아닙니다.</p>
 *
 * <p>@todo 원격 서비스 검색과 혼동되지 않도록 {@code LocalServiceCapabilityRegistry}로 이름을 바꾸고,
 * contracts의 source document SPI와 함께 최소 모듈로 분리한다.</p>
 */
public interface ServiceDiscoveryRegistry {

    List<ServiceDescriptor> getServiceDescriptors();

    Optional<ServiceDescriptor> findDescriptor(String serviceName);

    List<ServiceDescriptor> findByContext(BoundedContext context);

    List<ServiceDescriptor> findByCapability(ServiceCapability capability);

    <T extends DiscoverableService> List<T> getServices(Class<T> serviceType);
}