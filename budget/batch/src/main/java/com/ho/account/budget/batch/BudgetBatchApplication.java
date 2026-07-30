package com.ho.account.budget.batch;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Budget 배치 전용 composition root.
 *
 * <p>초보자 가이드: API 서버와 달리 배치는 HTTP 요청을 받지 않습니다. 대신 아래의 명시적인
 * scan 범위에서 core 유즈케이스와 영속성 어댑터를 조립하고, 선택된 Spring Batch Job만 실행합니다.
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.budget")
@EntityScan(basePackages = "com.ho.account.budget")
@EnableJpaRepositories(basePackages = "com.ho.account.budget")
public class BudgetBatchApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(BudgetBatchApplication.class)
                .web(WebApplicationType.NONE)
                .run(args);
    }
}
