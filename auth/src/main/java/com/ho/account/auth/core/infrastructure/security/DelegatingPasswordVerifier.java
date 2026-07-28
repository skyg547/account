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

        // @todo 레거시 평문 비밀번호를 일회성 로그인 시 해시로 승격하는 저장 포트를 추가한 뒤,
        // 운영 profile에서는 접두사 없는 storedPassword를 fail-closed로 거부해야 한다.
        return Objects.equals(rawPassword, storedPassword);
    }
}

