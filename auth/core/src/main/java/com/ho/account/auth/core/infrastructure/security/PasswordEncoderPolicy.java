package com.ho.account.auth.core.infrastructure.security;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * ==============================================================================
 * Password Encoder & Policy Enforcer (비밀번호 인코딩 통합 정책 검증기)
 * ==============================================================================
 * [Architecture & Security Contract]
 * 1. Unified BCrypt Structural Validation:
 *    - Spring Security DelegatingPasswordEncoder의 정확한 소문자 prefix '{bcrypt}'만 지원.
 *    - BCrypt Payload Regex: ^\$2[aby]\$[0-9]{2}\$[./0-9A-Za-z]{53}$
 *    - Cost factor: 04 ~ 31 (inclusive).
 *    - Exact length: 60 characters for BCrypt payload.
 *
 * 2. Fail-Closed & Secret Isolation:
 *    - Plaintext ({noop}), raw string, unknown prefix, malformed payload 등은 즉시 거부.
 *    - 예외 메시지나 로그에 원본 비밀번호 또는 해시 페이로드를 노출하지 않음.
 * ==============================================================================
 */
public final class PasswordEncoderPolicy {

    public static final String BCRYPT_PREFIX = "{bcrypt}";
    public static final int BCRYPT_PAYLOAD_LENGTH = 60;
    public static final int MIN_COST = 4;
    public static final int MAX_COST = 31;
    public static final String BCRYPT_REGEX = "^\\$2[aby]\\$([0-9]{2})\\$[./0-9A-Za-z]{53}$";

    private static final Pattern BCRYPT_PATTERN = Pattern.compile(BCRYPT_REGEX);

    private PasswordEncoderPolicy() {
    }

    /**
     * Stored Password의 형식을 검증하며, 유효하지 않으면 IllegalArgumentException을 발생시킵니다.
     *
     * @param storedPassword 검증할 저장 비밀번호 문자열
     * @throws IllegalArgumentException 형식이 올바르지 않은 경우
     */
    public static void validate(String storedPassword) {
        validate(storedPassword, "Stored password");
    }

    /**
     * Stored Password의 형식을 지정한 필드명/컨텍스트와 함께 검증합니다.
     *
     * @param storedPassword 검증할 저장 비밀번호 문자열
     * @param contextName    예외 메시지에 표시할 필드/컨텍스트 이름
     * @throws IllegalArgumentException 형식이 올바르지 않은 경우
     */
    public static void validate(String storedPassword, String contextName) {
        String context = normalizedContext(contextName);
        if (storedPassword == null || !storedPassword.startsWith(BCRYPT_PREFIX)) {
            throw invalidCredential(context);
        }

        validateBcryptPayload(storedPassword.substring(BCRYPT_PREFIX.length()), context);
    }

    /**
     * BCrypt 해시 페이로드(접두사 제외 60자)의 정밀 유효성을 검증합니다.
     *
     * @param payload BCrypt 해시 문자열
     * @param context 예외 메시지용 컨텍스트
     */
    public static void validateBcryptPayload(String payload, String context) {
        String normalizedContext = normalizedContext(context);
        if (payload == null || payload.length() != BCRYPT_PAYLOAD_LENGTH) {
            throw invalidCredential(normalizedContext);
        }

        Matcher matcher = BCRYPT_PATTERN.matcher(payload);
        if (!matcher.matches()) {
            throw invalidCredential(normalizedContext);
        }

        int cost = Integer.parseInt(matcher.group(1));
        if (cost < MIN_COST || cost > MAX_COST) {
            throw invalidCredential(normalizedContext);
        }
    }

    /**
     * Stored Password가 유효한 BCrypt 인코딩 형식인지 여부를 반환합니다.
     *
     * @param storedPassword 검증할 문자열
     * @return 유효하면 true, 아니면 false (예외 발생 안 함)
     */
    public static boolean isValid(String storedPassword) {
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        try {
            validate(storedPassword);
            return true;
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Stored Password에서 순수 BCrypt 페이로드를 추출합니다.
     *
     * @param storedPassword 정확한 {bcrypt} 접두사가 포함된 저장 비밀번호
     * @return 60자 BCrypt 페이로드
     */
    public static String extractPayload(String storedPassword) {
        validate(storedPassword);
        return storedPassword.substring(BCRYPT_PREFIX.length());
    }

    /**
     * BCrypt 페이로드에 표준 {bcrypt} 접두사를 붙여 반환합니다.
     *
     * @param bcryptPayload 60자 BCrypt 페이로드
     * @return {bcrypt} 접두사가 붙은 인코딩 문자열
     */
    public static String formatWithPrefix(String bcryptPayload) {
        validateBcryptPayload(bcryptPayload, "BCrypt payload");
        return BCRYPT_PREFIX + bcryptPayload;
    }

    /**
     * 평문 비밀번호를 {bcrypt} 접두사가 포함된 BCrypt 해시로 동적 생성합니다.
     *
     * @param rawPassword 평문 비밀번호
     * @return {bcrypt} 접두사가 포함된 유효한 BCrypt 해시
     */
    public static String encode(CharSequence rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword must not be null.");
        }
        return BCRYPT_PREFIX + new BCryptPasswordEncoder().encode(rawPassword);
    }

    /**
     * 특정 cost factor를 사용하여 평문 비밀번호를 {bcrypt} 접두사가 포함된 BCrypt 해시로 동적 생성합니다.
     *
     * @param rawPassword 평문 비밀번호
     * @param strength    cost factor (4 ~ 31)
     * @return {bcrypt} 접두사가 포함된 유효한 BCrypt 해시
     */
    public static String encode(CharSequence rawPassword, int strength) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("rawPassword must not be null.");
        }
        if (strength < MIN_COST || strength > MAX_COST) {
            throw new IllegalArgumentException("BCrypt strength is outside the approved policy.");
        }
        return BCRYPT_PREFIX + new BCryptPasswordEncoder(strength).encode(rawPassword);
    }

    private static String normalizedContext(String contextName) {
        return contextName == null || contextName.isBlank() ? "Stored password" : contextName.trim();
    }

    private static IllegalArgumentException invalidCredential(String context) {
        return new IllegalArgumentException(
                context + " must use the exact {bcrypt} prefix and a structurally valid BCrypt payload.");
    }
}
