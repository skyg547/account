package com.ho.account.expenditure.resolution.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Expenditure Resolution Batch 실행 진입점이다.
 * 배치 모듈은 승인/정산 대상 조회와 Job/Step 제어를 맡고, 지출 업무 규칙은 :expenditure-resolution:core를 사용한다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.expenditure")
@EntityScan(basePackages = {
        "com.ho.account.expenditure.domain",
        "com.ho.account.masterdata.core.domain.model"
})
@EnableJpaRepositories(basePackages = "com.ho.account.expenditure.repository")
public class ExpenditureResolutionBatchApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenditureResolutionBatchApplication.class, args);
    }
}
