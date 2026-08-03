package com.ho.account.shared.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

class ProductionPostgresqlTlsGuardTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().setActiveProfiles("prod"))
            .withUserConfiguration(GuardConfiguration.class);

    @Test
    void acceptsSingleVerifyFullMode() {
        contextRunner
                .withPropertyValues(
                        "spring.datasource.url=jdbc:postgresql://db.invalid:5432/app?sslmode=verify-full")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ProductionPostgresqlTlsGuard.class);
                });
    }

    @Test
    void rejectsMissingOrInsecureOrAmbiguousSslModeAtStartup() {
        for (String url : new String[] {
                "jdbc:postgresql://db.invalid:5432/app",
                "jdbc:postgresql://db.invalid:5432/app?sslmode=require",
                "jdbc:postgresql://db.invalid:5432/app?sslmode=verify-full&sslmode=disable",
                "jdbc:h2:mem:wrong-production-driver"
        }) {
            contextRunner.withPropertyValues("spring.datasource.url=" + url)
                    .run(context -> assertThat(context)
                            .getFailure()
                            .rootCause()
                            .isInstanceOf(IllegalStateException.class)
                            .hasMessage("Production datasource must be PostgreSQL with sslmode=verify-full"));
        }
    }

    @Configuration(proxyBeanMethods = false)
    @Import(ProductionPostgresqlTlsGuard.class)
    static class GuardConfiguration {
    }
}
