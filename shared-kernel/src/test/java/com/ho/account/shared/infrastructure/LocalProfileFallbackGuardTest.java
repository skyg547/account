package com.ho.account.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.mock.env.MockEnvironment;

/**
 * local 프로파일로 운영 환경에 기동하는 것을 막는 가드를 검증한다.
 */
class LocalProfileFallbackGuardTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LocalProfileFallbackGuard.class));

    @Test
    void startsWhenNoProductionMarkerIsPresent() {
        assertThatCode(() -> LocalProfileFallbackGuard.requireNoProductionMarkers(new MockEnvironment()))
                .doesNotThrowAnyException();
    }

    @Test
    void refusesWhenProductionDatabaseUrlIsPresent() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("PROD_DB_URL", "jdbc:postgresql://db.internal:5432/ledger");

        assertThatThrownBy(() -> LocalProfileFallbackGuard.requireNoProductionMarkers(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROD_DB_URL")
                .hasMessageContaining("SPRING_PROFILES_ACTIVE");
    }

    @Test
    void refusesWhenProductionKafkaOrRedisIsPresent() {
        assertThatThrownBy(() -> LocalProfileFallbackGuard.requireNoProductionMarkers(
                new MockEnvironment().withProperty("PROD_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROD_KAFKA_BOOTSTRAP_SERVERS");

        assertThatThrownBy(() -> LocalProfileFallbackGuard.requireNoProductionMarkers(
                new MockEnvironment().withProperty("PROD_REDIS_HOST", "redis.internal")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROD_REDIS_HOST");
    }

    @Test
    void reportsEveryMarkerItFound() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("PROD_DB_URL", "jdbc:postgresql://db.internal:5432/ledger")
                .withProperty("PROD_REDIS_HOST", "redis.internal");

        assertThatThrownBy(() -> LocalProfileFallbackGuard.requireNoProductionMarkers(environment))
                .hasMessageContaining("PROD_DB_URL")
                .hasMessageContaining("PROD_REDIS_HOST");
    }

    @Test
    void treatsBlankMarkerAsAbsent() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("PROD_DB_URL", "   ");

        assertThatCode(() -> LocalProfileFallbackGuard.requireNoProductionMarkers(environment))
                .doesNotThrowAnyException();
    }

    @Test
    void registersOnlyUnderTheLocalProfile() {
        runner.withPropertyValues("spring.profiles.active=prod")
                .run(context -> assertThat(context).doesNotHaveBean(LocalProfileFallbackGuard.class));

        runner.withPropertyValues("spring.profiles.active=local")
                .run(context -> assertThat(context).hasSingleBean(LocalProfileFallbackGuard.class));
    }

    /**
     * 프로파일을 지정하지 않아 {@code spring.profiles.default}로 떨어지는 경우가 이 가드가
     * 노리는 실제 사고 경로다. 그 상태에서도 빈이 등록되어야 검사가 동작한다.
     */
    @Test
    void registersWhenLocalComesFromTheDefaultProfile() {
        runner.withPropertyValues("spring.profiles.default=local")
                .run(context -> assertThat(context).hasSingleBean(LocalProfileFallbackGuard.class));
    }

    @Test
    void failsStartupWhenLocalProfileMeetsProductionMarker() {
        runner.withPropertyValues(
                        "spring.profiles.default=local",
                        "PROD_DB_URL=jdbc:postgresql://db.internal:5432/ledger")
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .rootCause()
                        .isInstanceOf(IllegalStateException.class)
                        .hasMessageContaining("PROD_DB_URL"));
    }

    @Test
    void startsUnderProductionMarkersOnANonLocalProfile() {
        runner.withPropertyValues(
                        "spring.profiles.active=prod",
                        "PROD_DB_URL=jdbc:postgresql://db.internal:5432/ledger")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
