package com.ho.account.journalledger.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.ho.account.journalledger",
        "com.ho.account.common",
        "com.ho.account.shared",
        "com.ho.account.masterdata.core"
})
@EntityScan(basePackages = {
        "com.ho.account.journalledger.domain",
        "com.ho.account.masterdata.core.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.ho.account.journalledger.domain",
        "com.ho.account.journalledger.adapter.out.persistence",
        "com.ho.account.masterdata.core.infrastructure.persistence"
})
public class JournalLedgerBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(JournalLedgerBatchApplication.class, args);
    }
}
