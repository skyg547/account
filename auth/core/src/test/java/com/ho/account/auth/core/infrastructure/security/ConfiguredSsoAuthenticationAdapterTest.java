package com.ho.account.auth.core.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.core.application.model.SsoUserProfile;
import com.ho.account.auth.core.application.port.out.SsoAuthenticationPort;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConfiguredSsoAuthenticationAdapterTest {

    @Test
    void enabledMatchingProviderAndCredentialReturnsConfiguredProfile() {
        String credential = UUID.randomUUID().toString();
        ConfiguredSsoAuthenticationAdapter adapter = adapter(true, "corporate-oidc", credential);

        SsoUserProfile profile = adapter.authenticate("corporate-oidc", credential);

        assertThat(profile.subject()).isEqualTo("alice");
        assertThat(profile.email()).isEqualTo("alice@example.test");
        assertThat(profile.name()).isEqualTo("Alice");
        assertThat(profile.departmentCode()).isEqualTo("FIN");
        assertThat(profile.roles()).containsExactly("ROLE_PROVIDER_USER");
    }

    @Test
    void blankProviderUsesConfiguredDefaultProvider() {
        String credential = UUID.randomUUID().toString();
        ConfiguredSsoAuthenticationAdapter adapter = adapter(true, "corporate-oidc", credential);

        assertThat(adapter.authenticate(" ", credential).subject()).isEqualTo("alice");
    }

    @Test
    void disabledWrongProviderOrWrongCredentialFailsWithoutLeakingCredential() {
        String configuredCredential = UUID.randomUUID().toString();
        String suppliedCredential = UUID.randomUUID().toString();

        assertFailureDoesNotLeak(adapter(false, "corporate-oidc", configuredCredential),
                "corporate-oidc", configuredCredential);
        assertFailureDoesNotLeak(adapter(true, "corporate-oidc", configuredCredential),
                "other-provider", configuredCredential);
        assertFailureDoesNotLeak(adapter(true, "corporate-oidc", configuredCredential),
                "corporate-oidc", suppliedCredential);
    }

    private void assertFailureDoesNotLeak(
            ConfiguredSsoAuthenticationAdapter adapter, String provider, String suppliedCredential) {
        assertThatThrownBy(() -> adapter.authenticate(provider, suppliedCredential))
                .isInstanceOf(SsoAuthenticationPort.SsoAuthenticationException.class)
                .hasMessageNotContaining(suppliedCredential)
                .hasMessageNotContaining(provider);
    }

    private ConfiguredSsoAuthenticationAdapter adapter(boolean enabled, String provider, String credential) {
        AuthModuleProperties properties = new AuthModuleProperties();
        properties.getSso().setEnabled(enabled);
        properties.getSso().setDefaultProvider(provider);

        AuthModuleProperties.SsoIdentity identity = new AuthModuleProperties.SsoIdentity();
        identity.setProvider(provider);
        identity.setCredential(credential);
        identity.setSubject("alice");
        identity.setEmail("alice@example.test");
        identity.setName("Alice");
        identity.setDepartmentCode("FIN");
        identity.setRoles(List.of("ROLE_PROVIDER_USER"));
        properties.getSso().setIdentities(List.of(identity));
        return new ConfiguredSsoAuthenticationAdapter(properties);
    }
}
