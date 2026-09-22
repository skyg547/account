package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.model.SsoUserProfile;
import com.ho.account.auth.core.application.port.out.SsoAuthenticationPort;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Fail-closed port used only while SSO integration is disabled or unspecified.
 */
@Component
@ConditionalOnProperty(prefix = "auth.sso", name = "enabled", havingValue = "false", matchIfMissing = true)
public class DisabledSsoAuthenticationAdapter implements SsoAuthenticationPort {

    @Override
    public SsoUserProfile authenticate(String provider, String tokenOrCredential) {
        throw new SsoAuthenticationException();
    }
}
