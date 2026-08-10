package com.ho.account.internalaudit.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class InternalAuditProductionTlsGuardTest {

    @Test
    void productionRejectsWeakTlsBeforeDatasourceOrNetworkInitialization(CapturedOutput output) {
        SpringApplication application = InternalAuditApiApplication.application();
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setRegisterShutdownHook(false);

        assertThatThrownBy(() -> application.run(
                        "--spring.profiles.active=prod",
                        "--spring.datasource.url=jdbc:postgresql://should-never-resolve.invalid:5432/internal_audit?sslmode=require",
                        "--spring.datasource.username=test_internal_audit_app",
                        "--spring.datasource.password=not-a-real-secret",
                        "--spring.cloud.config.enabled=false",
                        "--spring.cloud.config.import-check.enabled=false",
                        "--spring.cloud.discovery.enabled=false",
                        "--eureka.client.enabled=false"))
                .hasStackTraceContaining(
                        "Production datasource must be PostgreSQL with sslmode=verify-full");

        assertThat(output)
                .doesNotContain("HikariPool")
                .doesNotContain("UnknownHostException")
                .doesNotContain("should-never-resolve.invalid");
    }
}
