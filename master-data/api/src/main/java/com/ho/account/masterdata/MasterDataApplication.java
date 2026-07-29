package com.ho.account.masterdata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Master Data HTTP API application.
 *
 * <p>This class lives in {@code master-data:api} because a Spring Boot entry
 * point is an inbound adapter. The explicit root scan still discovers the
 * application services and persistence adapters supplied by
 * {@code master-data:core}, while the core jar remains independently
 * testable and cannot accidentally be deployed as a server.</p>
 *
 * <p>@todo 운영 PostgreSQL의 전체 기준정보 DDL baseline이 아직 없습니다.
 * 완료 조건은 빈 PostgreSQL에서 모든 entity table/index/constraint를
 * Flyway만으로 생성하고, {@code ddl-auto=validate} 부팅 및 migration
 * 통합 테스트를 통과하며, 운영 설정에서 {@code ddl-auto=update}를
 * 제거하는 것입니다.</p>
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.masterdata")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ho.account.masterdata")
@EnableJpaRepositories(basePackages = "com.ho.account.masterdata.core.infrastructure.persistence")
@EntityScan(basePackages = {
        "com.ho.account.masterdata.core.domain.model",
        "com.ho.account.masterdata.core.domain.changerequest"
})
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
