package com.ho.account.masterdata;

import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Import;

/**
 * Master Data HTTP API application.
 *
 * <p>This class lives in {@code master-data:api} because a Spring Boot entry
 * point is an inbound adapter. The explicit root scan still discovers the
 * application services and persistence adapters supplied by
 * {@code master-data:core}, while the core jar remains independently
 * testable and cannot accidentally be deployed as a server.</p>
 *
 * <p>Flyway owns the complete PostgreSQL schema. API and Batch composition
 * roots validate the same mappings and never create production tables.</p>
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.masterdata")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ho.account.masterdata")
@EnableJpaRepositories(basePackages = "com.ho.account.masterdata.core.infrastructure.persistence")
@EntityScan(basePackages = {
        "com.ho.account.masterdata.core.domain.model",
        "com.ho.account.masterdata.core.domain.changerequest",
        // Pure domain aggregates intentionally carry no JPA annotations. The
        // persistence adapter therefore owns separate entity classes here.
        "com.ho.account.masterdata.core.infrastructure.persistence"
})
@Import(ProductionPostgresqlTlsGuard.class)
public class MasterDataApplication {

    /**
     * Starts only the HTTP executable. Batch jobs have their own process in
     * {@code master-data:batch}, so an API restart cannot interrupt a running
     * batch execution.
     */
    public static void main(String[] args) {
        SpringApplication.run(MasterDataApplication.class, args);
    }
}
