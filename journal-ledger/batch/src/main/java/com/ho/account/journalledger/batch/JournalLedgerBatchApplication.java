package com.ho.account.journalledger.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Import;
import com.ho.account.shared.infrastructure.SpringServiceDiscoveryRegistry;
import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;

@SpringBootApplication(scanBasePackages = {
        "com.ho.account.journalledger",
        "com.ho.account.common"
})
@EntityScan(basePackages = "com.ho.account.journalledger.domain")
@EnableJpaRepositories(basePackages = {
        "com.ho.account.journalledger.domain",
        "com.ho.account.journalledger.adapter.out.persistence"
})
@Import({SpringServiceDiscoveryRegistry.class, ProductionPostgresqlTlsGuard.class})
public class JournalLedgerBatchApplication {
    public static void main(String[] args) {
        SpringApplication.run(JournalLedgerBatchApplication.class, args);
    }
}
