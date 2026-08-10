package com.ho.account.internalaudit.api;

import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Profiles;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Internal Audit HTTP API composition root.
 *
 * <p>The executable owns inbound HTTP wiring. Use cases and persistence
 * adapters remain in core and are assembled through explicit package scopes,
 * avoiding unrelated legacy governance entities.</p>
 */
@SpringBootApplication(scanBasePackages = {
        "com.ho.account.internalaudit.api",
        "com.ho.account.internalaudit.core.application",
        "com.ho.account.internalaudit.core.infrastructure.persistence"
})
@EntityScan(basePackages = "com.ho.account.internalaudit.core.infrastructure.persistence.entity")
@EnableJpaRepositories(
        basePackages = "com.ho.account.internalaudit.core.infrastructure.persistence.repository")
@Import(ProductionPostgresqlTlsGuard.class)
public class InternalAuditApiApplication {

    public static void main(String[] args) {
        application().run(args);
    }

    static SpringApplication application() {
        SpringApplication application = new SpringApplication(InternalAuditApiApplication.class);
        application.addInitializers(InternalAuditApiApplication::validateProductionTlsBeforeBeans);
        return application;
    }

    private static void validateProductionTlsBeforeBeans(ConfigurableApplicationContext context) {
        if (context.getEnvironment().acceptsProfiles(Profiles.of("prod"))) {
            ProductionPostgresqlTlsGuard.requireVerifyFull(
                    context.getEnvironment().getProperty("spring.datasource.url"));
        }
    }
}
