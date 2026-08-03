package com.ho.account.reconciliation.batch;

import com.ho.account.shared.infrastructure.ProductionPostgresqlTlsGuard;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.context.annotation.Import;

/**
 * Reconciliation Batch 실행 진입점이다.
 * 대량 대사 실행의 Trigger/Job/Step 제어는 batch가 맡고, 비교 규칙과 차이 판단은 :reconciliation:core를 참조한다.
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
@Import(ProductionPostgresqlTlsGuard.class)
public class ReconciliationBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ReconciliationBatchApplication.class, args);
    }
}
