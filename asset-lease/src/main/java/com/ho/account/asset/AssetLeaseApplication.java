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
@SpringBootApplication(scanBasePackages = "com.ho.account")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ho.account")
@EntityScan(basePackages = "com.ho.account")
@EnableJpaRepositories(basePackages = "com.ho.account")
public class AssetLeaseApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssetLeaseApplication.class, args);
    }
}
