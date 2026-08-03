package com.ho.account.journalledger;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import com.ho.account.shared.infrastructure.SpringServiceDiscoveryRegistry;
import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;

/**
 * Journal Ledger 마이크로서비스 애플리케이션 메인 클래스
 */
@SpringBootApplication(scanBasePackages = {
        "com.ho.account.journalledger",
        "com.ho.account.common"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ho.account.journalledger")
@EnableJpaRepositories(basePackages = {
        "com.ho.account.journalledger.domain",
        "com.ho.account.journalledger.adapter.out.persistence"
})
@EntityScan(basePackages = "com.ho.account.journalledger.domain")
@Import({SpringServiceDiscoveryRegistry.class, ProductionPostgresqlTlsGuard.class})
public class JournalLedgerApplication {

    public static void main(String[] args) {
        SpringApplication.run(JournalLedgerApplication.class, args);
    }

}
