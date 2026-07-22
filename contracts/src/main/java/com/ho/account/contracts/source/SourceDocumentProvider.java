package com.ho.account.contracts.source;

import com.ho.account.shared.BoundedContext;
import com.ho.account.shared.DiscoverableService;
import com.ho.account.shared.ServiceCapability;
import com.ho.account.shared.ServiceDescriptor;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 전표의 lineageSourceType/lineageSourceId를 원업무 문서로 연결하는 로컬 SPI입니다.
 *
 * <p>현재 구현은 같은 Spring ApplicationContext 안의 Bean을 찾는 구조이며 Eureka나 원격 서비스
 * 디스커버리를 수행하지 않습니다. 원격 MSA에서는 별도 REST/메시지 어댑터가 이 계약을 구현해야 합니다.</p>
 *
 * <p>@todo {@code Map<String, Object>} 응답을 sourceType별 버전이 있는 조회 DTO 계약으로 교체해,
 * 필드 변경과 민감정보 노출을 컴파일/계약 테스트에서 검증할 수 있게 한다.</p>
 */
public interface SourceDocumentProvider extends DiscoverableService {

    Set<String> supportedLineageSourceTypes();

    default boolean supports(String lineageSourceType) {
        return lineageSourceType != null && supportedLineageSourceTypes().contains(lineageSourceType);
    }

    Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId);

    @Override
    default ServiceDescriptor descriptor() {
        return new ServiceDescriptor(
                serviceName(),
                boundedContext(),
                Set.of(ServiceCapability.SOURCE_DOCUMENT_LOOKUP),
                description()
        );
    }

    String serviceName();

    BoundedContext boundedContext();

    String description();
}