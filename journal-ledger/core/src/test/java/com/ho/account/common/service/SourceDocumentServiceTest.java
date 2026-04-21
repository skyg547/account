package com.ho.account.common.service;

import com.ho.account.contracts.source.SourceDocumentProvider;
import com.ho.account.shared.BoundedContext;
import com.ho.account.shared.DiscoverableService;
import com.ho.account.shared.ServiceCapability;
import com.ho.account.shared.ServiceDescriptor;
import com.ho.account.shared.ServiceDiscoveryRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SourceDocumentServiceTest {

    @Test
    void returnsDocumentFromDiscoveredProvider() {
        StubSourceDocumentProvider provider = new StubSourceDocumentProvider();
        SourceDocumentService service = new SourceDocumentService(new StubServiceDiscoveryRegistry(List.of(provider)));

        Optional<Map<String, Object>> document = service.getSourceDocument("TEST_SOURCE", "DOC-1");

        assertThat(document).isPresent();
        assertThat(document.orElseThrow()).containsEntry("id", "DOC-1");
        assertThat(service.getAvailableProviders())
                .extracting(ServiceDescriptor::serviceName)
                .containsExactly("stub-source-provider");
    }

    @Test
    void returnsEmptyWhenNoProviderSupportsType() {
        SourceDocumentService service = new SourceDocumentService(
                new StubServiceDiscoveryRegistry(List.of(new StubSourceDocumentProvider())));

        Optional<Map<String, Object>> document = service.getSourceDocument("UNKNOWN_SOURCE", "DOC-1");

        assertThat(document).isEmpty();
    }

    private static final class StubServiceDiscoveryRegistry implements ServiceDiscoveryRegistry {

        private final List<DiscoverableService> services;

        private StubServiceDiscoveryRegistry(List<DiscoverableService> services) {
            this.services = services;
        }

        @Override
        public List<ServiceDescriptor> getServiceDescriptors() {
            return services.stream()
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
            return services.stream()
                    .filter(serviceType::isInstance)
                    .map(serviceType::cast)
                    .toList();
        }
    }

    private static final class StubSourceDocumentProvider implements SourceDocumentProvider {

        @Override
        public Set<String> supportedLineageSourceTypes() {
            return Set.of("TEST_SOURCE");
        }

        @Override
        public Optional<Map<String, Object>> getSourceDocument(String lineageSourceType, String lineageSourceId) {
            return Optional.of(Map.of("id", lineageSourceId, "type", lineageSourceType));
        }

        @Override
        public String serviceName() {
            return "stub-source-provider";
        }

        @Override
        public BoundedContext boundedContext() {
            return BoundedContext.JOURNAL_LEDGER;
        }

        @Override
        public String description() {
            return "Test provider";
        }
    }
}
