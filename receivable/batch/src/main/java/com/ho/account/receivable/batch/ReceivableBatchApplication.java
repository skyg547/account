package com.ho.account.receivable.batch;

import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Receivable Batch 실행 진입점이다.
 * 대량 수납 매칭의 Job/Step 제어만 이 모듈에 두고, 매칭 판단은 :receivable:core를 사용한다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.receivable")
@EntityScan(basePackages = "com.ho.account.receivable.infrastructure.persistence.entity")
@EnableJpaRepositories(basePackages = "com.ho.account.receivable.repository")
@Import(ProductionPostgresqlTlsGuard.class)
public class ReceivableBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReceivableBatchApplication.class, args);
    }
}
