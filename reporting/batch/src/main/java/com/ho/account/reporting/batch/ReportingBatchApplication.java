package com.ho.account.reporting.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.ho.account.reporting")
@EntityScan(basePackages = "com.ho.account.reporting.infrastructure.persistence")
@EnableJpaRepositories(basePackages = "com.ho.account.reporting.infrastructure.persistence")
@EnableBatchProcessing
public class ReportingBatchApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(ReportingBatchApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
