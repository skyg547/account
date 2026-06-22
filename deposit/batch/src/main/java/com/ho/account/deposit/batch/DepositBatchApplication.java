package com.ho.account.deposit.batch;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.ho.account.deposit")
@EntityScan(basePackages = "com.ho.account.deposit.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.deposit.infrastructure.adapter.out.persistence")
public class DepositBatchApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(DepositBatchApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
