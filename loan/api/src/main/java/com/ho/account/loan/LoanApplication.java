package com.ho.account.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/** Loan-owned persistence; non-dev provider composition lives in LoanMonolithConfiguration. */
@SpringBootApplication(scanBasePackages = "com.ho.account.loan")
@EntityScan(basePackages = "com.ho.account.loan.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.loan.infrastructure.persistence")
@EnableDiscoveryClient
public class LoanApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoanApplication.class, args);
    }
}
