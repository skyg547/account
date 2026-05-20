package com.ho.account.asset;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * 고정자산 및 리스 서비스 (Asset & Lease Service) 메인 클래스
 */
@SpringBootApplication(scanBasePackages = {
        "com.ho.account.asset",
        "com.ho.account.masterdata.core"
})
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ho.account.asset")
@EntityScan(basePackages = {
        "com.ho.account.asset.domain",
        "com.ho.account.masterdata.core.domain"
})
@EnableJpaRepositories(basePackages = {
        "com.ho.account.asset.repository",
        "com.ho.account.masterdata.core.infrastructure.persistence"
})
public class AssetLeaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssetLeaseApplication.class, args);
    }
}
