package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.port.out.PasswordVerifierPort;
import org.springframework.stereotype.Component;

@Component
public class DelegatingPasswordVerifier implements PasswordVerifierPort {

    @Override
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        try {
            PasswordEncoderPolicy.requireValidEncodedPassword(storedPassword, "Password verification");
            return PasswordEncoderPolicy.getDelegatingPasswordEncoder().matches(rawPassword, storedPassword);
        } catch (Exception ex) {
            return false;
        }
    }
}

