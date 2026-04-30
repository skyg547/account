package com.ho.account.risk.batch;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableBatchProcessing
@ComponentScan(basePackages = "com.ho.account")
@EnableJpaRepositories(basePackages = "com.ho.account")
@EntityScan(basePackages = "com.ho.account")
public class RiskBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiskBatchApplication.class, args);
    }
}
