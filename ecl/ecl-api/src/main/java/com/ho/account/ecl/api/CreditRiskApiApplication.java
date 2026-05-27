package com.ho.account.ecl.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [Service] 대손충당금(IFRS9) API 서비스 애플리케이션 진입점.
 */
@SpringBootApplication(scanBasePackages = {"com.ho.account.ecl.api", "com.ho.account.ecl.core", "com.ho.account.ecl.batch", "com.ho.account.shared.finance"})
@EnableDiscoveryClient
@EntityScan(basePackages = {"com.ho.account.ecl.core.domain", "com.ho.account.shared.finance.entity"})
@EnableJpaRepositories(basePackages = {"com.ho.account.ecl.core.application.port.out", "com.ho.account.ecl.batch"})
public class CreditRiskApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CreditRiskApiApplication.class, args);
    }
}
