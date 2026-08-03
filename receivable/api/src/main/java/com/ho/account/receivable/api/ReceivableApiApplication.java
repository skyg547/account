package com.ho.account.receivable.api;

import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Receivable API 실행 진입점이다.
 * 수납/채권 업무 판단은 :receivable:core가 담당하고, 이 모듈은 HTTP 요청을 core UseCase로 연결한다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.receivable")
@EntityScan(basePackages = "com.ho.account.receivable.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.receivable.repository")
@Import(ProductionPostgresqlTlsGuard.class)
public class ReceivableApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReceivableApiApplication.class, args);
    }
}
