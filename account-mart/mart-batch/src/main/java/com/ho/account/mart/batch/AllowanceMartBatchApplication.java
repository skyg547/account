package com.ho.account.mart.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [IFRS 9 Allowance Mart Batch Service]
 */
@SpringBootApplication(scanBasePackages = "com.ho.account.mart")
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.ho.account.mart.core.domain",
        "com.ho.account.mart.core.infrastructure.persistence.entity",
        "com.ho.account.shared.finance.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.ho.account.mart.core.infrastructure.persistence.jpa"
})
public class AllowanceMartBatchApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AllowanceMartBatchApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        // [초보 가이드] Batch 모듈은 API 서버가 아니므로 내장 WAS를 띄우지 않습니다.
        // Job 이름을 넘긴 CLI 실행에서는 Job 완료 후 Gradle/Java 프로세스도 함께 종료합니다.
        if (containsJobName(args)) {
            ConfigurableApplicationContext context = application.run(args);
            int exitCode = SpringApplication.exit(context);
            System.exit(exitCode);
            return;
        }

        application.run(args);
    }

    private static boolean containsJobName(String[] args) {
        for (String arg : args) {
            if (arg.contains("spring.batch.job.name") || arg.contains("job.name")) {
                return true;
            }
        }
        return false;
    }
}
