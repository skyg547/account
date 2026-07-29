package com.ho.account.masterdata.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [Pedagogical Comment] Master Data API Application Entry Point.
 * 
 * why this design?
 * Master Data는 거래처(Business Partner), 계정과목(Account Subject), 조직(Department) 등 전사 기준정보를 관할합니다.
 * 헥사고날 멀티모듈 레벨에서 core의 도메인/엔티티를 스캔할 수 있도록 scanBasePackages, EntityScan, EnableJpaRepositories를 통합 설정합니다.
 */
@SpringBootApplication(scanBasePackages = {"com.ho.account.masterdata"})
@EntityScan(basePackages = {"com.ho.account.masterdata"})
@EnableJpaRepositories(basePackages = {"com.ho.account.masterdata"})
public class MasterDataApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(MasterDataApiApplication.class, args);
    }
}
