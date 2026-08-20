package com.ho.account.configserver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.web.context.support.StandardServletEnvironment;

class EncryptionKeyStartupGuardTest {

    @Test
    void failsBeforeContextCreationWhenExternalKeyIsMissing() {
        assertThatThrownBy(() -> startApplication(null))
                .hasMessageContaining(EncryptionKeyStartupGuard.REQUIRED_INPUT_MESSAGE);
    }

    @ParameterizedTest
    @MethodSource("invalidExternalKeys")
    void rejectsBlankAndWhitespaceExternalKeys(String externalKey) {
        assertThatThrownBy(() -> startApplication(externalKey))
                .hasMessageContaining(EncryptionKeyStartupGuard.REQUIRED_INPUT_MESSAGE);
    }

    @Test
    void startsWithProcessGeneratedNonBlankExternalKey() {
        String ephemeralKey = UUID.randomUUID().toString();

        try (ConfigurableApplicationContext context = startApplication(ephemeralKey)) {
            assertThat(context).isInstanceOf(ServletWebServerApplicationContext.class);
            assertThat(context.isActive()).isTrue();
        }
    }

    private ConfigurableApplicationContext startApplication(String externalKey) {
        SpringApplication application = ConfigServerApplication.application();
        StandardServletEnvironment environment = isolatedEnvironment(externalKey);
        application.setEnvironment(environment);

        return application.run(
                "--server.port=0",
                "--spring.profiles.active=native",
                "--spring.cloud.config.server.native.search-locations=classpath:/config-repository",
                "--spring.cloud.config.server.health.repositories.master-data.name=policy-test-service",
                "--spring.cloud.config.server.health.repositories.master-data.profiles=default",
                "--config-server.repository-probe.application=policy-test-service",
                "--config-server.repository-probe.profile=default");
    }

    private StandardServletEnvironment isolatedEnvironment(String externalKey) {
        StandardServletEnvironment environment = new StandardServletEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        if (externalKey != null) {
            environment.getPropertySources().addFirst(new MapPropertySource(
                    "ephemeral-test-encryption-key",
                    Map.of("ENCRYPT_KEY", externalKey)));
        }
        return environment;
    }

    private static Stream<Arguments> invalidExternalKeys() {
        return Stream.of(
                Arguments.of(""),
                Arguments.of(" "),
                Arguments.of("\t"),
                Arguments.of(" leading"),
                Arguments.of("trailing "));
    }
}
