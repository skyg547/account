package com.ho.account.configserver.health;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * readiness가 실제로 조회할 대표 설정의 식별자입니다.
 *
 * <p>초보자 설명: 본사 건물이 열렸는지만 보지 않고, 대표 레시피 한 장을 실제로 꺼낼 수 있는지 확인합니다.</p>
 */
@ConfigurationProperties(prefix = "config-server.repository-probe")
public record ConfigRepositoryProbeProperties(
        String application,
        String profile,
        String label
) {

    public ConfigRepositoryProbeProperties {
        application = requireText(application, "application");
        profile = requireText(profile, "profile");
        label = StringUtils.hasText(label) ? label.trim() : null;
    }

    private static String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException("config-server.repository-probe." + fieldName + " must not be blank");
        }
        return value.trim();
    }
}