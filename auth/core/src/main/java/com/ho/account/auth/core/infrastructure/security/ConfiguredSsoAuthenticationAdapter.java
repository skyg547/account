package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.model.SsoUserProfile;
import com.ho.account.auth.core.application.port.out.SsoAuthenticationPort;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Local-profile configuration-backed provider for controlled development and testing.
 * Production must supply a real OAuth2/OIDC outbound adapter when SSO is enabled.
 */
@Component
@Profile("local")
@ConditionalOnProperty(prefix = "auth.sso", name = "enabled", havingValue = "true")
public class ConfiguredSsoAuthenticationAdapter implements SsoAuthenticationPort {

    private final AuthModuleProperties properties;

    public ConfiguredSsoAuthenticationAdapter(AuthModuleProperties properties) {
        this.properties = properties;
    }

    @Override
    public SsoUserProfile authenticate(String provider, String tokenOrCredential) {
        AuthModuleProperties.Sso sso = properties.getSso();
        if (!sso.isEnabled() || tokenOrCredential == null || tokenOrCredential.isBlank()) {
            throw new SsoAuthenticationException();
        }

        String selectedProvider = provider == null || provider.isBlank()
                ? sso.getDefaultProvider()
                : provider.trim();
        byte[] suppliedCredential = tokenOrCredential.getBytes(StandardCharsets.UTF_8);
        AuthModuleProperties.SsoIdentity matchedIdentity = null;
        for (AuthModuleProperties.SsoIdentity identity : sso.getIdentities()) {
            if (identity != null
                    && selectedProvider.equalsIgnoreCase(identity.getProvider())
                    && identity.getCredential() != null
                    && MessageDigest.isEqual(
                            identity.getCredential().getBytes(StandardCharsets.UTF_8),
                            suppliedCredential)) {
                matchedIdentity = identity;
            }
        }
        if (matchedIdentity == null) {
            throw new SsoAuthenticationException();
        }

        return new SsoUserProfile(
                matchedIdentity.getSubject(),
                matchedIdentity.getEmail(),
                matchedIdentity.getName(),
                matchedIdentity.getDepartmentCode(),
                matchedIdentity.getRoles());
    }
}
