package com.ho.account.auth.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.auth.AuthApplication;
import com.ho.account.auth.core.domain.model.AuthUser;
import com.ho.account.auth.core.application.port.out.AuthUserQueryPort;
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
 *
 * 3. Configured User Password Encoding Contract Test (설정 사용자 비밀번호 인코딩 계약 검증)
 *    - auth 모듈의 설정 사용자는 반드시 유효한 위임 비밀번호 접두사({id}, 예: {bcrypt})를 가져야 합니다.
 *    - 접두사가 없는 raw 비밀번호, {noop} 평문 접두사, 빈 접두사({}) 또는 malformed 접두사는 구동 시 차단됩니다.
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
    @DisplayName("필수 보안 환경변수가 누락된 경우 base/dev/prod 환경은 Fail-Closed 예외를 발생시킨다")
    void devAndProdProfilesFailClosedWithoutRequiredCredentials() {
        for (String profile : new String[] {null, "dev", "prod"}) {
            String jwtSecret = ephemeralValue();
            String internalToken = ephemeralValue();

            assertThatThrownBy(() -> securityPolicyContext(profile, "", internalToken))
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage(
                            "Fail-Closed Security Violation: 'auth.jwt.secret' must be provided via AUTH_JWT_SECRET environment variable.");
            assertThatThrownBy(() -> securityPolicyContext(profile, jwtSecret, ""))
                    .hasRootCauseInstanceOf(IllegalStateException.class)
                    .hasRootCauseMessage(
                            "Fail-Closed Security Violation: 'auth.internal-api.token' must be provided via AUTH_INTERNAL_API_TOKEN environment variable.");
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
    @DisplayName("유효한 {bcrypt} 인코딩 패스워드를 가진 설정 사용자는 정상 구동되고 DB에 적재된다")
    void configuredUserWithValidBcryptPasswordIsAcceptedAndPersisted() {
        String jwtSecret = ephemeralValue();
        String internalToken = ephemeralValue();
        String bcryptPassword = "{bcrypt}$2a$10$dXJ3SW6G7P50lGmMkkmwe.20cQQubK3.HZWzG3YB1tlRy.fqvM/BG";

        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("spring.profiles.active", "local");
        overrides.put("auth.jwt.secret", jwtSecret);
        overrides.put("auth.internal-api.token", internalToken);
        overrides.put("auth.users[0].username", "seed_admin");
        overrides.put("auth.users[0].password", bcryptPassword);
        overrides.put("auth.users[0].department-code", "FIN");
        overrides.put("auth.users[0].roles[0]", "ROLE_ADMIN");

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testOverrides", overrides));

        try (ConfigurableApplicationContext context = new SpringApplicationBuilder(AuthApplication.class)
                .environment(environment)
                .web(WebApplicationType.NONE)
                .registerShutdownHook(false)
                .properties(
                        "spring.config.location="
                                + repositoryRoot()
                                        .resolve("auth/api/src/main/resources/")
                                        .toUri(),
                        "spring.main.banner-mode=off")
                .run()) {

            AuthModuleProperties properties = context.getBean(AuthModuleProperties.class);
            assertThat(properties.getUsers()).hasSize(1);
            assertThat(properties.getUsers().get(0).getUsername()).isEqualTo("seed_admin");
            assertThat(properties.getUsers().get(0).getPassword()).isEqualTo(bcryptPassword);

            AuthUserQueryPort userQueryPort = context.getBean(AuthUserQueryPort.class);
            AuthUser user = userQueryPort.findByUsername("seed_admin").orElseThrow();
            assertThat(user.getUsername()).isEqualTo("seed_admin");
            assertThat(user.getStoredPassword()).isEqualTo(bcryptPassword);
        }
    }

    @Test
    @DisplayName("접두사가 없는 raw 비밀번호를 가진 설정 사용자는 Fail-Closed 예외로 구동이 차단된다")
    void configuredUserWithRawPasswordFailsClosed() {
        String jwtSecret = ephemeralValue();
        String internalToken = ephemeralValue();

        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("auth.jwt.secret", jwtSecret);
        overrides.put("auth.internal-api.token", internalToken);
        overrides.put("auth.users[0].username", "raw_user");
        overrides.put("auth.users[0].password", "plainPassword123");

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testOverrides", overrides));

        assertThatThrownBy(() -> new SpringApplicationBuilder(AuthConfiguration.class)
                .environment(environment)
                .web(WebApplicationType.NONE)
                .registerShutdownHook(false)
                .properties(
                        "spring.config.location="
                                + repositoryRoot()
                                        .resolve("auth/api/src/main/resources/")
                                        .toUri(),
                        "spring.main.banner-mode=off")
                .run())
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage(
                        "Fail-Closed Security Violation: Password for configured user 'raw_user' must have a delegated encoding prefix (e.g., '{bcrypt}'). Raw passwords are not allowed.");
    }

    @Test
    @DisplayName("{noop} 평문 비밀번호를 가진 설정 사용자는 Fail-Closed 예외로 구동이 차단된다")
    void configuredUserWithNoopPasswordFailsClosed() {
        String jwtSecret = ephemeralValue();
        String internalToken = ephemeralValue();

        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("auth.jwt.secret", jwtSecret);
        overrides.put("auth.internal-api.token", internalToken);
        overrides.put("auth.users[0].username", "noop_user");
        overrides.put("auth.users[0].password", "{noop}plainPassword123");

        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("testOverrides", overrides));

        assertThatThrownBy(() -> new SpringApplicationBuilder(AuthConfiguration.class)
                .environment(environment)
                .web(WebApplicationType.NONE)
                .registerShutdownHook(false)
                .properties(
                        "spring.config.location="
                                + repositoryRoot()
                                        .resolve("auth/api/src/main/resources/")
                                        .toUri(),
                        "spring.main.banner-mode=off")
                .run())
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage(
                        "Fail-Closed Security Violation: Password for configured user 'noop_user' uses forbidden '{noop}' prefix. Plaintext passwords are not allowed in configuration.");
    }

    @Test
    @DisplayName("빈 접두사 또는 잘못된 형식의 접두사를 가진 설정 사용자는 Fail-Closed 예외로 구동이 차단된다")
    void configuredUserWithEmptyOrMalformedPrefixFailsClosed() {
        String jwtSecret = ephemeralValue();
        String internalToken = ephemeralValue();

        for (String malformedPassword : new String[] {"{}secret", "{ }secret", "{bcrypt", "{}"}) {
            Map<String, Object> overrides = new LinkedHashMap<>();
            overrides.put("auth.jwt.secret", jwtSecret);
            overrides.put("auth.internal-api.token", internalToken);
            overrides.put("auth.users[0].username", "malformed_user");
            overrides.put("auth.users[0].password", malformedPassword);

            StandardEnvironment environment = new StandardEnvironment();
            environment.getPropertySources().addFirst(new MapPropertySource("testOverrides", overrides));

            assertThatThrownBy(() -> new SpringApplicationBuilder(AuthConfiguration.class)
                    .environment(environment)
                    .web(WebApplicationType.NONE)
                    .registerShutdownHook(false)
                    .properties(
                            "spring.config.location="
                                    + repositoryRoot()
                                            .resolve("auth/api/src/main/resources/")
                                            .toUri(),
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
