package com.ho.account.tax.batch;

import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Import;

/**
 * Tax Batch 실행 진입점이다.
 * 신고/대량 검증 배치의 실행 제어만 맡고, 세금계산서 업무 규칙은 :tax:core를 참조한다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.tax")
@EntityScan(basePackages = "com.ho.account.tax.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.tax.repository")
@Import(ProductionPostgresqlTlsGuard.class)
public class TaxBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaxBatchApplication.class, args);
    }
}
