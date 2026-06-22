package com.ho.account.expenditure.payable.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Payable API 실행 진입점이다.
 * 업무 규칙은 :payable:core의 domain/application 계층에 두고, 이 모듈은 HTTP로 core UseCase를 호출할 Spring 컨텍스트만 연다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.expenditure")
@EntityScan(basePackages = "com.ho.account.expenditure.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.expenditure.repository")
public class PayableApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayableApiApplication.class, args);
    }
}
