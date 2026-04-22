package com.ho.account.journalledger.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.ho.account")
@EntityScan(basePackages = "com.ho.account")
@EnableJpaRepositories(basePackages = "com.ho.account")
public class JournalLedgerBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(JournalLedgerBatchApplication.class, args);
    }
}
