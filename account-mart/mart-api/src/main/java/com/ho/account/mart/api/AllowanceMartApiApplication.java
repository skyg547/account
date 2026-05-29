package com.ho.account.mart.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [IFRS 9 Allowance Mart API Service]
 */
@SpringBootApplication(scanBasePackages = {"com.ho.account.mart", "com.ho.account.shared.finance"})
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.ho.account.mart.core.domain",
        "com.ho.account.mart.core.infrastructure.persistence.entity",
        "com.ho.account.shared.finance.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.ho.account.mart.core.infrastructure.persistence.jpa"
})
public class AllowanceMartApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AllowanceMartApiApplication.class, args);
    }
}
