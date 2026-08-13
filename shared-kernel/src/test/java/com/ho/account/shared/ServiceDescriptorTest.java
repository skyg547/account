package com.ho.account.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 서비스 명함(ServiceDescriptor)의 불변식과 capability 조회 규칙을 검증합니다.
 */
class ServiceDescriptorTest {

    @Test
    void trimsServiceNameAndDescription() {
        ServiceDescriptor descriptor = new ServiceDescriptor(
                "  journal-ledger  ",
                BoundedContext.JOURNAL_LEDGER,
                Set.of(ServiceCapability.JOURNAL_POSTING),
                "  전표 전기  ");

        assertThat(descriptor.serviceName()).isEqualTo("journal-ledger");
        assertThat(descriptor.description()).isEqualTo("전표 전기");
    }

    @Test
    void shorthandConstructorStartsWithNoCapabilities() {
        ServiceDescriptor descriptor = new ServiceDescriptor(
                "closing", BoundedContext.CLOSING, "결산");

        assertThat(descriptor.capabilities()).isEmpty();
        assertThat(descriptor.supports(ServiceCapability.JOURNAL_POSTING)).isFalse();
    }

    @Test
    void supportsReturnsTrueOnlyForDeclaredCapabilities() {
        ServiceDescriptor descriptor = new ServiceDescriptor(
                "master-data",
                BoundedContext.MASTER_DATA,
                Set.of(ServiceCapability.MASTER_DATA_QUERY, ServiceCapability.ASSET_REGISTRATION),
                "기준정보");

        assertThat(descriptor.supports(ServiceCapability.MASTER_DATA_QUERY)).isTrue();
        assertThat(descriptor.supports(ServiceCapability.ASSET_REGISTRATION)).isTrue();
        assertThat(descriptor.supports(ServiceCapability.JOURNAL_POSTING)).isFalse();
    }

    @Test
    void supportsRejectsNullCapability() {
        ServiceDescriptor descriptor = new ServiceDescriptor(
                "master-data", BoundedContext.MASTER_DATA, "기준정보");

        assertThatThrownBy(() -> descriptor.supports(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("capability");
    }

    @Test
    void defensivelyCopiesCapabilitiesSoLaterMutationCannotLeakIn() {
        Set<ServiceCapability> mutable = new HashSet<>();
        mutable.add(ServiceCapability.MASTER_DATA_QUERY);

        ServiceDescriptor descriptor = new ServiceDescriptor(
                "master-data", BoundedContext.MASTER_DATA, mutable, "기준정보");

        mutable.add(ServiceCapability.JOURNAL_POSTING);

        assertThat(descriptor.capabilities()).containsExactly(ServiceCapability.MASTER_DATA_QUERY);
        assertThat(descriptor.supports(ServiceCapability.JOURNAL_POSTING)).isFalse();
    }

    @Test
    void exposesUnmodifiableCapabilities() {
        ServiceDescriptor descriptor = new ServiceDescriptor(
                "master-data",
                BoundedContext.MASTER_DATA,
                Set.of(ServiceCapability.MASTER_DATA_QUERY),
                "기준정보");

        assertThatThrownBy(() -> descriptor.capabilities().add(ServiceCapability.JOURNAL_POSTING))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsBlankServiceName() {
        assertThatThrownBy(() -> new ServiceDescriptor(
                "   ", BoundedContext.MASTER_DATA, Set.of(), "기준정보"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serviceName must not be blank");
    }

    @Test
    void rejectsBlankDescription() {
        assertThatThrownBy(() -> new ServiceDescriptor(
                "master-data", BoundedContext.MASTER_DATA, Set.of(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("description must not be blank");
    }

    @Test
    void rejectsNullContext() {
        assertThatThrownBy(() -> new ServiceDescriptor(
                "master-data", null, Set.of(), "기준정보"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("context");
    }

    @Test
    void rejectsNullCapabilities() {
        assertThatThrownBy(() -> new ServiceDescriptor(
                "master-data", BoundedContext.MASTER_DATA, null, "기준정보"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("capabilities");
    }

    @Test
    void treatsDescriptorsWithSameStateAsEqual() {
        ServiceDescriptor one = new ServiceDescriptor(
                "master-data", BoundedContext.MASTER_DATA, Set.of(ServiceCapability.MASTER_DATA_QUERY), "기준정보");
        ServiceDescriptor other = new ServiceDescriptor(
                "  master-data  ", BoundedContext.MASTER_DATA, Set.of(ServiceCapability.MASTER_DATA_QUERY), "  기준정보  ");

        assertThat(one).isEqualTo(other);
        assertThat(one).hasSameHashCodeAs(other);
    }
}
