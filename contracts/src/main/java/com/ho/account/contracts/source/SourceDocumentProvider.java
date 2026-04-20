package com.ho.account.contracts.source;

import com.ho.account.shared.BoundedContext;
import com.ho.account.shared.DiscoverableService;
import com.ho.account.shared.ServiceCapability;
import com.ho.account.shared.ServiceDescriptor;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface SourceDocumentProvider extends DiscoverableService {

    Set<String> supportedLineageSourceTypes();

    default boolean supports(String lineageSourceType) {
        return supportedLineageSourceTypes().contains(lineageSourceType);
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
