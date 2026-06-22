package com.ho.account.expenditure.resolution.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Expenditure Resolution API 실행 진입점이다.
 * 지출결의 상태 전이, 예산 통제, 승인 이후 전표 연결은 :expenditure-resolution:core의 업무 흐름을 따른다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.expenditure")
@EntityScan(basePackages = {
        "com.ho.account.expenditure.domain",
        "com.ho.account.masterdata.core.domain.model"
})
@EnableJpaRepositories(basePackages = "com.ho.account.expenditure.repository")
public class ExpenditureResolutionApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExpenditureResolutionApiApplication.class, args);
    }
}
