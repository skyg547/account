package com.ho.account.asset.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.ho.account.asset")
@EnableDiscoveryClient
@EnableFeignClients(basePackages = "com.ho.account.asset")
@EntityScan(basePackages = "com.ho.account.asset.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.asset.repository")
public class AssetLeaseApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(AssetLeaseApiApplication.class, args);
    }
}
