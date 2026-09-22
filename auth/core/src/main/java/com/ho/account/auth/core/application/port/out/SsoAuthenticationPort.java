package com.ho.account.auth.core.application.port.out;

import com.ho.account.auth.core.application.model.SsoUserProfile;

/**
 * Boundary for an OAuth2/OIDC provider or a configuration-backed local substitute.
 */
public interface SsoAuthenticationPort {

    SsoUserProfile authenticate(String provider, String tokenOrCredential);

    /**
     * Authentication failures intentionally carry no credential or provider response details.
     */
    final class SsoAuthenticationException extends RuntimeException {

        public SsoAuthenticationException() {
            super("SSO authentication failed");
        }
    }
}
