package com.ho.account.budget.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

/**
 * shared-kernel의 LocalProfileFallbackGuard와 동일한 규칙을 Budget에서도 강제하는지 검증한다.
 * Budget은 shared-kernel에 의존하지 않아 가드를 복제해 두었으므로, 두 구현이 어긋나지
 * 않도록 같은 시나리오를 여기서도 확인한다.
 */
class BudgetLocalProfileFallbackGuardTest {

    @Test
    void startsWhenNoProductionMarkerIsPresent() {
        assertThatCode(() -> BudgetLocalProfileFallbackGuard.requireNoProductionMarkers(new MockEnvironment()))
                .doesNotThrowAnyException();
    }

    @Test
    void refusesWhenProductionDatabaseUrlIsPresent() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("PROD_DB_URL", "jdbc:postgresql://db.internal:5432/budget");

        assertThatThrownBy(() -> BudgetLocalProfileFallbackGuard.requireNoProductionMarkers(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PROD_DB_URL")
                .hasMessageContaining("SPRING_PROFILES_ACTIVE");
    }

    @Test
    void refusesWhenProductionKafkaOrRedisIsPresent() {
        assertThatThrownBy(() -> BudgetLocalProfileFallbackGuard.requireNoProductionMarkers(
                new MockEnvironment().withProperty("PROD_KAFKA_BOOTSTRAP_SERVERS", "kafka:9092")))
                .isInstanceOf(IllegalStateException.class);

        assertThatThrownBy(() -> BudgetLocalProfileFallbackGuard.requireNoProductionMarkers(
                new MockEnvironment().withProperty("PROD_REDIS_HOST", "redis.internal")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void treatsBlankMarkerAsAbsent() {
        assertThatCode(() -> BudgetLocalProfileFallbackGuard.requireNoProductionMarkers(
                new MockEnvironment().withProperty("PROD_DB_URL", "   ")))
                .doesNotThrowAnyException();
    }
}
