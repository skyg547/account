package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.port.out.PasswordVerifierPort;
import java.util.Objects;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DelegatingPasswordVerifier implements PasswordVerifierPort {

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Override
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }

        if (storedPassword.startsWith("{")) {
            try {
                return passwordEncoder.matches(rawPassword, storedPassword);
            } catch (IllegalArgumentException ex) {
                return false;
            }
        }

        // 레거시 평문 비밀번호 허용 코드를 제거하고 보안을 강화합니다.
        // 모든 비밀번호는 Spring Security의 포맷팅(예: {bcrypt})을 준수해야 합니다.
        return false;
    }
}

