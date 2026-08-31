package com.ho.account.auth.core.infrastructure.security;

import com.ho.account.auth.core.application.port.out.PasswordVerifierPort;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * ==============================================================================
 * Delegating Password Verifier (위임형 비밀번호 검증기)
 * ==============================================================================
 * [Architecture & Security Contract]
 * 1. Fail-Closed Validation:
 *    - PasswordEncoderPolicy를 통해 사전 구조/포맷/cost 검증을 수행.
 *    - malformed 문자열, {noop}, 미지원 접두사, 잘못된 cost/길이는 false 반환.
 *
 * 2. Secret & Exception Isolation:
 *    - 비밀번호 검증 실패 시 내부 500 에러를 유발하지 않고 false 반환.
 *    - 비밀번호 내용을 로그에 남기지 않음.
 * ==============================================================================
 */
@Component
public class DelegatingPasswordVerifier implements PasswordVerifierPort {

    private final PasswordEncoder passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    @Override
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null || rawPassword.isBlank() || storedPassword.isBlank()) {
            return false;
        }

        if (!PasswordEncoderPolicy.isValid(storedPassword)) {
            return false;
        }

        try {
            String trimmedStored = storedPassword.trim();
            if (trimmedStored.startsWith("{")) {
                return passwordEncoder.matches(rawPassword, trimmedStored);
            }
            return passwordEncoder.matches(rawPassword, PasswordEncoderPolicy.BCRYPT_PREFIX + trimmedStored);
        } catch (Exception ex) {
            // Fail closed without leaking secret contents
            return false;
        }
    }
}

