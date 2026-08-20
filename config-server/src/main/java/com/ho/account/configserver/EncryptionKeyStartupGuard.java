package com.ho.account.configserver;

import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;

/**
 * Config Server가 HTTP context를 만들기 전에 외부 암호화 키 계약을 검증한다.
 *
 * <p>빈 키로 서버가 기동하면 암호화 기능이 비활성화된 상태를 정상 상태로 오인할 수 있다.
 * 이 guard는 실제 키를 로그나 예외에 포함하지 않고 누락, 공백 및 앞뒤 공백 입력을 거부한다.</p>
 */
final class EncryptionKeyStartupGuard
        implements ApplicationListener<ApplicationEnvironmentPreparedEvent>, Ordered {

    static final String REQUIRED_INPUT_MESSAGE =
            "Config Server requires a non-blank ENCRYPT_KEY external input without surrounding whitespace";

    @Override
    public void onApplicationEvent(ApplicationEnvironmentPreparedEvent event) {
        requireValidKey(event.getEnvironment());
    }

    static void requireValidKey(ConfigurableEnvironment environment) {
        String encryptionKey;
        try {
            encryptionKey = environment.getProperty("encrypt.key");
        } catch (IllegalArgumentException unresolvedPlaceholder) {
            throw new IllegalStateException(REQUIRED_INPUT_MESSAGE, unresolvedPlaceholder);
        }

        if (encryptionKey == null
                || encryptionKey.isBlank()
                || !encryptionKey.equals(encryptionKey.strip())) {
            throw new IllegalStateException(REQUIRED_INPUT_MESSAGE);
        }
    }

    @Override
    public int getOrder() {
        // ConfigData가 application.yml을 적재한 뒤, context 생성 전 검증한다.
        return Ordered.LOWEST_PRECEDENCE;
    }
}
