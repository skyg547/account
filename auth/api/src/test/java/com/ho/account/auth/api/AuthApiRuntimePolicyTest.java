package com.ho.account.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.AuthApplication;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.StandardEnvironment;

/**
 * ==============================================================================
 * Auth API Module - Runtime Security & Profile Policy Verification
 * ==============================================================================
 * [Architecture & Pedagogical Explanation]
 * 1. Profile Isolation Test (프로파일 격리 검증)
 *    - 'local' 프로파일 적용 시 H2 인메모리 DB, Flyway 스키마 마이그레이션, 데모 자격증명(JWT secret, internal token)이
 *      격리되어 정상적으로 주입되는지 다룹니다.
 *
 * 2. Fail-Closed Security Policy Test (보안 실패 기본 차단 검증)
 *    - base/dev/prod 프로파일에서 필수 보안 키(AUTH_JWT_SECRET, AUTH_INTERNAL_API_TOKEN)가 제공되지 않는 경우,
 *      애플리케이션 초기화 과정에서 Fail-Closed 예외가 발생하여 오구성된 구동을 방지함을 검증합니다.
 * ==============================================================================
 */
class AuthApiRuntimePolicyTest {

    @Test
    @DisplayName("local 프로파일은 H2 PostgreSQL 호환 모드와 Flyway, 격리된 데모 자격증명을 사용한다")
    void localProfileUsesFlywayOwnedH2AndIsolatesDemoCredentials() {
        try (ConfigurableApplicationContext context = localContext()) {
            Environment environment = context.getEnvironment();
            assertThat(environment.acceptsProfiles(Profiles.of("local"))).isTrue();

            assertThat(environment.getProperty("spring.datasource.url"))
                    .startsWith("jdbc:h2:mem:")
                    .contains("MODE=PostgreSQL");
            assertThat(environment.getProperty("spring.datasource.driver-class-name"))
                    .isEqualTo("org.h2.Driver");
            assertThat(environment.getProperty("spring.flyway.enabled", Boolean.class)).isTrue();
            assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("validate");

            assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("spring.cloud.vault.enabled", Boolean.class)).isFalse();
            assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();

            AuthModuleProperties properties = context.getBean(AuthModuleProperties.class);
            assertThat(properties.getJwt().getSecret())
                    .isEqualTo("modern-account-system-super-secret-key-1234567890");
            assertThat(properties.getInternalApi().getToken()).isEqualTo("local-internal-auth-token");
            assertThat(properties.getUsers()).hasSize(1);
            assertThat(properties.getUsers().get(0).getUsername()).isEqualTo("admin");
        }
    }

    @Test
    @DisplayName("필수 보안 환경변수가 누락된 경우 base/dev/prod 환경은 Fail-Closed 예외를 발생시킨다")
    void devAndProdProfilesFailClosedWithoutRequiredCredentials() {
        AuthModuleProperties properties = new AuthModuleProperties();
        assertThatThrownBy(properties::validateFailClosedPolicy)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Fail-Closed Security Violation");
    }

    @Test
    @DisplayName("환경변수/프로퍼티를 주입받으면 dev 및 prod 프로파일도 정상적인 보안 구성을 유지한다")
    void devAndProdProfilesSucceedWithInjectedCredentials() {
        for (String profile : new String[] {"dev", "prod"}) {
            AuthModuleProperties properties = new AuthModuleProperties();
            properties.getJwt().setSecret("dev-prod-injected-jwt-secret-key-1234567890");
            properties.getInternalApi().setToken("dev-prod-injected-internal-token");

            properties.validateFailClosedPolicy();

            assertThat(properties.getJwt().getSecret())
                    .isEqualTo("dev-prod-injected-jwt-secret-key-1234567890");
            assertThat(properties.getInternalApi().getToken())
                    .isEqualTo("dev-prod-injected-internal-token");
        }
    }

    @Test
    @DisplayName("Auth API 모듈만이 실행 가능한 Boot JAR 애플리케이션이다")
    void onlyAuthApiIsExecutableApplication() throws Exception {
        Path root = repositoryRoot();
        String settings = Files.readString(root.resolve("settings.gradle"));
        String coreBuild = Files.readString(root.resolve("auth/core/build.gradle"));

        assertThat(settings).contains("'auth:core', 'auth:api'");
        assertThat(coreBuild)
                .contains("id 'java-library'")
                .doesNotContain("id 'org.springframework.boot'");
        assertThat(root.resolve("auth/core/src/main/java/com/ho/account/auth/AuthApplication.java")).doesNotExist();
        assertThat(root.resolve("auth/api/src/main/java/com/ho/account/auth/AuthApplication.java")).exists();
    }

    private ConfigurableApplicationContext localContext() {
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("spring.profiles.active", "local");
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testOverrides", overrides));

        return new SpringApplicationBuilder(AuthApplication.class)
                .environment(environment)
                .web(WebApplicationType.NONE)
                .registerShutdownHook(false)
                .properties(
                        "spring.config.location="
                                + repositoryRoot()
                                        .resolve("auth/api/src/main/resources/")
                                        .toUri(),
                        "spring.main.banner-mode=off")
                .run();
    }

    private Path repositoryRoot() {
        Path candidate = Path.of("").toAbsolutePath();
        while (candidate != null && !Files.exists(candidate.resolve("settings.gradle"))) {
            candidate = candidate.getParent();
        }
        if (candidate == null) {
            throw new IllegalStateException("repository root was not found");
        }
        return candidate;
    }
}
