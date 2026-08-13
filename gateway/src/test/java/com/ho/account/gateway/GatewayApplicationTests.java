package com.ho.account.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ho.account.gateway.config.JwtProperties;
import com.ho.account.gateway.security.TokenVersionValidationProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.Profiles;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
@ActiveProfiles("local")
class GatewayApplicationTests {

    private static final String EPHEMERAL_SECRET = ephemeralValue();

    @Autowired
    private Environment environment;

    @Autowired
    private JwtProperties jwtProperties;

    @Autowired
    private TokenVersionValidationProperties tokenVersionProperties;

    @DynamicPropertySource
    static void runtimeInputs(DynamicPropertyRegistry registry) {
        registry.add("auth.jwt.secret", () -> EPHEMERAL_SECRET);
    }

    @Test
    @DisplayName("local 프로파일은 외부 control-plane 없이 실행자 제공 JWT 입력으로 기동한다")
    void contextLoads() {
        assertThat(environment.acceptsProfiles(Profiles.of("local"))).isTrue();
        assertThat(environment.getProperty("spring.cloud.config.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.discovery.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("spring.cloud.loadbalancer.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty(
                        "spring.cloud.gateway.discovery.locator.enabled", Boolean.class))
                .isFalse();
        assertThat(environment.getProperty("eureka.client.enabled", Boolean.class)).isFalse();
        assertThat(environment.getProperty("management.tracing.enabled", Boolean.class)).isFalse();
        assertThat(tokenVersionProperties.isEnabled()).isFalse();
        assertThat(jwtProperties.getSecret()).isEqualTo(EPHEMERAL_SECRET);
    }

    @Test
    @DisplayName("local 프로파일은 검증 키가 없으면 실제 Spring 컨텍스트에서 fail-closed 한다")
    void localProfileFailsClosedWithoutVerificationKey() {
        assertThatThrownBy(() -> {
                    try (ConfigurableApplicationContext ignored = missingKeyContext()) {
                        // A successful context is closed before AssertJ reports the missing exception.
                    }
                })
                .hasRootCauseInstanceOf(IllegalStateException.class)
                .hasRootCauseMessage(
                        "JWT verification requires either a valid secret (auth.jwt.secret) or public key (auth.jwt.public-key) / JWKS URI");
    }

    @Test
    @DisplayName("tracked local resource에는 JWT 검증 키나 원격 검증 주소 기본값이 없다")
    void localResourceContainsOnlyStandalonePolicy() throws Exception {
        Path resources = repositoryRoot().resolve("gateway/src/main/resources");
        String localProfile = Files.readString(resources.resolve("application-local.yml"));
        String baseProfile = Files.readString(resources.resolve("application.yml"));

        assertThat(localProfile)
                .doesNotContain("AUTH_JWT_SECRET")
                .doesNotContain("JWT_SECRET")
                .doesNotContain("jwt:")
                .doesNotContain("secret:")
                .doesNotContain("public-key")
                .doesNotContain("jwks-uri")
                .doesNotContain("base-url");
        assertThat(baseProfile)
                .contains("${AUTH_JWT_SECRET:${JWT_SECRET:}}")
                .contains("${AUTH_JWT_PUBLIC_KEY:${JWT_PUBLIC_KEY:}}")
                .contains("${AUTH_JWT_JWKS_URI:${JWT_JWKS_URI:}}");
    }

    private ConfigurableApplicationContext missingKeyContext() {
        Map<String, Object> overrides = new LinkedHashMap<>();
        overrides.put("spring.profiles.active", "local");
        overrides.put("server.port", "0");
        overrides.put("auth.jwt.secret", "");
        overrides.put("auth.jwt.public-key", "");
        overrides.put("auth.jwt.jwks-uri", "");
        StandardEnvironment isolatedEnvironment = new StandardEnvironment();
        isolatedEnvironment
                .getPropertySources()
                .addFirst(new MapPropertySource("missingVerificationKey", overrides));

        return new SpringApplicationBuilder(GatewayApplication.class)
                .environment(isolatedEnvironment)
                .web(WebApplicationType.REACTIVE)
                .registerShutdownHook(false)
                .properties(
                        "spring.config.location="
                                + repositoryRoot()
                                        .resolve("gateway/src/main/resources/")
                                        .toUri(),
                        "spring.main.banner-mode=off")
                .run();
    }

    private static String ephemeralValue() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getEncoder().encodeToString(bytes);
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
