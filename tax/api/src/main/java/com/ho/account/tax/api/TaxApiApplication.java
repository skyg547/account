package com.ho.account.tax.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Tax API 실행 진입점이다.
 * 세금계산서 금액 검증과 취소 정책은 :tax:core가 담당하고, 이 모듈은 HTTP 호출 경계만 담당한다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.tax")
@EntityScan(basePackages = "com.ho.account.tax.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.tax.repository")
public class TaxApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxApiApplication.class, args);
    }
}
