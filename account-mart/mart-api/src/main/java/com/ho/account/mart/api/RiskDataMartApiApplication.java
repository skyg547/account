package com.ho.account.mart.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [Risk Data Mart API Service]
 */
@SpringBootApplication(scanBasePackages = "com.risk")
@EnableDiscoveryClient
@EntityScan(basePackages = "com.risk")
@EnableJpaRepositories(basePackages = "com.risk")
public class RiskDataMartApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(RiskDataMartApiApplication.class, args);
    }
}
