package com.ho.account.loan;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 대출 관리 마이크로서비스 (Loan Microservice)
 */
@SpringBootApplication(scanBasePackages = {
        "com.ho.account.loan",
        "com.ho.account.journalledger",
        "com.ho.account.common",
        "com.ho.account.shared",
        "com.ho.account.masterdata.core"
})
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.ho.account.loan.domain",
        "com.ho.account.journalledger.domain",
        "com.ho.account.masterdata.core.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.ho.account.loan.infrastructure.persistence",
        "com.ho.account.journalledger.domain",
        "com.ho.account.journalledger.adapter.out.persistence",
        "com.ho.account.masterdata.core.infrastructure.persistence"
})
public class LoanApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanApplication.class, args);
    }
}
