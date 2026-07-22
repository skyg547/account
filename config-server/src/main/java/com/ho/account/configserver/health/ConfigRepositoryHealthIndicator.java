package com.ho.account.configserver.health;

import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.stereotype.Component;

/**
 * Config Server가 대표 설정을 실제로 제공할 수 있는지 확인하는 readiness 경계입니다.
 *
 * <p>Spring 기본 health는 빈 Environment도 정상 조회로 볼 수 있습니다. 이 프로젝트는 뒤쪽 서비스가
 * 빈 중앙 설정으로 시작하는 것을 막기 위해 property source가 하나 이상일 때만 준비 완료로 판단합니다.</p>
 */
@Component
public class ConfigRepositoryHealthIndicator extends AbstractHealthIndicator {

    private static final String LOOKUP_FAILED = "repository lookup failed";
    private static final String PROPERTY_SOURCE_MISSING = "representative property source is missing";

    private final EnvironmentRepository environmentRepository;
    private final ConfigRepositoryProbeProperties probeProperties;

    public ConfigRepositoryHealthIndicator(
            EnvironmentRepository environmentRepository,
            ConfigRepositoryProbeProperties probeProperties
    ) {
        this.environmentRepository = environmentRepository;
        this.probeProperties = probeProperties;
    }

    @Override
    protected void doHealthCheck(Health.Builder builder) {
        Environment environment;
        try {
            environment = environmentRepository.findOne(
                    probeProperties.application(),
                    probeProperties.profile(),
                    probeProperties.label(),
                    false
            );
        } catch (RuntimeException exception) {
            // 외부 저장소 예외 원문에는 경로나 URL이 들어갈 수 있으므로 health detail에 전달하지 않습니다.
            markDown(builder, LOOKUP_FAILED);
            return;
        }

        int sourceCount = environment == null ? 0 : environment.getPropertySources().size();
        if (sourceCount == 0) {
            markDown(builder, PROPERTY_SOURCE_MISSING);
            return;
        }

        builder.up()
                .withDetail("application", probeProperties.application())
                .withDetail("profile", probeProperties.profile())
                .withDetail("sourceCount", sourceCount);
    }

    private void markDown(Health.Builder builder, String reason) {
        builder.down()
                .withDetail("application", probeProperties.application())
                .withDetail("profile", probeProperties.profile())
                .withDetail("reason", reason);
    }
}