package com.ho.account.loan;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 대출계좌 대량 처리 배치 서비스 (Loan Batch Service)
 */
@SpringBootApplication(scanBasePackages = {
        "com.ho.account.loan",
        "com.ho.account.journalledger",
        "com.ho.account.common",
        "com.ho.account.shared",
        "com.ho.account.masterdata.core"
})
@EnableBatchProcessing
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
public class LoanBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoanBatchApplication.class, args);
    }
}
