package com.ho.account.asset.batch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.ho.account.asset")
@EnableDiscoveryClient
@EntityScan(basePackages = "com.ho.account.asset.domain")
@EnableJpaRepositories(basePackages = "com.ho.account.asset.repository")
public class AssetLeaseBatchApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(AssetLeaseBatchApplication.class);
        application.setWebApplicationType(WebApplicationType.NONE);

        // [초보 가이드] Batch 실행은 API 서버가 아니므로 Tomcat을 띄우지 않습니다.
        // Job 이름을 명시한 실행은 Job 완료 상태를 Gradle 종료 코드로 전달합니다.
        ConfigurableApplicationContext context = application.run(args);
        if (containsJobName(args)) {
            int exitCode = SpringApplication.exit(context);
            System.exit(exitCode);
        }
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
