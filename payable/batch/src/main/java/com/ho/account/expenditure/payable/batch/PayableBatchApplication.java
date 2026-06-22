package com.ho.account.expenditure.payable.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Payable Batch 실행 진입점이다.
 * 배치 모듈은 Job/Step 실행 껍데기만 담당하고, 채무/지급 계산은 :payable:core의 서비스와 도메인을 참조한다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.expenditure")
@EntityScan(basePackages = "com.ho.account.expenditure.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.expenditure.repository")
public class PayableBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayableBatchApplication.class, args);
    }
}
