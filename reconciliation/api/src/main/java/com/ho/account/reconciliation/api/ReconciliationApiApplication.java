package com.ho.account.reconciliation.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Reconciliation API 실행 진입점이다.
 * 대사 기준, 실행, 차이 해소 규칙은 :reconciliation:core가 담당하고, API 모듈은 요청 진입점만 제공한다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.reconciliation")
@EntityScan(basePackages = {
        "com.ho.account.reconciliation.domain",
        "com.ho.account.reconciliation.infrastructure.persistence"
})
@EnableJpaRepositories(basePackages = {
        "com.ho.account.reconciliation.repository",
        "com.ho.account.reconciliation.infrastructure.persistence"
})
public class ReconciliationApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconciliationApiApplication.class, args);
    }
}
