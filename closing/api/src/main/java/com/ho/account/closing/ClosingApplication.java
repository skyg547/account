package com.ho.account.closing;

import com.ho.account.masterdata.core.infrastructure.persistence.JpaFiscalPeriodPersistenceAdapter;
import com.ho.account.masterdata.core.infrastructure.persistence.JpaBusinessPartnerPersistenceAdapter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "com.ho.account.closing",
        "com.ho.account.journalledger.application.service",
        "com.ho.account.journalledger.infrastructure",
        "com.ho.account.common.adapter",
        "com.ho.account.masterdata.core.infrastructure.adapter"
})
@EntityScan(basePackages = {
        "com.ho.account.closing.domain",
        "com.ho.account.journalledger.domain",
        "com.ho.account.masterdata.core.domain",
        "com.ho.account.masterdata.core.infrastructure.persistence"
})
@EnableJpaRepositories(basePackages = {
        "com.ho.account.closing.infrastructure.persistence",
        "com.ho.account.journalledger.domain",
        "com.ho.account.journalledger.adapter.out.persistence",
        "com.ho.account.masterdata.core.infrastructure.persistence.repository"
})
@EnableDiscoveryClient
@Import({
        JpaFiscalPeriodPersistenceAdapter.class,
        JpaBusinessPartnerPersistenceAdapter.class
})
public class ClosingApplication {

    public static void main(String[] args) {
        SpringApplication.run(ClosingApplication.class, args);
    }
}
