package com.ho.account.common.service;

import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.shared.ServiceCapability;
import com.ho.account.shared.ServiceDescriptor;
import com.ho.account.shared.ServiceDiscoveryRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SourceDocumentService {

    private final ServiceDiscoveryRegistry serviceDiscoveryRegistry;

    public SourceDocumentService(ServiceDiscoveryRegistry serviceDiscoveryRegistry) {
        this.serviceDiscoveryRegistry = serviceDiscoveryRegistry;
    }

    public Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId) {
        return serviceDiscoveryRegistry.getServices(SourceDocumentProvider.class).stream()
                .filter(provider -> provider.supports(lineageSourceType))
                .findFirst()
                .flatMap(provider -> provider.getSourceDocument(lineageSourceType, lineageSourceId));
    }

    public List<ServiceDescriptor> getAvailableProviders() {
        return serviceDiscoveryRegistry.findByCapability(ServiceCapability.SOURCE_DOCUMENT_LOOKUP);
    }
}
