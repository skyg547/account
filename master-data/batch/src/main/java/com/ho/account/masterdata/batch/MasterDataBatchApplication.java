package com.ho.account.masterdata.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Dedicated Spring Batch executable for Master Data maintenance jobs.
 *
 * <p>The batch module depends on core, but core never depends on batch. This
 * direction matters because domain rules and aggregation logic must be usable
 * from tests or another adapter without loading Spring Batch infrastructure.
 * The explicit scan roots assemble core services, entities, and repository
 * adapters into this executable without putting API controllers on the batch
 * classpath.</p>
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.masterdata")
@EnableDiscoveryClient
@EnableJpaRepositories(basePackages = "com.ho.account.masterdata.core.infrastructure.persistence")
@EntityScan(basePackages = {
        "com.ho.account.masterdata.core.domain.model",
        "com.ho.account.masterdata.core.domain.changerequest",
        // BusinessPartner is persistence-ignorant; its JPA entity belongs to
        // the outbound adapter and must be registered by this composition root.
        "com.ho.account.masterdata.core.infrastructure.persistence"
})
public class MasterDataBatchApplication {

    /**
     * Starts the job process independently from the HTTP service. Operators
     * can therefore restart a failed job without recycling API connections.
     */
    public static void main(String[] args) {
        SpringApplication.run(MasterDataBatchApplication.class, args);
    }
}
