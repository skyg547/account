package com.risk.mart.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * [Risk Data Mart Batch Service]
 */
@SpringBootApplication(scanBasePackages = "com.risk.mart")
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.risk.mart.core.domain",
        "com.risk.mart.core.infrastructure.persistence.entity",
        "com.risk.common.entity"
})
@EnableJpaRepositories(basePackages = {
        "com.risk.mart.core.infrastructure.persistence.jpa"
})
public class RiskDataMartBatchApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(RiskDataMartBatchApplication.class);

        // [초보 가이드] CLI 인자가 있으면 웹 서버를 끄고 배치 종료 후 프로세스를 종료합니다.
        boolean isCliMode = false;
        for (String arg : args) {
            if (arg.contains("spring.batch.job.name") || arg.contains("job.name")) {
                isCliMode = true;
                break;
            }
        }

        if (isCliMode) {
            application.setWebApplicationType(WebApplicationType.NONE);
            ConfigurableApplicationContext context = application.run(args);
            int exitCode = SpringApplication.exit(context);
            System.exit(exitCode);
        } else {
            application.setWebApplicationType(WebApplicationType.SERVLET);
            application.run(args);
        }
    }
}
