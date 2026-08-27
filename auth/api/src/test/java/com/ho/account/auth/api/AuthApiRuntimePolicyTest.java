package com.ho.account.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.AuthApplication;
import com.ho.account.auth.core.infrastructure.config.AuthConfiguration;
import com.ho.account.auth.core.infrastructure.config.AuthModuleProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
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
 *    - 'local' 프로파일은 H2 인메모리 DB와 Flyway 스키마만 소유합니다.
 *    - JWT secret과 internal token은 파일 기본값이 아니라 실행자가 생성한 임시 입력으로만 주입합니다.
 *
 * 2. Fail-Closed Security Policy Test (보안 실패 기본 차단 검증)
 *    - base/dev/prod 프로파일에서 필수 보안 키(AUTH_JWT_SECRET, AUTH_INTERNAL_API_TOKEN)가 제공되지 않는 경우,
 *      애플리케이션 초기화 과정에서 Fail-Closed 예외가 발생하여 오구성된 구동을 방지함을 검증합니다.
 * ==============================================================================
 */
class AuthApiRuntimePolicyTest {

    @Test
    @DisplayName("local 프로파일은 H2/Flyway를 사용하고 보안 입력은 실행자가 임시 주입한다")
    void localProfileUsesFlywayOwnedH2AndEphemeralCredentials() throws Exception {
        String jwtSecret = ephemeralValue();
        String internalToken = ephemeralValue();

        try (ConfigurableApplicationContext context = localContext(jwtSecret, internalToken)) {
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
            assertThat(properties.getJwt().getSecret()).isEqualTo(jwtSecret);
            assertThat(properties.getInternalApi().getToken()).isEqualTo(internalToken);
            assertThat(properties.getUsers()).isEmpty();
        }

        Path resources = repositoryRoot().resolve("auth/api/src/main/resources");
        String localProfile = Files.readString(resources.resolve("application-local.yml"));
        String baseProfile = Files.readString(resources.resolve("application.yml"));

        assertThat(localProfile)
                .doesNotContain("AUTH_JWT_SECRET")
                .doesNotContain("AUTH_INTERNAL_API_TOKEN")
                .doesNotContain("jwt:")
                .doesNotContain("internal-api:");
        assertThat(baseProfile)
                .contains("${AUTH_JWT_SECRET:}")
                .contains("${AUTH_INTERNAL_API_TOKEN:}");
    }

    @Test
    @DisplayName("필수 보안 환경변수가 누락되거나 공백인 경우 base/dev/prod 환경은 Fail-Closed 예외를 발생시킨다")
    void devAndProdProfilesFailClosedWithoutRequiredCredentials() {
        for (String profile : new String[] {null, "dev", "prod"}) {
            String jwtSecret = ephemeralValue();
            String internalToken = ephemeralValue();

            for (String blankSecret : new String[] {"", "   ", "\t\n  "}) {
                assertThatThrownBy(() -> securityPolicyContext(profile, blankSecret, internalToken))
                        .hasRootCauseInstanceOf(IllegalStateException.class)
                        .hasRootCauseMessage(
                                "Fail-Closed Security Violation: 'auth.jwt.secret' must be provided via AUTH_JWT_SECRET environment variable.");
            }
            for (String blankToken : new String[] {"", "   ", "\t\n  "}) {
                assertThatThrownBy(() -> securityPolicyContext(profile, jwtSecret, blankToken))
                        .hasRootCauseInstanceOf(IllegalStateException.class)
                        .hasRootCauseMessage(
                                "Fail-Closed Security Violation: 'auth.internal-api.token' must be provided via AUTH_INTERNAL_API_TOKEN environment variable.");
            }
        }
    }

    @Test
    @DisplayName("JWT signing key가 32바이트 미만(31바이트 이하)인 경우 모든 프로파일에서 Fail-Closed 예외가 발생한다")
    void jwtSecretFailsBelow32BytesAcrossAllProfiles() {
        String exact31Bytes = "1234567890123456789012345678901"; // 31 ASCII bytes
        assertThat(exact31Bytes.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(31);

        String multiByte31Bytes = "가나다라마바사아자차x"; // 10 Korean chars (30 bytes) + 1 ASCII (1 byte) = 31 bytes (11 chars)
        assertThat(multiByte31Bytes.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(31);

        String internalToken = ephemeralValue();

        for (String profile : new String[] {null, "local", "dev", "prod"}) {
            for (String shortSecret : new String[] {exact31Bytes, multiByte31Bytes}) {
                assertThatThrownBy(() -> securityPolicyContext(profile, shortSecret, internalToken))
                        .hasRootCauseInstanceOf(IllegalStateException.class)
                        .hasRootCauseMessage(
                                "Fail-Closed Security Violation: 'auth.jwt.secret' must be at least 32 UTF-8 bytes for secure HS256 signing.");
            }
        }
    }

    @Test
    @DisplayName("JWT signing key가 정확히 32바이트 이상인 경우 모든 프로파일에서 정상 기동한다")
    void jwtSecretSucceedsAt32BytesOrAboveAcrossAllProfiles() {
        String exact32Bytes = "12345678901234567890123456789012"; // exactly 32 ASCII bytes
        assertThat(exact32Bytes.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(32);

        String multiByte33Bytes = "가나다라마바사아자차카"; // 11 Korean chars = 33 UTF-8 bytes (11 chars)
        assertThat(multiByte33Bytes.getBytes(java.nio.charset.StandardCharsets.UTF_8)).hasSize(33);

        String internalToken = ephemeralValue();

        for (String profile : new String[] {null, "local", "dev", "prod"}) {
            for (String validSecret : new String[] {exact32Bytes, multiByte33Bytes}) {
                try (ConfigurableApplicationContext context =
                        securityPolicyContext(profile, validSecret, internalToken)) {
                    AuthModuleProperties properties = context.getBean(AuthModuleProperties.class);
                    assertThat(properties.getJwt().getSecret()).isEqualTo(validSecret);
                }
            }
        }
    }

    @Test
    @DisplayName("환경변수/프로퍼티를 주입받으면 dev 및 prod 프로파일도 정상적인 보안 구성을 유지한다")
    void devAndProdProfilesSucceedWithInjectedCredentials() {
        for (String profile : new String[] {"dev", "prod"}) {
            String jwtSecret = ephemeralValue();
            String internalToken = ephemeralValue();
            try (ConfigurableApplicationContext context =
                    securityPolicyContext(profile, jwtSecret, internalToken)) {
                assertThat(context.getEnvironment().acceptsProfiles(Profiles.of(profile))).isTrue();
                AuthModuleProperties properties = context.getBean(AuthModuleProperties.class);
                assertThat(properties.getJwt().getSecret()).isEqualTo(jwtSecret);
                assertThat(properties.getInternalApi().getToken()).isEqualTo(internalToken);
            }
        }
    }

    @Test
    @DisplayName("Configured user 비밀번호가 {bcrypt} 형식인 경우 정상 기동한다")
    void configuredUserPasswordSucceedsWithBcryptPrefix() {
        String jwtSecret = ephemeralValue();
        String internalToken = ephemeralValue();
        String dynamicHash = "{bcrypt}" + new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode(ephemeralValue());

        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("spring.profiles.active", "local");
        overrides.put("auth.jwt.secret", jwtSecret);
        overrides.put("auth.internal-api.token", internalToken);
        overrides.put("auth.users[0].username", "testuser");
        overrides.put("auth.users[0].password", dynamicHash);

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("userOverrides", overrides));

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AuthConfiguration.class)
                .environment(environment)
                .web(WebApplicationType.NONE)
                .registerShutdownHook(false)
                .properties(
                        "spring.config.location="
                                + repositoryRoot().resolve("auth/api/src/main/resources/").toUri(),
                        "spring.main.banner-mode=off")
                .run()) {
            AuthModuleProperties properties = context.getBean(AuthModuleProperties.class);
            assertThat(properties.getUsers()).hasSize(1);
            assertThat(properties.getUsers().get(0).getPassword()).isEqualTo(dynamicHash);
        }
    }

    @Test
    @DisplayName("Configured user 비밀번호가 raw 평문, {noop}, {unknown}, 빈 prefix, 빈 payload인 경우 Fail-Closed 예외가 발생한다")
    void configuredUserPasswordFailsClosedOnInvalidPrefixOrPayload() {
        String jwtSecret = ephemeralValue();
        String internalToken = ephemeralValue();

        String[] invalidPasswords = new String[] {
                "raw_unencoded_plaintext_password",
                "{noop}plaintext_not_allowed",
                "{unknown}unsupported_algorithm",
                "{}",
                "{ }",
                "{bcrypt}",
                "{bcrypt}   "
        };

        for (String invalidPassword : invalidPasswords) {
            Map<String, Object> overrides = new LinkedHashMap<>();
            overrides.put("spring.profiles.active", "local");
            overrides.put("auth.jwt.secret", jwtSecret);
            overrides.put("auth.internal-api.token", internalToken);
            overrides.put("auth.users[0].username", "testuser");
            overrides.put("auth.users[0].password", invalidPassword);

            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(new MapPropertySource("invalidUserOverrides", overrides));

            assertThatThrownBy(() -> new SpringApplicationBuilder(AuthConfiguration.class)
                    .environment(environment)
                    .web(WebApplicationType.NONE)
                    .registerShutdownHook(false)
                    .properties(
                            "spring.config.location="
                                    + repositoryRoot().resolve("auth/api/src/main/resources/").toUri(),
                            "spring.main.banner-mode=off")
                    .run())
                    .hasRootCauseInstanceOf(IllegalStateException.class);
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

    private ConfigurableApplicationContext localContext(String jwtSecret, String internalToken) {
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("spring.profiles.active", "local");
        overrides.put("auth.jwt.secret", jwtSecret);
        overrides.put("auth.internal-api.token", internalToken);
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

    private ConfigurableApplicationContext securityPolicyContext(
            String profile, String jwtSecret, String internalToken) {
        Map<String, Object> overrides = new LinkedHashMap<>();
        if (profile != null) {
            overrides.put("spring.profiles.active", profile);
        }
        overrides.put("auth.jwt.secret", jwtSecret);
        overrides.put("auth.internal-api.token", internalToken);
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("credentialIsolation", overrides));

        return new SpringApplicationBuilder(AuthConfiguration.class)
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

    private static String ephemeralValue() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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
