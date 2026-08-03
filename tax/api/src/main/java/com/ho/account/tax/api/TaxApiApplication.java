package com.ho.account.tax.api;

import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Import;

/**
 * Tax API 실행 진입점이다.
 * 세금계산서 금액 검증과 취소 정책은 :tax:core가 담당하고, 이 모듈은 HTTP 호출 경계만 담당한다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.tax")
@EntityScan(basePackages = "com.ho.account.tax.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.tax.repository")
@Import(ProductionPostgresqlTlsGuard.class)
public class TaxApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxApiApplication.class, args);
    }
}
