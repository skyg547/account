package com.ho.account;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * 마스터 데이터 관리 마이크로서비스 (Master Data Service)
 * 계정과목, 거래처, 부서 등 시스템 전반에서 사용되는 기준 정보를 관리합니다.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaRepositories(basePackages = "com.ho.account")
@EntityScan(basePackages = "com.ho.account")
public class MasterDataApplication {
    public static void main(String[] args) {
        SpringApplication.run(MasterDataApplication.class, args);
    }
}
