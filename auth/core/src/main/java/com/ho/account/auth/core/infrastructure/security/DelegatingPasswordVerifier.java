package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.port.out.PasswordVerifierPort;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DelegatingPasswordVerifier implements PasswordVerifierPort {

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Override
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || rawPassword.isBlank() || storedPassword.isBlank()) {
            return false;
        }

        String trimmedStored = storedPassword.trim();
        if (!trimmedStored.startsWith("{")) {
            return false;
        }

        int closingBrace = trimmedStored.indexOf('}');
        if (closingBrace <= 1) {
            return false;
        }

        String id = trimmedStored.substring(1, closingBrace).trim();
        if (id.isEmpty() || "noop".equalsIgnoreCase(id)) {
            return false;
        }

        String payload = trimmedStored.substring(closingBrace + 1);
        if (payload.isBlank()) {
            return false;
        }

        try {
            return passwordEncoder.matches(rawPassword, trimmedStored);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }
}
