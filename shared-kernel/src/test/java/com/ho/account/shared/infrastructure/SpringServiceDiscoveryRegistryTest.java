package com.ho.account.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.shared.BoundedContext;
import com.ho.account.shared.DiscoverableService;
import com.ho.account.shared.ServiceCapability;
import com.ho.account.shared.ServiceDescriptor;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SpringServiceDiscoveryRegistryTest {

    @Test
    void storesAnImmutableDeterministicSnapshotAndFiltersCapabilities() {
        TestService second = service(
                "z-service", BoundedContext.LOAN, ServiceCapability.JOURNAL_POSTING);
        TestService first = service(
                "a-service", BoundedContext.JOURNAL_LEDGER,
                ServiceCapability.SOURCE_DOCUMENT_LOOKUP);

        SpringServiceDiscoveryRegistry registry =
                new SpringServiceDiscoveryRegistry(List.of(second, first));

        assertThat(registry.getServiceDescriptors())
                .extracting(ServiceDescriptor::serviceName)
                .containsExactly("a-service", "z-service");
        assertThat(registry.findDescriptor(" a-service ")).contains(first.descriptor());
        assertThat(registry.findByContext(BoundedContext.LOAN))
                .extracting(ServiceDescriptor::serviceName)
                .containsExactly("z-service");
        assertThat(registry.findByCapability(ServiceCapability.SOURCE_DOCUMENT_LOOKUP))
                .extracting(ServiceDescriptor::serviceName)
                .containsExactly("a-service");
        assertThat(registry.getServices(TestService.class)).containsExactly(second, first);
        assertThatThrownBy(() -> registry.getServiceDescriptors().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsDuplicateOrBlankServiceNames() {
        TestService first =
                service("duplicate", BoundedContext.LOAN, ServiceCapability.JOURNAL_POSTING);
        TestService second =
                service("duplicate", BoundedContext.CLOSING, ServiceCapability.JOURNAL_POSTING);

        assertThatThrownBy(() -> new SpringServiceDiscoveryRegistry(List.of(first, second)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate");
        assertThatThrownBy(() -> new ServiceDescriptor(
                " ", BoundedContext.LOAN, Set.of(), "description"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serviceName");
    }

    private TestService service(
            String name,
            BoundedContext context,
            ServiceCapability capability) {
        return new TestService(new ServiceDescriptor(
                name,
                context,
                Set.of(capability),
                name + " description"));
    }

    private record TestService(ServiceDescriptor descriptor) implements DiscoverableService {
    }
}