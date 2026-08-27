package com.ho.account.auth.core.infrastructure.security;

import java.util.Set;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * ==============================================================================
 * Password Encoder Policy (비밀번호 인코딩 단일 계약 정책)
 * ==============================================================================
 * [Architecture & Fail-Closed Security Policy]
 * 1. 단일 인코딩 정책 정의 (Single Encoding Policy):
 *    - 지원되는 Password Encoder ID: "bcrypt"
 *    - {noop} 등 평문(Plaintext) 인코더는 fail-closed 보안 원칙에 따라 startup 및 entity 저장 시점에 절대 허용하지 않습니다.
 *
 * 2. 계약 형식 (Contract Format):
 *    - '{id}encodedPayload' 형태를 엄격히 강제합니다.
 *    - raw 문자열 (접두사 없음), malformed prefix ({}, { }), unknown id ({unknown}),
 *      빈 payload ({bcrypt})는 bootstrap 및 Entity 영속화 전에 즉시 거부(Fail-Closed)됩니다.
 * ==============================================================================
 */
public final class PasswordEncoderPolicy {

    public static final String DEFAULT_ENCODER_ID = "bcrypt";
    public static final Set<String> SUPPORTED_ENCODER_IDS = Set.of(DEFAULT_ENCODER_ID);

    private static final PasswordEncoder DELEGATING_PASSWORD_ENCODER = PasswordEncoderFactories.createDelegatingPasswordEncoder();

    private PasswordEncoderPolicy() {
    }

    public static PasswordEncoder getDelegatingPasswordEncoder() {
        return DELEGATING_PASSWORD_ENCODER;
    }

    /**
     * 비밀번호 문자열이 유효한 인코딩 계약을 준수하는지 검증하고, 유효한 경우 trim된 문자열을 반환합니다.
     *
     * @param password 검증할 비밀번호 문자열
     * @param context 예외 메시지에 표기할 컨텍스트 설명
     * @return 유효성이 검증된 비밀번호 문자열
     * @throws IllegalStateException 계약 위반 시 Fail-Closed 예외 발생
     */
    public static String requireValidEncodedPassword(String password, String context) {
        if (password == null || password.isBlank()) {
            throw new IllegalStateException(context + ": password must not be null or blank.");
        }
        String trimmed = password.trim();
        if (!trimmed.startsWith("{")) {
            throw new IllegalStateException(
                    context + ": raw plaintext password is strictly prohibited under fail-closed security policy. Must use format '{<encoder_id>}<encoded_payload>' (e.g. {bcrypt}...).");
        }
        int closeBraceIndex = trimmed.indexOf('}');
        if (closeBraceIndex <= 1) {
            throw new IllegalStateException(
                    context + ": malformed password encoder prefix. Must use format '{<encoder_id>}<encoded_payload>'.");
        }
        String encoderId = trimmed.substring(1, closeBraceIndex).trim();
        if (encoderId.equalsIgnoreCase("noop")) {
            throw new IllegalStateException(
                    context + ": '{noop}' plaintext password encoding is strictly prohibited under fail-closed security policy.");
        }
        if (!SUPPORTED_ENCODER_IDS.contains(encoderId)) {
            throw new IllegalStateException(
                    context + ": unsupported password encoder id '" + encoderId + "'. Supported encoder ids: " + SUPPORTED_ENCODER_IDS);
        }
        String payload = trimmed.substring(closeBraceIndex + 1);
        if (payload.isBlank()) {
            throw new IllegalStateException(
                    context + ": password payload after '{" + encoderId + "}' must not be blank.");
        }
        return trimmed;
    }
}