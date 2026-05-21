package com.ho.account.closing.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
    "com.ho.account.closing.batch",
    "com.ho.account.closing.application.service", // Closing properties
    "com.ho.account.journalledger.application.port", // Journal UseCase 
    "com.ho.account.masterdata.core.infrastructure.persistence" // Exchange rates
})
@EntityScan(basePackages = {
    "com.ho.account.closing.domain",
    "com.ho.account.journalledger.domain.ledger.domain",
    "com.ho.account.journalledger.domain.journal.domain",
    "com.ho.account.masterdata.core.domain.model"
})
@EnableJpaRepositories(basePackages = {
    "com.ho.account.closing.infrastructure.persistence",
    "com.ho.account.journalledger.domain.ledger.repository",
    "com.ho.account.masterdata.core.infrastructure.persistence.repository"
})
public class ClosingBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(ClosingBatchApplication.class, args);
    }
}
