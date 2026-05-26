package com.risk.credit.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [Service] 신용 리스크 API 서비스 애플리케이션 진입점.
 */
@SpringBootApplication(scanBasePackages = {"com.risk.credit.api", "com.risk.credit.core", "com.risk.credit.batch", "com.risk.common"})
@EnableDiscoveryClient
@EntityScan(basePackages = {"com.risk.credit.core.domain", "com.risk.common.entity"})
@EnableJpaRepositories(basePackages = {"com.risk.credit.core.application.port.out", "com.risk.credit.batch"})
public class CreditRiskApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CreditRiskApiApplication.class, args);
    }
}
